package one.lfa.updater.inventory.vanilla

import one.lfa.updater.services.api.ServiceDirectoryType
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.SettableFuture
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import one.lfa.updater.installed.api.InstalledItemEvent.InstalledItemsChanged
import one.lfa.updater.installed.api.InstalledItemsType
import one.lfa.updater.inventory.api.InventoryEvent
import one.lfa.updater.inventory.api.InventoryEvent.InventoryRepositoryEvent.InventoryRepositoryItemEvent.ItemChanged
import one.lfa.updater.inventory.api.InventoryHashIndexedDirectoryType
import one.lfa.updater.inventory.api.InventoryItemInstallResult
import one.lfa.updater.inventory.api.InventoryItemState
import one.lfa.updater.inventory.api.InventoryItemState.InstallFailed
import one.lfa.updater.inventory.api.InventoryItemState.Installed
import one.lfa.updater.inventory.api.InventoryItemState.Installing
import one.lfa.updater.inventory.api.InventoryItemState.NotInstalled
import one.lfa.updater.inventory.api.InventoryProgress
import one.lfa.updater.inventory.api.InventoryProgressValue
import one.lfa.updater.inventory.api.InventoryRepositoryItemType
import one.lfa.updater.inventory.api.InventoryStringResourcesType
import one.lfa.updater.inventory.api.InventoryTaskStep
import one.lfa.updater.inventory.vanilla.tasks.InventoryTask
import one.lfa.updater.inventory.vanilla.tasks.InventoryTaskAPKFetchInstall
import one.lfa.updater.inventory.vanilla.tasks.InventoryTaskAPKFetchInstallRequest
import one.lfa.updater.inventory.vanilla.tasks.InventoryTaskExecutionType
import one.lfa.updater.inventory.vanilla.tasks.InventoryTaskOPDSManifestFetch
import one.lfa.updater.inventory.vanilla.tasks.InventoryTaskResult
import one.lfa.updater.repository.api.RepositoryItem
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.UUID

