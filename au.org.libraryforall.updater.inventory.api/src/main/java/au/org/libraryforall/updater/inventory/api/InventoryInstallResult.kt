package au.org.libraryforall.updater.inventory.api

import java.io.Serializable
import java.net.URI
import java.util.UUID

data class InventoryInstallResult(
  val repositoryId: UUID,
  val packageName: String,
  val packageVersionCode: Int,
  val packageVersionName: String,
  val packageURI: URI,
  val steps: List<InventoryTaskStep>): Serializable
