package au.org.libraryforall.updater.inventory.vanilla

import au.org.libraryforall.updater.apkinstaller.api.APKInstallerType
import au.org.libraryforall.updater.inventory.api.InventoryStringResourcesType
import au.org.libraryforall.updater.inventory.api.InventoryTaskStep
import org.slf4j.LoggerFactory
import java.io.File

class InventoryTaskInstallAPK(
  private val activity: Any,
  private val resources: InventoryStringResourcesType,
  private val packageName: String,
  private val packageVersionCode: Int,
  private val file: File,
  private val apkInstaller: APKInstallerType
) {

  private val logger = LoggerFactory.getLogger(InventoryTaskInstallAPK::class.java)

  /*
   * The bizarre choice of value for standard "operation succeeded" results on Android. We
   * assume any other value means something went wrong.
   */

  private val ACTIVITY_RESULT_OK = -1

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
      val status = task.future.get()
      this.logger.debug("install task returned")

      if (status == ACTIVITY_RESULT_OK) {
        step.resolution = this.resources.installAPKSucceeded(status)
        step.failed = false
        InventoryTaskMonad.InventoryTaskSuccess(Unit)
      } else {
        step.resolution = this.resources.installAPKFailedWithCode(status)
        step.failed = true
        InventoryTaskMonad.InventoryTaskFailed<Unit>()
      }
    } catch (e: Exception) {
      this.logger.error("APK install failed: ", e)
      step.resolution = this.resources.installAPKFailedWithException(e)
      step.failed = true
      InventoryTaskMonad.InventoryTaskFailed<Unit>()
    }
  }

}
