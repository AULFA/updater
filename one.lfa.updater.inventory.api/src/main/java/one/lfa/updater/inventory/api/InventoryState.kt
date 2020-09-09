package one.lfa.updater.inventory.api

import java.net.URI

/**
 * The state of the inventory.
 */

sealed class InventoryState {

  /**
   * The inventory is idle.
   */

  object InventoryIdle : InventoryState()

  /**
   * A repository with the given URI is currently being added to the inventory.
   */

  data class InventoryAddingRepository(
    val uri: URI
  ) : InventoryState()

  /**
   * A repository with the given URI could not be added to the inventory.
   */

  data class InventoryAddingRepositoryFailed(
    val uri: URI,
    val steps: List<InventoryTaskStep>
  ) : InventoryState()
}