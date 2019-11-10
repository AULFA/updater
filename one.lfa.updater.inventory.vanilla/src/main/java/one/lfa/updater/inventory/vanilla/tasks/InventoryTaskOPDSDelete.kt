package one.lfa.updater.inventory.vanilla.tasks

import one.lfa.updater.inventory.api.InventoryProgress
import one.lfa.updater.inventory.api.InventoryProgressValue
import one.lfa.updater.inventory.api.InventoryStringOPDSResourcesType
import one.lfa.updater.inventory.api.InventoryStringResourcesType
import one.lfa.updater.inventory.api.InventoryTaskStep
import one.lfa.updater.inventory.vanilla.InventoryOPDSPlanning
import one.lfa.updater.inventory.vanilla.InventoryOPDSRemovalOperation
import one.lfa.updater.inventory.vanilla.InventoryOPDSRemovalOperation.DeleteLocalDirectory
import one.lfa.updater.inventory.vanilla.InventoryOPDSRemovalOperation.DeleteLocalFile
import one.lfa.updater.opds.database.api.OPDSDatabaseType
import org.slf4j.LoggerFactory
import java.io.File
import java.util.UUID

/**
 * A task that, when evaluated, downloads an OPDS manifest and parses it.
 */

object InventoryTaskOPDSDelete {

  private val logger =
    LoggerFactory.getLogger(InventoryTaskOPDSDelete::class.java)

  fun create(
    id: UUID
  ): InventoryTask<Unit> {
    return InventoryTask { execution ->
      this.logger.debug("delete catalog {}", id)
      delete(execution, id)
    }.flatMap {
      updateDatabaseTask(id)
    }
  }

  private fun updateDatabaseTask(id: UUID): InventoryTask<Unit> {
    return InventoryTask { execution ->
      updateDatabase(execution, id)
    }
  }

  private fun updateDatabase(
    execution: InventoryTaskExecutionType,
    id: UUID
  ): InventoryTaskResult<Unit> {
    this.logger.debug("updating database entry for {}", id)

    val opdsStrings =
      execution.services.requireService(InventoryStringResourcesType::class.java)
        as InventoryStringOPDSResourcesType
    val opdsDatabase =
      execution.services.requireService(OPDSDatabaseType::class.java)

    val step =
      InventoryTaskStep(opdsStrings.opdsCatalogDeletingDatabaseEntry)

    opdsDatabase.delete(id)
    return InventoryTaskResult.succeeded(Unit, step)
  }

  private fun delete(
    execution: InventoryTaskExecutionType,
    id: UUID
  ): InventoryTaskResult<Unit> {

    if (execution.isCancelRequested) {
      return InventoryTaskResult.InventoryTaskCancelled(listOf())
    }

    val opdsStrings =
      execution.services.requireService(InventoryStringResourcesType::class.java)
        as InventoryStringOPDSResourcesType
    val opdsDatabase =
      execution.services.requireService(OPDSDatabaseType::class.java)

    val step =
      InventoryTaskStep(opdsStrings.opdsCatalogDeleting)

    val entry = opdsDatabase.open(id)
    return if (entry != null) {
      val plan =
        InventoryOPDSPlanning.planDeletion(entry.directory)

      val tasks = plan.mapIndexed { index, op ->
        taskOfOp(opdsStrings, index, plan.size, op)
      }

      return InventoryTask.succeeded(step, Unit)
        .flatMap { InventoryTask.sequenceUnit(tasks) }
        .evaluate(execution)
    } else {
      step.failed = true
      step.resolution = opdsStrings.opdsDatabaseEntryMissing
      InventoryTaskResult.failed(step)
    }
  }

  private fun taskOfOp(
    strings: InventoryStringOPDSResourcesType,
    index: Int,
    size: Int,
    operation: InventoryOPDSRemovalOperation
  ): InventoryTask<Unit> {
    return when (operation) {
      is DeleteLocalFile ->
        InventoryTask { execution ->
          return@InventoryTask deleteOne(execution, strings, index, size, operation.localFile)
        }
      is DeleteLocalDirectory ->
        InventoryTask { execution ->
          return@InventoryTask deleteOne(execution, strings, index, size, operation.localFile)
        }
    }
  }

  private fun deleteOne(
    execution: InventoryTaskExecutionType,
    strings: InventoryStringOPDSResourcesType,
    index: Int,
    size: Int,
    file: File
  ): InventoryTaskResult<Unit> {
    this.logger.debug("delete {}", file)

    val progressMajor = null
    val progressMinor =
      InventoryProgressValue.InventoryProgressValueDefinite(
        current = index.toLong(),
        perSecond = 1L,
        maximum = size.toLong()
      )

    val status = strings.opdsCatalogDeletingFile(index, size)
    execution.onProgress(InventoryProgress(
      progressMajor,
      progressMinor,
      status
    ))

    val step = InventoryTaskStep(status)
    val deleted = file.delete()
    if (!deleted) {
      if (file.exists()) {
        step.failed = true
        step.resolution = strings.opdsCatalogDeletingFileFailed(index, size, file)
        return InventoryTaskResult.failed(step)
      }
    }
    return InventoryTaskResult.succeeded(Unit, step)
  }
}
