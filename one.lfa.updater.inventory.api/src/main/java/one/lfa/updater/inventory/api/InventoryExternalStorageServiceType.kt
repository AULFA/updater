package one.lfa.updater.inventory.api

import java.io.File
import java.net.URI

/**
 * A service that interfaces with Android's "external storage" directories.
 */

interface InventoryExternalStorageServiceType {

  companion object {

    /**
     * The URI scheme used to denote external storage files.
     */

    val uriScheme: String = "lfaUpdaterExternal"
  }

  /**
   * Find all of the external storage directories.
   */

  fun allDirectories(): List<File>

  /**
   * Find the external storage directory, if any, that starts with `mount`.
   */

  fun findDirectoryFor(
    mount: File
  ): File?

  /**
   * Find the given file for an `lfaUpdaterExternal` URI.
   */

  fun findFile(
    uri: URI
  ): File?
}
