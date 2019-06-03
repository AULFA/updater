package au.org.libraryforall.updater.inventory.api

import java.io.Serializable

data class InventoryFailureReport(
  val title: String,
  val attributes: Map<String, String>,
  val taskSteps: List<InventoryTaskStep>)
  : Serializable
