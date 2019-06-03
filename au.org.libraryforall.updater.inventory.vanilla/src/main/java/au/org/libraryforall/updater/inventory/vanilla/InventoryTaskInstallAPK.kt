package au.org.libraryforall.updater.inventory.vanilla

import au.org.libraryforall.updater.apkinstaller.api.APKInstallTaskType
import au.org.libraryforall.updater.apkinstaller.api.APKInstallerType
import au.org.libraryforall.updater.inventory.api.InventoryStringResourcesType
import au.org.libraryforall.updater.inventory.api.InventoryTaskStep
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.TimeUnit

class InventoryTaskInstallAPK(
  private val activity: Any,
  private val resources: InventoryStringResourcesType,
  private val packageName: String,
  private val packageVersionCode: Int,
  private val file: File,
  private val apkInstaller: APKInstallerType
) {

  private val logger = LoggerFactory.getLogger(InventoryTaskInstallAPK::class.java)

  fun execute(): InventoryTaskMonad<Unit> {
    val step = InventoryTaskStep(description = this.resources.installAPKStarted)
    return InventoryTaskMonad.startWithStep(step).flatMap { runAPK(step) }
  }

  private fun runAPK(step: InventoryTaskStep): InventoryTaskMonad<Unit> {
    return try {
      val task =
        this.apkInstaller.createInstallTask(
          activity = this.activity,
          packageName = this.packageName,
          packageVersionCode = this.packageVersionCode,
          file = this.file)

      this.logger.debug("waiting for install task")
      val status = task.future.get(3L, TimeUnit.MINUTES)
      this.logger.debug("install task returned")

      when (status) {
        is APKInstallTaskType.Status.Failed -> {
          step.resolution = this.resources.installAPKFailedWithCode(status.errorCode)
          step.failed = true
          InventoryTaskMonad.InventoryTaskFailed()
        }
        APKInstallTaskType.Status.Cancelled -> {
          step.resolution = this.resources.installAPKCancelled
          step.failed = false
          InventoryTaskMonad.InventoryTaskSuccess(Unit)
        }
        APKInstallTaskType.Status.Succeeded -> {
          step.resolution = this.resources.installAPKSucceeded
          step.failed = false
          InventoryTaskMonad.InventoryTaskSuccess(Unit)
        }
      }
    } catch (e: Exception) {
      this.logger.error("APK install failed: ", e)
      step.resolution = this.resources.installAPKFailedWithException(e)
      step.failed = true
      InventoryTaskMonad.InventoryTaskFailed()
    }
  }

}
