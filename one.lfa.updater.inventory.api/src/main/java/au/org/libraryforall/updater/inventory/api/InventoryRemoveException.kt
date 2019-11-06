package au.org.libraryforall.updater.inventory.api

import java.util.UUID

open class InventoryRemoveException(
  val id: UUID,
  message: String,
  cause: java.lang.Exception? = null) : Exception(message, cause)
