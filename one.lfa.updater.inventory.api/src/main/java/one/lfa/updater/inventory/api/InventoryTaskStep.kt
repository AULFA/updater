package one.lfa.updater.inventory.api

import java.io.Serializable

data class InventoryTaskStep(
  val description: String,
  var resolution: String = "",
  var exception: Exception? = null,
  var failed : Boolean = false
): Serializable

