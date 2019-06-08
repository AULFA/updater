package au.org.libraryforall.updater.inventory.api

import au.org.libraryforall.updater.repository.api.Repository

interface InventoryRepositoryDatabaseEntryType {

  val database: InventoryRepositoryDatabaseType

  val repository: Repository

  fun update(repository: Repository)

}
