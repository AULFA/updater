package au.org.libraryforall.updater.inventory.vanilla

import au.org.libraryforall.updater.apkinstaller.api.APKInstallerType
import au.org.libraryforall.updater.installed.api.InstalledItemEvent.InstalledItemsChanged
import au.org.libraryforall.updater.installed.api.InstalledItemsType
import au.org.libraryforall.updater.inventory.api.InventoryAPKDirectoryType
import au.org.libraryforall.updater.inventory.api.InventoryAPKDirectoryType.VerificationProgressType
import au.org.libraryforall.updater.inventory.api.InventoryEvent
import au.org.libraryforall.updater.inventory.api.InventoryEvent.InventoryRepositoryEvent.InventoryRepositoryItemEvent.ItemChanged
import au.org.libraryforall.updater.inventory.api.InventoryItemInstallResult
import au.org.libraryforall.updater.inventory.api.InventoryItemState
import au.org.libraryforall.updater.inventory.api.InventoryItemState.InstallFailed
import au.org.libraryforall.updater.inventory.api.InventoryItemState.Installed
import au.org.libraryforall.updater.inventory.api.InventoryItemState.Installing
import au.org.libraryforall.updater.inventory.api.InventoryItemState.InstallingStatus.InstallingStatusDefinite
import au.org.libraryforall.updater.inventory.api.InventoryItemState.InstallingStatus.InstallingStatusIndefinite
import au.org.libraryforall.updater.inventory.api.InventoryItemState.NotInstalled
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryItemType
import au.org.libraryforall.updater.inventory.api.InventoryStringResourcesType
import au.org.libraryforall.updater.inventory.api.InventoryTaskStep
import au.org.libraryforall.updater.repository.api.RepositoryItem
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import one.irradia.http.api.HTTPAuthentication
import one.irradia.http.api.HTTPClientType
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicBoolean

internal class InventoryRepositoryItem(
  private val repositoryId: UUID,
  private val repositoryItem: RepositoryItem,
  private val http: HTTPClientType,
  private val httpAuthentication: (URI) -> HTTPAuthentication?,
  private val directory: InventoryAPKDirectoryType,
  private val apkInstaller: APKInstallerType,
  private val resources: InventoryStringResourcesType,
  private val events: PublishSubject<InventoryEvent>,
  private val installedPackages: InstalledItemsType,
  private val executor: ListeningExecutorService,
  initiallyInstalledVersion: NamedVersion?) : InventoryRepositoryItemType {

  private val installedSubscription: Disposable
  private val installCancel = AtomicBoolean(false)
  private val stateLock = Object()
  private var stateActual: InventoryItemState =
    if (initiallyInstalledVersion != null) {
      Installed(
        inventoryItem = this,
        installedVersionCode = initiallyInstalledVersion.code.toLong(),
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

  private val logger =
    LoggerFactory.getLogger(InventoryRepositoryItem::class.java)

  override fun install(activity: Any): ListenableFuture<InventoryItemInstallResult> {
    this.logger.debug("[${this.id}]: install")

    return synchronized(this.stateLock) {
      when (this.stateActual) {
        is NotInstalled,
        is Installed,
        is InstallFailed -> {
          this.stateActual = Installing(this, InstallingStatusIndefinite(""))
          return this.runInstall(activity)
        }

        is Installing -> {
          Futures.immediateFuture(InventoryItemInstallResult(
            repositoryId = this.repositoryId,
            itemVersionCode = this.versionCode,
            itemVersionName = this.versionName,
            itemName = this.name,
            itemURI = this.repositoryItem.source,
            steps = listOf(InventoryTaskStep(
              description = this.resources.installStarted,
              resolution = this.resources.installAlreadyInstalling))))
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
    this.installCancel.set(false)
    val step0 = InventoryTaskStep(description = this.resources.installStarted)
    return this.executor.submit(Callable<InventoryItemInstallResult> {
      runInstallActual(step0, activity)
    })
  }

  override fun cancel() {
    this.installCancel.set(true)
  }

  private fun runInstallActual(
    initialStep: InventoryTaskStep,
    activity: Any
  ): InventoryItemInstallResult {

    val result =
      try {
        this.directory.withKey(this.repositoryItem.sha256) { reservation ->
          InventoryTaskMonad.startWithStep(initialStep)
            .flatMap {
              InventoryTaskDownload(
                resources = this.resources,
                http = this.http,
                httpAuthentication = this.httpAuthentication,
                reservation = reservation,
                onVerificationProgress = this::onInstallVerificationProgress,
                uri = this.repositoryItem.source,
                onDownloadProgress = this::onInstallDownloadProgress,
                cancel = this.installCancel
              ).execute()
            }.flatMap {
              InventoryTaskVerify(
                resources = this.resources,
                reservation = reservation,
                onVerificationProgress = this::onInstallVerificationProgress,
                cancel = this.installCancel
              ).execute()
            }.flatMap {
              this.stateActual =
                Installing(
                  this,
                  InstallingStatusIndefinite(
                    status = this.resources.installWaitingForInstaller))

              InventoryTaskAPKInstall(
                activity,
                resources = this.resources,
                packageName = this.id,
                packageVersionCode = this.versionCode,
                file = reservation.file,
                apkInstaller = this.apkInstaller,
                cancel = this.installCancel
              ).execute()
            }
        }
      } catch (e: Exception) {
        this.installCancel.set(false)
        initialStep.resolution = this.resources.installDownloadReservationFailed(e)
        initialStep.failed = true
        initialStep.exception = e
        InventoryTaskMonad.InventoryTaskFailed<File>(listOf(initialStep))
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
      is InventoryTaskMonad.InventoryTaskFailed -> {
        this.stateActual = InstallFailed(this, installResult)
        installResult
      }
      is InventoryTaskMonad.InventoryTaskSuccess,
      is InventoryTaskMonad.InventoryTaskCancelled -> {
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

  private fun onInstallDownloadProgress(progress: InventoryTaskDownloadProgressType) {
    val total = progress.expectedBytesTotal
    if (total != null) {
      this.stateActual =
        Installing(
          this,
          InstallingStatusDefinite(
            currentBytes = progress.receivedBytesTotal,
            maximumBytes = total,
            status = this.resources.installDownloading(
              receivedBytesTotal = progress.receivedBytesTotal,
              expectedBytesTotal = total,
              bytesPerSecond = progress.receivedBytesPerSecond
            )))
    } else {
      this.stateActual =
        Installing(
          this,
          InstallingStatusIndefinite(
            status = this.resources.installDownloadingIndefinite(
              receivedBytesTotal = progress.receivedBytesTotal,
              bytesPerSecond = progress.receivedBytesPerSecond
            )))
    }
  }

  private fun onInstallVerificationProgress(progress: VerificationProgressType) {
    this.stateActual =
      Installing(
        this,
        InstallingStatusDefinite(
          currentBytes = progress.currentBytes,
          maximumBytes = progress.maximumBytes,
          status = this.resources.installVerifying(progress.currentBytes, progress.maximumBytes)))
  }

  override val state: InventoryItemState
    get() = synchronized(this.stateLock) { this.stateActual }

}