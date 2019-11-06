package au.org.libraryforall.updater.inventory.api

open class InventoryException(
  message: String,
  cause: java.lang.Exception?) : Exception(message, cause) {

  constructor(cause: java.lang.Exception): this(cause.message ?: "", cause)

}