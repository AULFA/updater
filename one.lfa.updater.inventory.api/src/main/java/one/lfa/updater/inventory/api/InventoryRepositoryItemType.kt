package one.lfa.updater.inventory.api

import one.lfa.updater.repository.api.RepositoryItem
import com.google.common.util.concurrent.ListenableFuture
import net.jcip.annotations.ThreadSafe

/**
 * The run-time representation of an item within a repository. This is typically an APK
 * file or an OPDS catalog.
 */

@ThreadSafe
interface InventoryRepositoryItemType {

  /**
   * @return `true` if the version of the item in the repository is newer than what is currently installed
   */

  val isUpdateAvailable: Boolean

  /**
   * The state of the item.
   */

  val state: InventoryItemState

  /**
   * The raw repository item.
   */

  val item: RepositoryItem

  /**
   * Attempt to install the item. The given `activity` parameter should be an Android activity.
   */

  fun install(
    activity: Any
  ): ListenableFuture<InventoryItemResult>

  /**
   * Attempt to uninstall the item. The given `activity` parameter should be an Android activity.
   */

  fun uninstall(
    activity: Any
  ): ListenableFuture<InventoryItemResult>

  /**
   * Cancel any operation that is currently occurring on this item.
   */

  fun cancel()
}
