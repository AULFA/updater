package au.org.libraryforall.updater.inventory.api

import java.io.Serializable
import java.util.UUID

data class InventoryRepositoryRemoveResult(
  val id: UUID,
  val steps: List<InventoryTaskStep>): Serializable
