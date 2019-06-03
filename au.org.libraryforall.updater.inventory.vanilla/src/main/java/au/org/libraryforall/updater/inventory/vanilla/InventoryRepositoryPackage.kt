package au.org.libraryforall.updater.inventory.vanilla

import au.org.libraryforall.updater.apkinstaller.api.APKInstallerType
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
import au.org.libraryforall.updater.repository.api.RepositoryPackage
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
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
  private val executor: ListeningExecutorService) : InventoryRepositoryPackageType {

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
    get() = false

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
                uri = this.repositoryPackage.source
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
        this.stateActual = Installed(this)
        installResult
      }
      is InventoryTaskMonad.InventoryTaskFailed -> {
        this.stateActual = InstallFailed(this, installResult)
        installResult
      }
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

  private val stateLock = Object()
  private var stateActual: InventoryPackageState =
    NotInstalled(this)
    set(value) {
      synchronized(this.stateLock) {
        field = value
      }
      this.events.onNext(PackageChanged(this.repositoryId, this.id))
    }

  override val state: InventoryPackageState
    get() = synchronized(this.stateLock) { this.stateActual }

}