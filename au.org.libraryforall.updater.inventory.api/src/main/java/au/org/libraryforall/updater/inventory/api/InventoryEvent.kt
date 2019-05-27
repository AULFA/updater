package au.org.libraryforall.updater.inventory.api

import java.util.UUID

sealed class InventoryEvent {

  sealed class InventoryRepositoryEvent : InventoryEvent() {

    abstract val repositoryId: UUID

    sealed class InventoryRepositoryPackageEvent : InventoryRepositoryEvent() {

      abstract val packageId: String

      data class PackageBecameVisible(
        override val repositoryId: UUID,
        override val packageId: String) :
        InventoryRepositoryPackageEvent()

      data class PackageChanged(
        override val repositoryId: UUID,
        override val packageId: String) :
        InventoryRepositoryPackageEvent()

      data class PackageBecameInvisible(
        override val repositoryId: UUID,
        override val packageId: String) :
        InventoryRepositoryPackageEvent()
    }
  }
}
