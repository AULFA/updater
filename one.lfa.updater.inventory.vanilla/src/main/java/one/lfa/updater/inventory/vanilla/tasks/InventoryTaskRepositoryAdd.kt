package one.lfa.updater.inventory.vanilla.tasks

import one.lfa.updater.inventory.api.InventoryRepositoryDatabaseEntryType
import java.net.URI
import java.util.UUID

/**
 * A task that will download and add a repository to the database.
 */

object InventoryTaskRepositoryAdd {

  /**
   * Create a task that will, when evaluated, download the repository at the given URI,
   * check that it has the given required UUID (if one is specified), and then save it
   * to the inventory database.
   */

  fun create(
    uri: URI,
    requiredUUID: UUID?
  ): InventoryTask<InventoryRepositoryDatabaseEntryType> {
    return InventoryTaskRepositoryFetch.create(uri, requiredUUID)
      .flatMap(InventoryTaskRepositorySave::create)
  }
}