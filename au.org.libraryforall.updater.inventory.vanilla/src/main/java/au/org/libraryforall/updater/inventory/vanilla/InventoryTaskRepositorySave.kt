package au.org.libraryforall.updater.inventory.vanilla

import au.org.libraryforall.updater.inventory.api.InventoryRepositoryDatabaseEntryType
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryDatabaseType
import au.org.libraryforall.updater.inventory.api.InventoryStringResourcesType
import au.org.libraryforall.updater.inventory.api.InventoryTaskStep
import au.org.libraryforall.updater.repository.api.Repository
import org.slf4j.LoggerFactory

class InventoryTaskRepositorySave(
  private val resources: InventoryStringResourcesType,
  private val database: InventoryRepositoryDatabaseType,
  private val repository: Repository) {

  private val logger = LoggerFactory.getLogger(InventoryTaskRepositorySave::class.java)

  fun execute(): InventoryTaskMonad<InventoryRepositoryDatabaseEntryType> {
    this.logger.debug("save: {}", this.repository.id)

    val step =
      InventoryTaskStep(
        description = this.resources.inventoryRepositorySaving(this.repository.id),
        resolution = "",
        exception = null,
        failed = false)

    return InventoryTaskMonad.startWithStep(step).flatMap {
      try {
        val entry = this.database.createOrUpdate(this.repository)
        step.resolution = this.resources.inventoryRepositorySavingSucceeded(this.repository.id)
        step.failed = false
        InventoryTaskMonad.InventoryTaskSuccess(entry, listOf(step))
      } catch (e: Exception) {
        this.logger.error("save: {}: failed: ", this.repository.id, e)
        step.resolution = this.resources.inventoryRepositorySavingFailed(this.repository.id)
        step.exception = e
        step.failed = true
        InventoryTaskMonad.InventoryTaskFailed<InventoryRepositoryDatabaseEntryType>(listOf(step))
      }
    }
  }
}