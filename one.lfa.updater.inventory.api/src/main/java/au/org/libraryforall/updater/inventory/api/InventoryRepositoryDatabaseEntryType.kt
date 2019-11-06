package au.org.libraryforall.updater.inventory.api

import one.lfa.updater.repository.api.Repository

interface InventoryRepositoryDatabaseEntryType {

  val database: InventoryRepositoryDatabaseType

  val repository: Repository

  fun update(repository: Repository)

}
