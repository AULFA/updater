package one.lfa.updater.inventory.vanilla

import one.lfa.updater.opds.api.OPDSManifest
import java.io.File
import java.net.URI

/**
 * A description of an operation to be performed to fetch an OPDS catalog.
 */

sealed class InventoryOPDSOperation {

  /**
   * A local directory needs to be created.
   */

  data class CreateDirectory(
    val directory: File
  ) : InventoryOPDSOperation()

  /**
   * A file needs to be downloaded. The download can be skipped if there's already an existing
   * file with the correct hash.
   */

  data class DownloadFile(
    val uri: URI,
    val hashAlgorithm: String,
    val hash: String,
    val outputFile: File
  ) : InventoryOPDSOperation()

  /**
   * An OPDS manifest needs to be serialized.
   */

  data class SerializeManifest(
    val manifest: OPDSManifest
  ) : InventoryOPDSOperation()

  /**
   * A local file needs to be deleted.
   */

  data class DeleteLocalFile(
    val localFile: File
  ) : InventoryOPDSOperation()
}