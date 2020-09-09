package one.lfa.updater.inventory.api

import java.util.UUID

/**
 * The state of a repository within the inventory.
 */

sealed class InventoryRepositoryState {

  /**
   * The globally-unique ID of the repository.
   */

  abstract val id: UUID

  /**
   * The repository is currently in the process of updating.
   */

  data class RepositoryUpdating(
    override val id: UUID
  ) : InventoryRepositoryState()

  /**
   * The most recent attempt to update this repository failed.
   */

  data class RepositoryUpdateFailed(
    override val id: UUID,
    val steps: List<InventoryTaskStep>
  ) : InventoryRepositoryState()

  /**
   * The repository is idle.
   */

  data class RepositoryIdle(
    override val id: UUID
  ) : InventoryRepositoryState()
}
