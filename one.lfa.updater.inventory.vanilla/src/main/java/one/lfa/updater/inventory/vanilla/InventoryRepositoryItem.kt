package one.lfa.updater.inventory.vanilla

import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.SettableFuture
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import one.lfa.updater.installed.api.InstalledApplicationEvent.InstalledApplicationsChanged
import one.lfa.updater.installed.api.InstalledApplicationsType
import one.lfa.updater.inventory.api.InventoryCatalogDirectoryType
import one.lfa.updater.inventory.api.InventoryEvent
import one.lfa.updater.inventory.api.InventoryEvent.InventoryRepositoryEvent.InventoryRepositoryItemEvent.ItemChanged
import one.lfa.updater.inventory.api.InventoryHashIndexedDirectoryType
import one.lfa.updater.inventory.api.InventoryItemResult
import one.lfa.updater.inventory.api.InventoryItemState
import one.lfa.updater.inventory.api.InventoryItemState.Failed
import one.lfa.updater.inventory.api.InventoryItemState.Installed
import one.lfa.updater.inventory.api.InventoryItemState.Operating
import one.lfa.updater.inventory.api.InventoryItemState.NotInstalled
import one.lfa.updater.inventory.api.InventoryProgress
import one.lfa.updater.inventory.api.InventoryProgressValue
import one.lfa.updater.inventory.api.InventoryRepositoryItemType
import one.lfa.updater.inventory.api.InventoryStringResourcesType
import one.lfa.updater.inventory.api.InventoryTaskStep
import one.lfa.updater.inventory.vanilla.tasks.InventoryTaskAPKFetchInstall
import one.lfa.updater.inventory.vanilla.tasks.InventoryTaskAPKFetchInstallRequest
import one.lfa.updater.inventory.vanilla.tasks.InventoryTaskAPKUninstall
import one.lfa.updater.inventory.vanilla.tasks.InventoryTaskExecutionType
import one.lfa.updater.inventory.vanilla.tasks.InventoryTaskOPDSDelete
import one.lfa.updater.inventory.vanilla.tasks.InventoryTaskOPDSFetch
import one.lfa.updater.inventory.vanilla.tasks.InventoryTaskResult
import one.lfa.updater.opds.database.api.OPDSDatabaseEvent.OPDSDatabaseEntryEvent
import one.lfa.updater.opds.database.api.OPDSDatabaseType
import one.lfa.updater.repository.api.RepositoryItem
import one.lfa.updater.services.api.ServiceDirectoryType
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
  private val installedApplications =
    this.services.requireService(InstalledApplicationsType::class.java)
  private val opdsDatabase =
    this.services.requireService(OPDSDatabaseType::class.java)
  private val apkDirectory =
    this.services.requireService(InventoryHashIndexedDirectoryType::class.java)

  private val opdsSubscription: Disposable
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
  private var installing: ListenableFuture<InventoryItemResult>? = null

  private val isInstalled: Boolean
    get() = this.installedApplications.isInstalled(this.id)
      || this.opdsDatabase.isInstalled(this.id)

  init {
    this.installedSubscription =
      this.installedApplications.events.ofType(InstalledApplicationsChanged::class.java)
        .filter { event -> event.installedApplication.id == this.item.id }
        .subscribe(this::onInstalledPackageEvent)

    this.opdsSubscription =
      this.opdsDatabase.events.ofType(OPDSDatabaseEntryEvent::class.java)
        .filter { event -> event.id.toString() == this.item.id }
        .subscribe(this::onOPDSDatabaseEvent)
  }

  private fun onOPDSDatabaseEvent(event: OPDSDatabaseEntryEvent) {
    val stateCurrent = this.stateActual
    return when (event) {
      is OPDSDatabaseEntryEvent.DatabaseEntryUpdated -> {
        when (stateCurrent) {
          is NotInstalled,
          is Operating.Installing -> {
            val entry = this.opdsDatabase.open(event.id)
            if (entry != null) {
              this.logger.debug("OPDS package {} became installed", this.item.id)
              this.stateActual = Installed(
                inventoryItem = this,
                installedVersionCode = entry.versionCode,
                installedVersionName = entry.versionName)
            } else {

            }
          }
          is Installed,
          is Failed,
          is Operating.Uninstalling -> {

          }
        }
      }

      is OPDSDatabaseEntryEvent.DatabaseEntryDeleted -> {
        when (stateCurrent) {
          is Installed,
          is Operating.Uninstalling -> {
            this.logger.debug("OPDS package {} became uninstalled", this.item.id)
            this.stateActual = NotInstalled(this)
          }
          is NotInstalled ,
          is Failed ,
          is Operating.Installing -> {

          }
        }
      }
    }
  }

  private fun onInstalledPackageEvent(event: InstalledApplicationsChanged) {
    val stateCurrent = this.stateActual
    return when (event) {
      is InstalledApplicationsChanged.InstalledApplicationAdded -> {
        if (stateCurrent is NotInstalled) {
          this.logger.debug("application package {} became installed", this.item.id)
          this.stateActual = Installed(
            inventoryItem = this,
            installedVersionCode = event.installedApplication.versionCode,
            installedVersionName = event.installedApplication.versionName)
        } else {

        }
      }
      is InstalledApplicationsChanged.InstalledApplicationRemoved -> {
        if (stateCurrent is Installed) {
          this.logger.debug("application package {} became uninstalled", this.item.id)
          this.stateActual = NotInstalled(this)
        } else {

        }
      }
      is InstalledApplicationsChanged.InstalledApplicationUpdated -> {
        if (stateCurrent is NotInstalled) {
          this.logger.debug("application package {} became installed", this.item.id)
          this.stateActual = Installed(
            inventoryItem = this,
            installedVersionCode = event.installedApplication.versionCode,
            installedVersionName = event.installedApplication.versionName)
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

  override fun install(activity: Any): ListenableFuture<InventoryItemResult> {
    this.logger.debug("[${this.id}]: install")

    return synchronized(this.stateLock) {
      when (this.stateActual) {
        is NotInstalled,
        is Installed,
        is Failed -> {
          this.stateActual =
            Operating.Installing(
              inventoryItem = this,
              major = null,
              minor = InventoryProgressValue.InventoryProgressValueIndefinite(0L, 0L),
              status = "")
          this.runInstall(activity)
        }

        is Operating.Installing -> {
          Futures.immediateFuture(InventoryItemResult(
            repositoryId = this.repositoryId,
            itemVersionCode = this.versionCode,
            itemVersionName = this.versionName,
            itemName = this.name,
            itemURI = this.item.source,
            steps = listOf(InventoryTaskStep(
              description = this.stringResources.installStarted,
              resolution = this.stringResources.installAlreadyInstalling))))
        }
        is Operating.Uninstalling -> {
          TODO()
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
        is Failed -> true
        is Operating.Installing -> false
        is Operating.Uninstalling -> false
      }
    }
  }

  private fun runInstall(activity: Any): ListenableFuture<InventoryItemResult> {
    val settableFuture = SettableFuture.create<InventoryItemResult>()
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
    future: SettableFuture<InventoryItemResult>
  ): InventoryItemResult {

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
      InventoryItemResult(
        this.repositoryId,
        this.id,
        this.versionCode,
        this.versionName,
        this.item.source,
        result.steps)

    return when (result) {
      is InventoryTaskResult.InventoryTaskFailed -> {
        this.stateActual = Failed(this, installResult)
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
    future: SettableFuture<InventoryItemResult>
  ): InventoryTaskResult<Unit> {
    this.logger.debug("runInstallActualOPDS: {}", this.sourceURI)

    val inventoryCatalogDirectory =
      executionContext.services.requireService(InventoryCatalogDirectoryType::class.java)

    return InventoryTaskOPDSFetch.create(this.sourceURI, inventoryCatalogDirectory.directory)
      .map { Unit }
      .evaluate(executionContext)
  }

  private fun runInstallActualAPK(
    activity: Any,
    executionContext: InventoryTaskExecutionType,
    future: SettableFuture<InventoryItemResult>
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

  override fun uninstall(activity: Any): ListenableFuture<InventoryItemResult> {
    val settableFuture = SettableFuture.create<InventoryItemResult>()
    this.installing = settableFuture

    this.executor.execute {
      this.runUninstallActual(activity, settableFuture)
    }

    return settableFuture
  }

  private fun runUninstallActual(
    activity: Any,
    future: SettableFuture<InventoryItemResult>
  ): InventoryItemResult {
    val executionContext = object : InventoryTaskExecutionType {
      override val services: ServiceDirectoryType
        get() = this@InventoryRepositoryItem.services
      override val isCancelRequested: Boolean
        get() = future.isCancelled
      override val onProgress: (InventoryProgress) -> Unit
        get() = this@InventoryRepositoryItem::onUninstallProgress
    }

    val result =
      when (this.item) {
        is RepositoryItem.RepositoryAndroidPackage ->
          this.runUninstallActualAPK(activity, executionContext, future)
        is RepositoryItem.RepositoryOPDSPackage ->
          this.runUninstallActualOPDS(executionContext, future)
      }

    val installResult =
      InventoryItemResult(
        this.repositoryId,
        this.id,
        this.versionCode,
        this.versionName,
        this.item.source,
        result.steps)

    return when (result) {
      is InventoryTaskResult.InventoryTaskFailed -> {
        this.stateActual = Failed(this, installResult)
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

  private fun runUninstallActualOPDS(
    executionContext: InventoryTaskExecutionType,
    future: SettableFuture<InventoryItemResult>
  ): InventoryTaskResult<Unit> {
    this.logger.debug("runUninstallActualOPDS: {}", this.sourceURI)

    return try {
      InventoryTaskOPDSDelete.create(UUID.fromString(this.id))
        .map { Unit }
        .evaluate(executionContext)
    } catch (e: Exception) {
      future.setException(e)
      val step = InventoryTaskStep(
        description = this.stringResources.opdsCatalogDeleting,
        resolution = e.localizedMessage,
        exception = e,
        failed = true)
      InventoryTaskResult.failed(step)
    }
  }

  private fun runUninstallActualAPK(
    activity: Any,
    executionContext: InventoryTaskExecutionType,
    future: SettableFuture<InventoryItemResult>
  ): InventoryTaskResult<Unit> {
    return InventoryTaskAPKUninstall.create(activity, this.item.id)
      .evaluate(executionContext)
  }

  private fun onInstallProgress(progress: InventoryProgress) {
    this.stateActual =
      Operating.Installing(
        inventoryItem = this,
        major = progress.major,
        minor = progress.minor,
        status = progress.status)
  }

  private fun onUninstallProgress(progress: InventoryProgress) {
    this.stateActual =
      Operating.Uninstalling(
        inventoryItem = this,
        major = progress.major,
        minor = progress.minor,
        status = progress.status)
  }

  override val state: InventoryItemState
    get() = synchronized(this.stateLock) { this.stateActual }

  internal fun dispose() {
    this.logger.debug("[{}]: dispose", this.id)
    this.installedSubscription.dispose()
    this.opdsSubscription.dispose()
  }
}