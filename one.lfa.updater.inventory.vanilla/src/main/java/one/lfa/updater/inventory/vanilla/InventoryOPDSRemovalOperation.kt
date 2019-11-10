package one.lfa.updater.inventory.vanilla

import java.io.File

/**
 * A description of an operation to be performed to delete an OPDS catalog.
 */

sealed class InventoryOPDSRemovalOperation {

  /**
   * A local file needs to be deleted.
   */

  data class DeleteLocalFile(
    val localFile: File
  ) : InventoryOPDSRemovalOperation()

  /**
   * A local directory needs to be deleted.
   */

  data class DeleteLocalDirectory(
    val localFile: File
  ) : InventoryOPDSRemovalOperation()
}