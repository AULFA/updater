package au.org.libraryforall.updater.main

import android.content.res.Resources
import one.lfa.updater.inventory.api.InventoryStringOPDSResourcesType
import org.joda.time.Duration
import java.io.File
import java.net.URI

class InventoryStringOPDSResources(
  val resources: Resources
) : InventoryStringOPDSResourcesType {

  override val opdsDatabaseEntryMissing: String
    get() = this.resources.getString(R.string.opdsDatabaseEntryMissing)

  override val opdsCatalogDeletingDatabaseEntry: String
    get() = this.resources.getString(R.string.opdsCatalogDeletingDatabaseEntry)

  override val opdsCatalogDeleting: String
    get() = this.resources.getString(R.string.opdsCatalogDeleting)

  override fun opdsCatalogDeletingFile(
    index: Int,
    size: Int
  ): String {
    return this.resources.getString(R.string.opdsCatalogDeletingFile, index, size)
  }

  override fun opdsCatalogDeletingFileFailed(
    index: Int,
    size: Int,
    localFile: File
  ): String {
    return this.resources.getString(R.string.opdsCatalogDeletingFileFailed, localFile)
  }

  override val opdsManifestSerializeFailed: String
    get() = this.resources.getString(R.string.opdsManifestSerializeFailed)

  override val opdsManifestSerializing: String
    get() = this.resources.getString(R.string.opdsManifestSerializing)

  override val opdsLocalFileDeletingFailed: String
    get() = this.resources.getString(R.string.opdsLocalFileDeletingFailed)

  override val opdsLocalFileDeleting: String
    get() = this.resources.getString(R.string.opdsLocalFileDeleting)

  override val opdsDirectoryCreatingFailed: String
    get() = this.resources.getString(R.string.opdsDirectoryCreatingFailed)

  override val opdsDirectoryCreating: String
    get() = this.resources.getString(R.string.opdsDirectoryCreating)

  override val opdsManifestParseFailed: String
    get() = this.resources.getString(R.string.opdsManifestParseFailed)

  override val opdsManifestParsing: String
    get() = this.resources.getString(R.string.opdsManifestParsing)

  override fun opdsManifestFetching(uri: URI): String {
    return this.resources.getString(R.string.opdsManifestFetching)
  }

  override fun opdsManifestFetched(duration: Duration): String {
    return this.resources.getString(R.string.opdsManifestFetched, duration.millis)
  }

  override fun opdsManifestFetchServerError(
    statusCode: Int,
    message: String,
    contentType: String,
    contentLength: Long?
  ): String {
    return this.resources.getString(
      R.string.install_connected_server_error,
      statusCode,
      message)
  }

  override fun opdsManifestConnectionFailed(exception: Exception): String {
    return this.resources.getString(
      R.string.install_connection_failed,
      exception.localizedMessage,
      exception.javaClass.simpleName)
  }

  override fun opdsManifestParsed(duration: Duration): String {
    return this.resources.getString(R.string.opdsManifestParsed, duration.millis)
  }
}
