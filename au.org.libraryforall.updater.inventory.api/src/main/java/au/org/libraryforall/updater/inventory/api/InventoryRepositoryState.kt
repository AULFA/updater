package au.org.libraryforall.updater.inventory.api

import java.util.UUID

sealed class InventoryRepositoryState {

  abstract val id: UUID

  data class RepositoryUpdating(
    override val id: UUID)
    : InventoryRepositoryState()

  data class RepositoryUpdateFailed(
    override val id: UUID,
    val steps: List<InventoryTaskStep>)
    : InventoryRepositoryState()

  data class RepositoryIdle(
    override val id: UUID)
    : InventoryRepositoryState()
}
