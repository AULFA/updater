package au.org.libraryforall.updater.inventory.api

import au.org.libraryforall.updater.repository.api.Repository
import com.google.common.util.concurrent.ListenableFuture
import net.jcip.annotations.ThreadSafe
import java.net.URI
import java.util.UUID

@ThreadSafe
interface InventoryType {

  fun inventoryRepositoryAdd(
    uri: URI)
    : ListenableFuture<InventoryRepositoryAddResult>

  fun inventoryRepositoryPut(
    repository: Repository)
    : ListenableFuture<InventoryRepositoryAddResult>

  fun inventoryRepositorySelect(
    id: UUID)
    : InventoryRepositoryType?

  fun inventoryRepositories(): List<UUID>

}
