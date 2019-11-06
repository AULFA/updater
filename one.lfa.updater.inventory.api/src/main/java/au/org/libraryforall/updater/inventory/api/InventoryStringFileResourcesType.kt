package au.org.libraryforall.updater.inventory.api

interface InventoryStringFileResourcesType {

  /**
   * Deleting a local file.
   */

  val fileDeleting: String

  /**
   * The local file does not exist.
   */

  val fileDoesNotExist: String

  /**
   * The local file could not be deleted.
   */

  val fileCouldNotDelete: String

}