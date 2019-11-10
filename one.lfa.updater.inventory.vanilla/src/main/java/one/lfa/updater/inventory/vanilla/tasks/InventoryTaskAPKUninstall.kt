package one.lfa.updater.inventory.vanilla.tasks

import one.lfa.updater.apkinstaller.api.APKInstallerStatus
import one.lfa.updater.apkinstaller.api.APKInstallerType
import one.lfa.updater.inventory.api.InventoryProgress
import one.lfa.updater.inventory.api.InventoryProgressValue
import one.lfa.updater.inventory.api.InventoryStringResourcesType
import one.lfa.updater.inventory.api.InventoryTaskStep
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * A task that, when evaluated, uninstalls an APK file.
 */

object InventoryTaskAPKUninstall {

  private val logger = LoggerFactory.getLogger(InventoryTaskAPKUninstall.javaClass)

  fun create(
    activity: Any,
    packageName: String
  ): InventoryTask<Unit> {
    return InventoryTask { execution ->

      val strings =
        execution.services.requireService(InventoryStringResourcesType::class.java)
      val apkInstaller =
        execution.services.requireService(APKInstallerType::class.java)

      val step =
        InventoryTaskStep(
          description = strings.uninstallAPKStarted,
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
          apkInstaller.createUninstallTask(
            activity = activity,
            packageName = packageName
          )

        logger.debug("waiting for uninstall task")
        val status = task.future.get(10L, TimeUnit.MINUTES)
        logger.debug("uninstall task returned")

        when (status) {
          is APKInstallerStatus.Failed -> {
            step.resolution = strings.uninstallAPKFailedWithCode(status.errorCode)
            step.failed = true
            InventoryTaskResult.failed<Unit>(step)
          }
          APKInstallerStatus.Cancelled -> {
            step.resolution = strings.uninstallAPKCancelled
            step.failed = false
            InventoryTaskResult.cancelled(step)
          }
          APKInstallerStatus.Succeeded -> {
            step.resolution = strings.uninstallAPKSucceeded
            step.failed = false
            InventoryTaskResult.succeeded(Unit, step)
          }
        }
      } catch (e: Exception) {
        logger.error("APK uninstall failed: ", e)
        step.failed = true
        step.exception = e
        InventoryTaskResult.failed<Unit>(step)
      }
    }
  }

}
