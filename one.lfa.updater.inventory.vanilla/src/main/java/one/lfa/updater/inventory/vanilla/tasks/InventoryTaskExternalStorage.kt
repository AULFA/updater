package one.lfa.updater.inventory.vanilla.tasks

import one.lfa.updater.inventory.api.InventoryExternalStorageServiceType
import one.lfa.updater.inventory.api.InventoryStringResourcesType
import one.lfa.updater.inventory.api.InventoryTaskStep
import org.slf4j.LoggerFactory
import java.net.URI

object InventoryTaskExternalStorage {

  private val logger =
    LoggerFactory.getLogger(InventoryTaskExternalStorage::class.java)

  fun resolveExternalInputFileTask(
    uri: URI
  ): InventoryTask<InventoryPossiblySizedInputStream> {
    return InventoryTask { execution ->
      this.resolveExternalInputFile(execution, uri)
    }
  }

  private fun resolveExternalInputFile(
    execution: InventoryTaskExecutionType,
    uri: URI
  ): InventoryTaskResult<InventoryPossiblySizedInputStream> {
    val strings =
      execution.services.requireService(InventoryStringResourcesType::class.java)
    val external =
      execution.services.requireService(InventoryExternalStorageServiceType::class.java)

    val step =
      InventoryTaskStep(
        description = strings.fileFinding,
        resolution = "",
        exception = null,
        failed = false
      )

    val file = external.findFile(uri)
    if (file != null) {
      return try {
        InventoryTaskResult.succeeded(
          result = InventoryPossiblySizedInputStream(file.length(), file.inputStream()),
          step = step
        )
      } catch (e: Exception) {
        this.logger.error("error opening file: ", e)
        step.resolution = strings.fileOpeningFailed(e)
        step.exception = e
        step.failed = true
        InventoryTaskResult.failed(step)
      }
    }

    this.logger.error("could not resolve {} to a file", uri)
    step.resolution = strings.fileDoesNotExist
    step.failed = true
    return InventoryTaskResult.failed(step)
  }
}