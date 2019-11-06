package one.lfa.updater.inventory.api

import java.util.UUID

sealed class InventoryEvent {

  object InventoryStateChanged : InventoryEvent()

  sealed class InventoryRepositoryEvent : InventoryEvent() {

    abstract val repositoryId: UUID

    sealed class InventoryRepositoryItemEvent : InventoryRepositoryEvent() {

      abstract val itemId: String

      data class ItemBecameVisible(
        override val repositoryId: UUID,
        override val itemId: String) :
        InventoryRepositoryItemEvent()

      data class ItemChanged(
        override val repositoryId: UUID,
        override val itemId: String) :
        InventoryRepositoryItemEvent()

      data class ItemBecameInvisible(
        override val repositoryId: UUID,
        override val itemId: String) :
        InventoryRepositoryItemEvent()
    }

    data class RepositoryChanged(
      override val repositoryId: UUID)
      : InventoryRepositoryEvent()
  }
}
