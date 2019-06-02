package au.org.libraryforall.updater.app

import au.org.libraryforall.updater.inventory.api.InventoryTaskStep
import java.io.Serializable

data class InventoryFailure(
  val title: String,
  val attributes: Map<String, String>,
  val taskSteps: List<InventoryTaskStep>)
  : Serializable
