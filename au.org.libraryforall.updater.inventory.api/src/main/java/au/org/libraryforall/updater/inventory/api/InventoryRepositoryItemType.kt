package au.org.libraryforall.updater.inventory.api

import au.org.libraryforall.updater.repository.api.RepositoryItem
import com.google.common.util.concurrent.ListenableFuture
import net.jcip.annotations.ThreadSafe
import java.net.URI

@ThreadSafe
interface InventoryRepositoryItemType {

  val isUpdateAvailable: Boolean

  val state: InventoryItemState

  val item: RepositoryItem

  fun install(activity: Any): ListenableFuture<InventoryItemInstallResult>

  fun cancel()
}
