package one.lfa.updater.inventory.api

import java.util.UUID

/**
 * The type of events published by the repository database.
 */

sealed class InventoryRepositoryDatabaseEvent {

  /**
   * The repository ID to which the event refers.
   */

  abstract val repositoryID: UUID

  /**
   * A repository was added to the database.
   */

  data class DatabaseRepositoryAdded(
    override val repositoryID: UUID
  ) : InventoryRepositoryDatabaseEvent()

  /**
   * A repository was removed from the database.
   */

  data class DatabaseRepositoryRemoved(
    override val repositoryID: UUID
  ) : InventoryRepositoryDatabaseEvent()

  /**
   * A repository was updated within the database.
   */

  data class DatabaseRepositoryUpdated(
    override val repositoryID: UUID
  ) : InventoryRepositoryDatabaseEvent()
}
