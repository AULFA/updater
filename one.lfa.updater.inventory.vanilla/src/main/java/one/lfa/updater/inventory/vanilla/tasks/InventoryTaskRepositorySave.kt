package one.lfa.updater.inventory.vanilla.tasks

import one.lfa.updater.inventory.api.InventoryRepositoryDatabaseEntryType
import one.lfa.updater.inventory.api.InventoryRepositoryDatabaseType
import one.lfa.updater.inventory.api.InventoryStringResourcesType
import one.lfa.updater.inventory.api.InventoryTaskStep
import one.lfa.updater.repository.api.Repository
import org.slf4j.LoggerFactory

/**
 * A task that saves a given repository to the inventory database.
 */

object InventoryTaskRepositorySave {

  private val logger = LoggerFactory.getLogger(InventoryTaskRepositorySave.javaClass)

  /**
   * Create a task that, when evaluated, will save the given repository.
   */

  fun create(
    repository: Repository
  ): InventoryTask<InventoryRepositoryDatabaseEntryType> {
    return InventoryTask { execution ->
      save(execution, repository)
    }
  }

  private fun save(
    execution: InventoryTaskExecutionType,
    repository: Repository
  ): InventoryTaskResult<InventoryRepositoryDatabaseEntryType> {
    logger.debug("save: {}", repository.id)

    val strings =
      execution.services.requireService(InventoryStringResourcesType::class.java)
    val database =
      execution.services.requireService(InventoryRepositoryDatabaseType::class.java)

    val step =
      InventoryTaskStep(
        description = strings.repositorySaving(repository.id),
        resolution = "",
        exception = null,
        failed = false)

    return try {
      val entry = database.createOrUpdate(repository)
      step.resolution = strings.repositorySavingSucceeded(repository.id)
      step.failed = false
      InventoryTaskResult.succeeded(entry, step)
    } catch (e: Exception) {
      logger.error("save: {}: failed: ", repository.id, e)
      step.resolution = strings.repositorySavingFailed(repository.id)
      step.exception = e
      step.failed = true
      InventoryTaskResult.failed(step)
    }
  }

}