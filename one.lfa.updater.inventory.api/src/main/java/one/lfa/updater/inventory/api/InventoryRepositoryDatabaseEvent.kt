package one.lfa.updater.inventory.api

import java.util.UUID

sealed class InventoryRepositoryDatabaseEvent {

  abstract val repositoryID: UUID

  data class DatabaseRepositoryAdded(
    override val repositoryID: UUID
  ) : InventoryRepositoryDatabaseEvent()

  data class DatabaseRepositoryRemoved(
    override val repositoryID: UUID
  ) : InventoryRepositoryDatabaseEvent()

  data class DatabaseRepositoryUpdated(
    override val repositoryID: UUID
  ) : InventoryRepositoryDatabaseEvent()

}
