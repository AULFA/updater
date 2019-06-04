package au.org.libraryforall.updater.inventory.vanilla

import au.org.libraryforall.updater.apkinstaller.api.APKInstallerType
import au.org.libraryforall.updater.installed.api.InstalledPackageEvent.InstalledPackagesChanged
import au.org.libraryforall.updater.installed.api.InstalledPackagesType
import au.org.libraryforall.updater.inventory.api.InventoryAPKDirectoryType
import au.org.libraryforall.updater.inventory.api.InventoryAPKDirectoryType.VerificationProgressType
import au.org.libraryforall.updater.inventory.api.InventoryEvent
import au.org.libraryforall.updater.inventory.api.InventoryEvent.InventoryRepositoryEvent.InventoryRepositoryPackageEvent.PackageChanged
import au.org.libraryforall.updater.inventory.api.InventoryPackageInstallResult
import au.org.libraryforall.updater.inventory.api.InventoryPackageState
import au.org.libraryforall.updater.inventory.api.InventoryPackageState.InstallFailed
import au.org.libraryforall.updater.inventory.api.InventoryPackageState.Installed
import au.org.libraryforall.updater.inventory.api.InventoryPackageState.Installing
import au.org.libraryforall.updater.inventory.api.InventoryPackageState.InstallingStatus.InstallingStatusDefinite
import au.org.libraryforall.updater.inventory.api.InventoryPackageState.InstallingStatus.InstallingStatusIndefinite
import au.org.libraryforall.updater.inventory.api.InventoryPackageState.NotInstalled
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryPackageType
import au.org.libraryforall.updater.inventory.api.InventoryStringResourcesType
import au.org.libraryforall.updater.inventory.api.InventoryTaskStep
import au.org.libraryforall.updater.inventory.vanilla.InventoryTaskDownload.DownloadProgressType
import au.org.libraryforall.updater.repository.api.RepositoryPackage
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

