package one.lfa.updater.inventory.api

import java.net.URI

open class InventoryUpdateException(
  val uri: URI,
  message: String,
  cause: java.lang.Exception? = null
) : Exception(message, cause)
