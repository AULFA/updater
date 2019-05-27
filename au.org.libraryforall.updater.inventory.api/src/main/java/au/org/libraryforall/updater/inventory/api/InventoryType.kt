package au.org.libraryforall.updater.inventory.api

import au.org.libraryforall.updater.repository.api.Repository
import net.jcip.annotations.ThreadSafe
import java.util.UUID

@ThreadSafe
interface InventoryType {

  fun inventoryRepositoryPut(repository: Repository): InventoryRepositoryType

  fun inventoryRepositorySelect(id: UUID): InventoryRepositoryType?

  fun inventoryRepositories(): List<UUID>

}
