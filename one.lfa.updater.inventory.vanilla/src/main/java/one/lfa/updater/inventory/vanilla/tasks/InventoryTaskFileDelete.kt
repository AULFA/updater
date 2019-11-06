package one.lfa.updater.inventory.vanilla.tasks

import one.lfa.updater.inventory.api.InventoryStringResourcesType
import one.lfa.updater.inventory.api.InventoryTaskStep
import org.slf4j.LoggerFactory
import java.io.File

/**
 * A task that deletes a local file.
 */

object InventoryTaskFileDelete {

  private val logger = LoggerFactory.getLogger(InventoryTaskFileDelete::class.java)

  /**
   * Create a task that will, when evaluated, delete a local file.
   */

  fun create(
    file: File
  ): InventoryTask<Unit> {
    return InventoryTask { execution ->
      verify(execution, file)
    }
  }

  private fun verify(
    execution: InventoryTaskExecutionType,
    file: File
  ): InventoryTaskResult<Unit> {
    logger.debug("delete: {}", file)

    val strings =
      execution.services.requireService(InventoryStringResourcesType::class.java)

    val step =
      InventoryTaskStep(
        description = strings.fileDeleting,
        resolution = "",
        exception = null,
        failed = false)

    if (!file.exists()) {
      step.resolution = strings.fileDoesNotExist
      return InventoryTaskResult.succeeded(Unit, step)
    }

    if (!file.delete()) {
      if (file.exists()) {
        step.failed = true
        step.resolution = strings.fileCouldNotDelete
        return InventoryTaskResult.failed(step)
      }
    }

    return InventoryTaskResult.succeeded(Unit, step)
  }
}