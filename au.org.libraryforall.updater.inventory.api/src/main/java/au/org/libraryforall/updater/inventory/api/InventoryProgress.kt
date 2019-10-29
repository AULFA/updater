package au.org.libraryforall.updater.inventory.api

/**
 * Information about the progress of an inventory operation.
 */

data class InventoryProgress(

  /**
   * The major progress.
   */

  val major: InventoryProgressValue?,

  /**
   * The minor progress.
   */

  val minor: InventoryProgressValue,

  /**
   * The status expressed as a humanly-readable string.
   */

  val status: String)
