package one.lfa.updater.inventory.api

import org.joda.time.Duration
import java.net.URI

interface InventoryStringOPDSResourcesType {

  val opdsManifestSerializeFailed: String

  val opdsManifestSerializing: String

  val opdsLocalFileDeletingFailed: String

  val opdsLocalFileDeleting: String

  val opdsDirectoryCreatingFailed: String

  val opdsDirectoryCreating: String

  val opdsManifestParseFailed: String

  val opdsManifestParsing: String

  fun opdsManifestFetching(uri: URI): String

  fun opdsManifestFetched(duration: Duration): String

  fun opdsManifestFetchServerError(
    statusCode: Int,
    message: String,
    contentType: String,
    contentLength: Long?
  ): String

  fun opdsManifestConnectionFailed(exception: Exception): String

  fun opdsManifestParsed(duration: Duration): String

}