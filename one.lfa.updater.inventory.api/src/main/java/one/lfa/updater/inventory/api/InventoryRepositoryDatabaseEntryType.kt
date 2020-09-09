package one.lfa.updater.inventory.api

import net.jcip.annotations.ThreadSafe
import one.lfa.updater.repository.api.Repository

/**
 * An entry in a repository database.
 */

@ThreadSafe
interface InventoryRepositoryDatabaseEntryType {

  /**
   * The database to which this entry belongs.
   */

  val database: InventoryRepositoryDatabaseType

  /**
   * The current repository value.
   */

  val repository: Repository

  /**
   * Write the given repository value into this database entry.
   */

  fun update(repository: Repository)
}
