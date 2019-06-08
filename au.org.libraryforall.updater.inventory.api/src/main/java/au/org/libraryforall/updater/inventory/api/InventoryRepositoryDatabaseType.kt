package au.org.libraryforall.updater.inventory.api

import au.org.libraryforall.updater.repository.api.Repository
import io.reactivex.Observable
import java.util.UUID

interface InventoryRepositoryDatabaseType {

  val events: Observable<InventoryRepositoryDatabaseEvent>

  fun createOrUpdate(
    repository: Repository)
    : InventoryRepositoryDatabaseEntryType

  fun delete(id: UUID)

  val entries: List<InventoryRepositoryDatabaseEntryType>

}
