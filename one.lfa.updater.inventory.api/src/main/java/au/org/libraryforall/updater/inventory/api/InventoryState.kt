package au.org.libraryforall.updater.inventory.api

import java.net.URI

sealed class InventoryState {

  object InventoryIdle : InventoryState()

  data class InventoryAddingRepository(
    val uri: URI)
    : InventoryState()

  data class InventoryAddingRepositoryFailed(
    val uri: URI,
    val steps: List<InventoryTaskStep>)
    : InventoryState()

}