internal class InventoryRepositoryPackage(
  private val repositoryId: UUID,
  private val repositoryPackage: RepositoryPackage,
  private val http: HTTPClientType,
  private val httpAuthentication: (URI) -> HTTPAuthentication?,
  private val directory: InventoryAPKDirectoryType,
  private val apkInstaller: APKInstallerType,
  private val resources: InventoryStringResourcesType,
  private val events: PublishSubject<InventoryEvent>,
  private val installedPackages: InstalledPackagesType,
  private val executor: ListeningExecutorService,
  initiallyInstalledVersion: NamedVersion?) : InventoryRepositoryPackageType {

  private val installedSubscription: Disposable

  private val stateLock = Object()
  private var stateActual: InventoryPackageState =
    if (initiallyInstalledVersion != null) {
      Installed(
        inventoryPackage = this,
        installedVersionCode = initiallyInstalledVersion.code,
        installedVersionName = initiallyInstalledVersion.name)
    } else {
      NotInstalled(this)
    }
    set(value) {
      synchronized(this.stateLock) {
        field = value
      }
      this.events.onNext(PackageChanged(this.repositoryId, this.id))
    }

  init {
    this.installedSubscription =
      this.installedPackages.events.ofType(InstalledPackagesChanged::class.java)
        .filter { event -> event.installedPackage.id == this.repositoryPackage.id }
        .subscribe(this::onInstalledPackageEvent)
  }

  private fun onInstalledPackageEvent(event: InstalledPackagesChanged) {
    val stateCurrent = this.stateActual
    return when (event) {
      is InstalledPackagesChanged.InstalledPackageAdded -> {
        if (stateCurrent is NotInstalled) {
          this.logger.debug("package {} became installed", this.repositoryPackage.id)
          this.stateActual = Installed(
            inventoryPackage = this,
            installedVersionCode = event.installedPackage.versionCode,
            installedVersionName = event.installedPackage.versionName)
        } else {

        }
      }
      is InstalledPackagesChanged.InstalledPackageRemoved -> {
        if (stateCurrent is Installed) {
          this.logger.debug("package {} became uninstalled", this.repositoryPackage.id)
          this.stateActual = NotInstalled(this)
        } else {

        }
      }
      is InstalledPackagesChanged.InstalledPackageUpdated -> {
        if (stateCurrent is NotInstalled) {
          this.logger.debug("package {} became installed", this.repositoryPackage.id)
          this.stateActual = Installed(
            inventoryPackage = this,
            installedVersionCode = event.installedPackage.versionCode,
            installedVersionName = event.installedPackage.versionName)
        } else {

        }
      }
    }
  }

  override val sourceURI: URI
    get() = this.repositoryPackage.source

  override val id: String
    get() = this.repositoryPackage.id

  override val versionCode: Int
    get() = this.repositoryPackage.versionCode

  override val versionName: String
    get() = this.repositoryPackage.versionName

  override val name: String
    get() = this.repositoryPackage.name

  private val logger =
    LoggerFactory.getLogger(InventoryRepositoryPackage::class.java)

  override fun install(activity: Any): ListenableFuture<InventoryPackageInstallResult> {
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
          Futures.immediateFuture(InventoryPackageInstallResult(
            repositoryId = this.repositoryId,
            packageVersionCode = this.versionCode,
            packageVersionName = this.versionName,
            packageName = this.name,
            packageURI = this.repositoryPackage.source,
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
        is Installed -> state.installedVersionCode < this.repositoryPackage.versionCode
        is InstallFailed -> true
        is Installing -> false
      }
    }
  }

  private fun runInstall(activity: Any): ListenableFuture<InventoryPackageInstallResult> {
    val step0 = InventoryTaskStep(description = this.resources.installStarted)

    return this.executor.submit(Callable<InventoryPackageInstallResult> {
      runInstallActual(step0, activity)
    })
  }

  private fun runInstallActual(
    initialStep: InventoryTaskStep,
    activity: Any
  ): InventoryPackageInstallResult {
    val result =
      try {
        this.directory.withKey(this.repositoryPackage.sha256) { reservation ->
          InventoryTaskMonad.startWithStep(initialStep)
            .flatMap {
              InventoryTaskDownload(
                resources = this.resources,
                http = this.http,
                httpAuthentication = this.httpAuthentication,
                reservation = reservation,
                onVerificationProgress = this::onInstallVerificationProgress,
                uri = this.repositoryPackage.source,
                onDownloadProgress = this::onInstallDownloadProgress
              ).execute()
            }.flatMap {
              InventoryTaskVerify(
                resources = this.resources,
                reservation = reservation,
                onVerificationProgress = this::onInstallVerificationProgress
              ).execute()
            }.flatMap {
              this.stateActual =
                Installing(
                  this,
                  InstallingStatusIndefinite(
                    status = this.resources.installWaitingForInstaller))

              InventoryTaskInstallAPK(
                activity,
                resources = this.resources,
                packageName = this.id,
                packageVersionCode = this.versionCode,
                file = reservation.file,
                apkInstaller = this.apkInstaller
              ).execute()
            }
        }
      } catch (e: Exception) {
        initialStep.resolution = this.resources.installDownloadReservationFailed(e)
        initialStep.failed = true
        initialStep.exception = e
        InventoryTaskMonad.InventoryTaskFailed<File>(listOf(initialStep))
      }

    val installResult =
      InventoryPackageInstallResult(
        this.repositoryId,
        this.id,
        this.versionCode,
        this.versionName,
        this.repositoryPackage.source,
        result.steps)

    return when (result) {
      is InventoryTaskMonad.InventoryTaskSuccess -> {
        this.stateActual = Installed(this, this.versionCode, this.versionName)
        installResult
      }
      is InventoryTaskMonad.InventoryTaskFailed -> {
        this.stateActual = InstallFailed(this, installResult)
        installResult
      }
    }
  }

  private fun onInstallDownloadProgress(progress: DownloadProgressType) {
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

  override val state: InventoryPackageState
    get() = synchronized(this.stateLock) { this.stateActual }

}