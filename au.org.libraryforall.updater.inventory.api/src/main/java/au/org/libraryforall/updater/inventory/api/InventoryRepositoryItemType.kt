package au.org.libraryforall.updater.inventory.api

import com.google.common.util.concurrent.ListenableFuture
import net.jcip.annotations.ThreadSafe
import java.net.URI

@ThreadSafe
interface InventoryRepositoryItemType {

  val isUpdateAvailable: Boolean

  val id: String

  val versionCode: Long

  val versionName: String

  val name: String

  val state: InventoryItemState

  val sourceURI: URI

  fun install(activity: Any): ListenableFuture<InventoryItemInstallResult>

  fun cancel()
}
