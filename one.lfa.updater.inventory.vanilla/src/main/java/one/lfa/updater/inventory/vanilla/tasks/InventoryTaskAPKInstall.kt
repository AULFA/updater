package one.lfa.updater.inventory.vanilla.tasks

import one.lfa.updater.apkinstaller.api.APKInstallTaskType
import one.lfa.updater.apkinstaller.api.APKInstallerType
import one.lfa.updater.inventory.api.InventoryProgress
import one.lfa.updater.inventory.api.InventoryProgressValue
import one.lfa.updater.inventory.api.InventoryStringResourcesType
import one.lfa.updater.inventory.api.InventoryTaskStep
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * A task that, when evaluated, installs an APK file.
 */

object InventoryTaskAPKInstall {

  private val logger = LoggerFactory.getLogger(InventoryTaskAPKInstall.javaClass)

  fun create(
    activity: Any,
    packageName: String,
    packageVersionCode: Int,
    apkFile: File
  ): InventoryTask<Unit> {
    return InventoryTask { execution ->

      val strings =
        execution.services.requireService(InventoryStringResourcesType::class.java)
      val apkInstaller =
        execution.services.requireService(APKInstallerType::class.java)

      val step =
        InventoryTaskStep(
          description = strings.installAPKStarted,
          resolution = "",
          exception = null,
          failed = false
        )

      try {
        execution.onProgress.invoke(InventoryProgress(
          major = null,
          minor = InventoryProgressValue.InventoryProgressValueIndefinite(0L, 0L),
          status = step.description))

        val task =
          apkInstaller.createInstallTask(
            activity = activity,
            packageName = packageName,
            packageVersionCode = packageVersionCode,
            file = apkFile
          )

        logger.debug("waiting for install task")
        val status = task.future.get(10L, TimeUnit.MINUTES)
        logger.debug("install task returned")

        when (status) {
          is APKInstallTaskType.Status.Failed -> {
            step.resolution = strings.installAPKFailedWithCode(status.errorCode)
            step.failed = true
            InventoryTaskResult.failed<Unit>(step)
          }
          APKInstallTaskType.Status.Cancelled -> {
            step.resolution = strings.installAPKCancelled
            step.failed = false
            InventoryTaskResult.cancelled(step)
          }
          APKInstallTaskType.Status.Succeeded -> {
            step.resolution = strings.installAPKSucceeded
            step.failed = false
            InventoryTaskResult.succeeded(Unit, step)
          }
        }
      } catch (e: Exception) {
        logger.error("APK install failed: ", e)
        step.failed = true
        step.exception = e
        InventoryTaskResult.failed<Unit>(step)
      }
    }
  }

}
