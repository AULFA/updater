package au.org.libraryforall.updater.inventory.api

import java.io.Serializable
import java.net.URI
import java.util.UUID

data class InventoryItemInstallResult(
  val repositoryId: UUID,
  val itemName: String,
  val itemVersionCode: Long,
  val itemVersionName: String,
  val itemURI: URI,
  val steps: List<InventoryTaskStep>
): Serializable