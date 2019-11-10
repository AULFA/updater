package one.lfa.updater.inventory.api

import one.lfa.updater.repository.api.RepositoryItem
import com.google.common.util.concurrent.ListenableFuture
import net.jcip.annotations.ThreadSafe

@ThreadSafe
interface InventoryRepositoryItemType {

  val isUpdateAvailable: Boolean

  val state: InventoryItemState

  val item: RepositoryItem

  fun install(activity: Any): ListenableFuture<InventoryItemResult>

  fun uninstall(activity: Any): ListenableFuture<InventoryItemResult>

  fun cancel()
}
