package one.lfa.updater.inventory.api

import one.lfa.updater.repository.api.Repository
import io.reactivex.Observable
import net.jcip.annotations.ThreadSafe
import java.util.UUID

/**
 * A database of repositories.
 */

@ThreadSafe
interface InventoryRepositoryDatabaseType {

  /**
   * A stream of events published when database items are updated.
   */

  val events: Observable<InventoryRepositoryDatabaseEvent>

  /**
   * Create a repository database entry, or update the existing entry with the given
   * repository value.
   */

  fun createOrUpdate(
    repository: Repository
  ): InventoryRepositoryDatabaseEntryType

  /**
   * Delete the repository with the given UUID.
   */

  fun delete(id: UUID)

  /**
   * A read-only list of the current database entries.
   */

  val entries: List<InventoryRepositoryDatabaseEntryType>
}
