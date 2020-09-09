package one.lfa.updater.inventory.api

import java.io.Serializable
import java.net.URI

data class InventoryRepositoryAddResult(
  val uri: URI,
  val repository: InventoryRepositoryType?,
  val steps: List<InventoryTaskStep>
): Serializable
