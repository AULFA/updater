package one.lfa.updater.inventory.api

import one.lfa.updater.repository.api.Repository
import io.reactivex.Observable
import java.util.UUID

interface InventoryRepositoryDatabaseType {

  val events: Observable<InventoryRepositoryDatabaseEvent>

  fun createOrUpdate(
    repository: Repository
  ): InventoryRepositoryDatabaseEntryType

  fun delete(id: UUID)

  val entries: List<InventoryRepositoryDatabaseEntryType>

}