internal class InventoryRepositoryItem(
  private val services: ServiceDirectoryType,
  private val repositoryId: UUID,
  override val item: RepositoryItem,
  private val events: PublishSubject<InventoryEvent>,
  private val executor: ListeningExecutorService,
  initiallyInstalledVersion: NamedVersion?
) : InventoryRepositoryItemType {

  private val logger =
    LoggerFactory.getLogger(InventoryRepositoryItem::class.java)

  private val stringResources =
    this.services.requireService(InventoryStringResourcesType::class.java)
  private val installedPackages =
    this.services.requireService(InstalledItemsType::class.java)
  private val apkDirectory =
    this.services.requireService(InventoryHashIndexedDirectoryType::class.java)

  private val installedSubscription: Disposable
  private val stateLock = Object()
  private var stateActual: InventoryItemState =
    if (initiallyInstalledVersion != null) {
      Installed(
        inventoryItem = this,
        installedVersionCode = initiallyInstalledVersion.code,
        installedVersionName = initiallyInstalledVersion.name)
    } else {
      NotInstalled(this)
    }
    set(value) {
      synchronized(this.stateLock) {
        field = value
      }
      this.events.onNext(ItemChanged(this.repositoryId, this.id))
    }

  @Volatile
  private var installing: ListenableFuture<InventoryItemInstallResult>? = null

  private val isInstalled: Boolean
    get() = this.installedPackages.items().containsKey(this.id)

  init {
    this.installedSubscription =
      this.installedPackages.events.ofType(InstalledItemsChanged::class.java)
        .filter { event -> event.installedItem.id == this.item.id }
        .subscribe(this::onInstalledPackageEvent)
  }

  private fun onInstalledPackageEvent(event: InstalledItemsChanged) {
    val stateCurrent = this.stateActual
    return when (event) {
      is InstalledItemsChanged.InstalledItemAdded -> {
        if (stateCurrent is NotInstalled) {
          this.logger.debug("package {} became installed", this.item.id)
          this.stateActual = Installed(
            inventoryItem = this,
            installedVersionCode = event.installedItem.versionCode,
            installedVersionName = event.installedItem.versionName)
        } else {

        }
      }
      is InstalledItemsChanged.InstalledItemRemoved -> {
        if (stateCurrent is Installed) {
          this.logger.debug("package {} became uninstalled", this.item.id)
          this.stateActual = NotInstalled(this)
        } else {

        }
      }
      is InstalledItemsChanged.InstalledItemUpdated -> {
        if (stateCurrent is NotInstalled) {
          this.logger.debug("package {} became installed", this.item.id)
          this.stateActual = Installed(
            inventoryItem = this,
            installedVersionCode = event.installedItem.versionCode,
            installedVersionName = event.installedItem.versionName)
        } else {

        }
      }
    }
  }

  private val sourceURI: URI
    get() = this.item.source

  private val id: String
    get() = this.item.id

  private val versionCode: Long
    get() = this.item.versionCode

  private val versionName: String
    get() = this.item.versionName

  private val name: String
    get() = this.item.name

  override fun install(activity: Any): ListenableFuture<InventoryItemInstallResult> {
    this.logger.debug("[${this.id}]: install")

    return synchronized(this.stateLock) {
      when (this.stateActual) {
        is NotInstalled,
        is Installed,
        is InstallFailed -> {
          this.stateActual =
            Installing(
              inventoryItem = this,
              major = null,
              minor = InventoryProgressValue.InventoryProgressValueIndefinite(0L, 0L),
              status = "")
          this.runInstall(activity)
        }

        is Installing -> {
          Futures.immediateFuture(InventoryItemInstallResult(
            repositoryId = this.repositoryId,
            itemVersionCode = this.versionCode,
            itemVersionName = this.versionName,
            itemName = this.name,
            itemURI = this.item.source,
            steps = listOf(InventoryTaskStep(
              description = this.stringResources.installStarted,
              resolution = this.stringResources.installAlreadyInstalling))))
        }
      }
    }
  }

  override val isUpdateAvailable: Boolean
    get() = this.checkIsUpdateAvailable()

  private fun checkIsUpdateAvailable(): Boolean {
    return synchronized(this.stateLock) {
      when (val state = this.stateActual) {
        is NotInstalled -> true
        is Installed -> state.installedVersionCode < this.item.versionCode
        is InstallFailed -> true
        is Installing -> false
      }
    }
  }

  private fun runInstall(activity: Any): ListenableFuture<InventoryItemInstallResult> {
    val settableFuture = SettableFuture.create<InventoryItemInstallResult>()
    this.installing = settableFuture

    this.executor.execute {
      this.runInstallActual(activity, settableFuture)
    }

    return settableFuture
  }

  override fun cancel() {
    this.installing?.cancel(true)
  }

  private fun runInstallActual(
    activity: Any,
    future: SettableFuture<InventoryItemInstallResult>
  ): InventoryItemInstallResult {

    val executionContext = object : InventoryTaskExecutionType {
      override val services: ServiceDirectoryType
        get() = this@InventoryRepositoryItem.services
      override val isCancelRequested: Boolean
        get() = future.isCancelled
      override val onProgress: (InventoryProgress) -> Unit
        get() = this@InventoryRepositoryItem::onInstallProgress
    }

    val result =
      when (this.item) {
        is RepositoryItem.RepositoryAndroidPackage ->
          this.runInstallActualAPK(activity, executionContext, future)
        is RepositoryItem.RepositoryOPDSPackage ->
          this.runInstallActualOPDS(executionContext, future)
      }

    val installResult =
      InventoryItemInstallResult(
        this.repositoryId,
        this.id,
        this.versionCode,
        this.versionName,
        this.item.source,
        result.steps)

    return when (result) {
      is InventoryTaskResult.InventoryTaskFailed -> {
        this.stateActual = InstallFailed(this, installResult)
        installResult
      }
      is InventoryTaskResult.InventoryTaskSucceeded,
      is InventoryTaskResult.InventoryTaskCancelled -> {
        this.stateActual =
          if (this.isInstalled) {
            Installed(this, this.versionCode, this.versionName)
          } else {
            NotInstalled(this)
          }
        installResult
      }
    }
  }

  private fun runInstallActualOPDS(
    executionContext: InventoryTaskExecutionType,
    future: SettableFuture<InventoryItemInstallResult>
  ): InventoryTaskResult<Unit> {
    this.logger.debug("runInstallActualOPDS: {}", this.sourceURI)

    return InventoryTaskOPDSManifestFetch.create(this.sourceURI)
      .flatMap { InventoryTask { InventoryTaskResult.succeeded(Unit, InventoryTaskStep("")) }  }
      .evaluate(executionContext)
  }

  private fun runInstallActualAPK(
    activity: Any,
    executionContext: InventoryTaskExecutionType,
    future: SettableFuture<InventoryItemInstallResult>
  ): InventoryTaskResult<Unit> {
    this.logger.debug("runInstallActualAPK: {}", this.sourceURI)

    return try {
      this.apkDirectory.withKey(this.item.sha256) { reservation ->
        InventoryTaskAPKFetchInstall.create(
          InventoryTaskAPKFetchInstallRequest(
            activity = activity,
            packageName = this.id,
            packageVersionCode = this.versionCode.toInt(),
            downloadURI = this.sourceURI,
            downloadRetries = 10,
            apkFile = reservation.file,
            hash = reservation.hash
          )
        ).evaluate(executionContext)
      }
    } catch (e: Exception) {
      future.setException(e)
      val step = InventoryTaskStep(
        description = this.stringResources.installReservingFile,
        resolution = this.stringResources.installDownloadReservationFailed(e),
        exception = e,
        failed = true)
      InventoryTaskResult.failed(step)
    }
  }

  private fun onInstallProgress(progress: InventoryProgress) {
    this.stateActual =
      Installing(
        inventoryItem = this,
        major = progress.major,
        minor = progress.minor,
        status = progress.status)
  }

  override val state: InventoryItemState
    get() = synchronized(this.stateLock) { this.stateActual }

}