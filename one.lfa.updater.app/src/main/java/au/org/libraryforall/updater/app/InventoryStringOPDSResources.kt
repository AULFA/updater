package au.org.libraryforall.updater.app

import android.content.res.Resources
import one.lfa.updater.inventory.api.InventoryStringOPDSResourcesType
import org.joda.time.Duration
import java.net.URI

class InventoryStringOPDSResources(
  val resources: Resources
) : InventoryStringOPDSResourcesType {

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
