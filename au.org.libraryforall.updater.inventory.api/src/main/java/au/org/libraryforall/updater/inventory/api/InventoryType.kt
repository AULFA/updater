package au.org.libraryforall.updater.inventory.api

import au.org.libraryforall.updater.repository.api.Repository
import com.google.common.util.concurrent.ListenableFuture
import io.reactivex.Observable
import net.jcip.annotations.ThreadSafe
import java.net.URI
import java.util.UUID

/**
 * The main inventory interface.
 *
 * Implementations are required to be thread-safe.
 */

@ThreadSafe
interface InventoryType {

  /**
   * The current state of the inventory.
   */

  val state: InventoryState

  /**
   * An observable that publishes events related to the inventory.
   */

  val events: Observable<InventoryEvent>

  /**
   * Fetch the repository at `uri`, adding a requirement that it has the uuid `requiredUUID`
   * if desired, and add it to the inventory.
   */

  fun inventoryRepositoryAdd(
    uri: URI,
    requiredUUID: UUID? = null)
    : ListenableFuture<InventoryRepositoryAddResult>

  /**
   * Unconditionally add the given repository to the inventory.
   */

  fun inventoryRepositoryPut(
    repository: Repository)
    : ListenableFuture<InventoryRepositoryAddResult>

  /**
   * Find the repository with the given UUID.
   */

  fun inventoryRepositorySelect(
    id: UUID)
    : InventoryRepositoryType?

  /**
   * List all available repositories.
   */

  fun inventoryRepositories(): List<InventoryRepositoryType>

  /**
   * Remove the repository with the given UUID, if it exists.
   */

  fun inventoryRepositoryRemove(
    id: UUID)
    : ListenableFuture<InventoryRepositoryRemoveResult>

}
