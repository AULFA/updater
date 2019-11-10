package one.lfa.updater.inventory.api

import java.io.Serializable
import java.net.URI
import java.util.UUID

data class InventoryItemResult(
  val repositoryId: UUID,
  val itemName: String,
  val itemVersionCode: Long,
  val itemVersionName: String,
  val itemURI: URI,
  val steps: List<InventoryTaskStep>
): Serializable
