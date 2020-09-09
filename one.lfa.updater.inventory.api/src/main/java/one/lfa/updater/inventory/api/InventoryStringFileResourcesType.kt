package one.lfa.updater.inventory.api

interface InventoryStringFileResourcesType {

  /**
   * Finding a local file.
   */

  val fileFinding: String

  /**
   * Opening a local file.
   */

  val fileOpening: String

  /**
   * Opening a local file failed.
   */

  fun fileOpeningFailed(e: Exception): String

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