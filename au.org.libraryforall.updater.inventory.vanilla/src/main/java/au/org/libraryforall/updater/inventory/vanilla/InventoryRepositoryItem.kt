package au.org.libraryforall.updater.inventory.vanilla

import au.org.libraryforall.updater.installed.api.InstalledItemEvent.InstalledItemsChanged
import au.org.libraryforall.updater.installed.api.InstalledItemsType
import au.org.libraryforall.updater.inventory.api.InventoryEvent
import au.org.libraryforall.updater.inventory.api.InventoryEvent.InventoryRepositoryEvent.InventoryRepositoryItemEvent.ItemChanged
import au.org.libraryforall.updater.inventory.api.InventoryHashIndexedDirectoryType
import au.org.libraryforall.updater.inventory.api.InventoryItemInstallResult
import au.org.libraryforall.updater.inventory.api.InventoryItemState
import au.org.libraryforall.updater.inventory.api.InventoryItemState.InstallFailed
import au.org.libraryforall.updater.inventory.api.InventoryItemState.Installed
import au.org.libraryforall.updater.inventory.api.InventoryItemState.Installing
import au.org.libraryforall.updater.inventory.api.InventoryItemState.NotInstalled
import au.org.libraryforall.updater.inventory.api.InventoryProgress
import au.org.libraryforall.updater.inventory.api.InventoryProgressValue
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryItemType
import au.org.libraryforall.updater.inventory.api.InventoryStringResourcesType
import au.org.libraryforall.updater.inventory.api.InventoryTaskStep
import au.org.libraryforall.updater.inventory.vanilla.tasks.InventoryTaskAPKFetchInstall
import au.org.libraryforall.updater.inventory.vanilla.tasks.InventoryTaskAPKFetchInstallRequest
import au.org.libraryforall.updater.inventory.vanilla.tasks.InventoryTaskExecutionType
import au.org.libraryforall.updater.inventory.vanilla.tasks.InventoryTaskResult
import au.org.libraryforall.updater.repository.api.RepositoryItem
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.SettableFuture
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import au.org.libraryforall.updater.services.api.ServiceDirectoryType
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.UUID

internal class InventoryRepositoryItem(
  private val services: ServiceDirectoryType,
  private val repositoryId: UUID,
  private val repositoryItem: RepositoryItem,
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
        .filter { event -> event.installedItem.id == this.repositoryItem.id }
        .subscribe(this::onInstalledPackageEvent)
  }

  private fun onInstalledPackageEvent(event: InstalledItemsChanged) {
    val stateCurrent = this.stateActual
    return when (event) {
      is InstalledItemsChanged.InstalledItemAdded -> {
        if (stateCurrent is NotInstalled) {
          this.logger.debug("package {} became installed", this.repositoryItem.id)
          this.stateActual = Installed(
            inventoryItem = this,
            installedVersionCode = event.installedItem.versionCode,
            installedVersionName = event.installedItem.versionName)
        } else {

        }
      }
      is InstalledItemsChanged.InstalledItemRemoved -> {
        if (stateCurrent is Installed) {
          this.logger.debug("package {} became uninstalled", this.repositoryItem.id)
          this.stateActual = NotInstalled(this)
        } else {

        }
      }
      is InstalledItemsChanged.InstalledItemUpdated -> {
        if (stateCurrent is NotInstalled) {
          this.logger.debug("package {} became installed", this.repositoryItem.id)
          this.stateActual = Installed(
            inventoryItem = this,
            installedVersionCode = event.installedItem.versionCode,
            installedVersionName = event.installedItem.versionName)
        } else {

        }
      }
    }
  }

  override val sourceURI: URI
    get() = this.repositoryItem.source

  override val id: String
    get() = this.repositoryItem.id

  override val versionCode: Long
    get() = this.repositoryItem.versionCode

  override val versionName: String
    get() = this.repositoryItem.versionName

  override val name: String
    get() = this.repositoryItem.name

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
            itemURI = this.repositoryItem.source,
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
        is Installed -> state.installedVersionCode < this.repositoryItem.versionCode
        is InstallFailed -> true
        is Installing -> false
      }
    }
  }

  private fun runInstall(activity: Any): ListenableFuture<InventoryItemInstallResult> {
    val settableFuture = SettableFuture.create<InventoryItemInstallResult>()
    this.installing = settableFuture

    this.executor.execute {
      runInstallActual(activity, settableFuture)
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

    val executionContext = object: InventoryTaskExecutionType {
      override val services: ServiceDirectoryType
        get() = this@InventoryRepositoryItem.services
      override val isCancelRequested: Boolean
        get() = future.isCancelled
      override val onProgress: (InventoryProgress) -> Unit
        get() = this@InventoryRepositoryItem::onInstallProgress
    }

    val result =
      try {
        this.apkDirectory.withKey(this.repositoryItem.sha256) { reservation ->
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
        InventoryTaskResult.failed<Unit>(step)
      }

    val installResult =
      InventoryItemInstallResult(
        this.repositoryId,
        this.id,
        this.versionCode,
        this.versionName,
        this.repositoryItem.source,
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