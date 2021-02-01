package one.lfa.updater.inventory.api

import com.google.common.util.concurrent.ListenableFuture
import io.reactivex.Observable
import net.jcip.annotations.ThreadSafe
import org.joda.time.LocalDateTime
import java.net.URI
import java.util.UUID

/**
 * The run-time representation of a repository as it appears in the inventory. Implementations
 * of this class effectively expose a stream of events indicating updates to the repository, and
 * allow for performing update operations that (for example) download updates from remote servers.
 */

@ThreadSafe
interface InventoryRepositoryType {

  /**
   * A stream of inventory events relating to this repository.
   */

  val events: Observable<InventoryEvent>

  /**
   * The globally-unique repository ID.
   */

  val id: UUID

  /**
   * The repository title.
   */

  val title: String

  /**
   * The time the repository was last (successfully) updated.
   */

  val updated: LocalDateTime

  /**
   * The URI that can be used to update this repository.
   */

  val updateURI: URI

  /**
   * The items exposed by this repository.
   */

  val items: List<InventoryRepositoryItemType>

  /**
   * The current repository state.
   */

  val state: InventoryRepositoryState

  /**
   * Attempt to update this repository.
   */

  fun update(): ListenableFuture<Unit>

  /**
   * `true` if this repository is a "testing" repository (not a production repository)
   */

  val isTesting: Boolean
}
