package au.org.libraryforall.updater.inventory.api

import com.google.common.util.concurrent.ListenableFuture
import net.jcip.annotations.ThreadSafe

@ThreadSafe
interface InventoryRepositoryPackageType {

  val isUpdateAvailable: Boolean

  val id: String

  val versionCode: Int

  val versionName: String

  val name: String

  val state: InventoryPackageState

  fun install(activity: Any): ListenableFuture<InventoryPackageInstallResult>
}
