package au.org.libraryforall.updater.app

import android.content.res.Resources
import au.org.libraryforall.updater.inventory.api.InventoryProgressValue
import au.org.libraryforall.updater.inventory.api.InventoryStringDownloadResourcesType
import org.joda.time.Duration
import java.net.URI

class InventoryStringDownloadResources(
  val resources: Resources
) : InventoryStringDownloadResourcesType {

  override val downloadingHTTPSkipping: String
    get() = this.resources.getString(R.string.downloadingHTTPSkipping)

  override val downloadingHTTPWaitingBeforeRetrying: String
    get() = this.resources.getString(R.string.downloadingHTTPWaitingBeforeRetrying)

  override fun downloadingHTTPRetryingInSeconds(
    time: Long,
    attemptCurrent: Int,
    attemptMax: Int
  ): String {
    return this.resources.getString(
      R.string.downloadingHTTPRetryingInSeconds,
      attemptCurrent,
      attemptMax,
      time
    )
  }

  override val downloadingHTTPSucceeded: String
    get() = this.resources.getString(R.string.downloadingHTTPSucceeded)

  override val downloadingHTTPWritingFile: String
    get() = this.resources.getString(R.string.downloadingHTTPWritingFile)

  override val downloadingHTTPOpeningFile: String
    get() = this.resources.getString(R.string.downloadingHTTPOpeningFile)

  override fun downloadingHTTPOK(duration: Duration): String {
    return this.resources.getString(R.string.downloadingHTTPOK, duration.toString())
  }

  override fun downloadingHTTPServerError(
    statusCode: Int,
    message: String,
    contentType: String,
    contentLength: Long?
  ): String {
    return this.resources.getString(
      R.string.downloadingHTTPServerError,
      statusCode,
      message,
      contentType,
      contentLength ?: -1L
    )
  }

  override fun downloadingHTTPConnectionFailed(exception: Exception): String {
    return this.resources.getString(
      R.string.downloadingHTTPConnectionFailed,
      exception.localizedMessage
    )
  }

  override fun downloadingHTTPRequest(
    uri: URI,
    attemptCurrent: Int,
    attemptMax: Int
  ): String {
    return this.resources.getString(
      R.string.downloadingHTTPRequest,
      uri.toString(),
      attemptCurrent,
      attemptMax
    )
  }

  override fun downloadingHTTPOpeningFileFailed(exception: Exception): String {
    return this.resources.getString(
      R.string.downloadingHTTPOpeningFileFailed,
      exception.localizedMessage
    )
  }

  override fun downloadingHTTPProgress(
    majorProgress: InventoryProgressValue?,
    minorProgress: InventoryProgressValue
  ): String {
    return when (majorProgress) {
      null,
      is InventoryProgressValue.InventoryProgressValueIndefinite ->
        this.minorProgressOnly(minorProgress)

      is InventoryProgressValue.InventoryProgressValueDefinite -> {
        when (minorProgress) {
          is InventoryProgressValue.InventoryProgressValueIndefinite -> {
            this.resources.getString(R.string.downloadingHTTPProgressMajorIndefiniteMinor,
              majorProgress.current,
              majorProgress.maximum,
              minorProgress.current.toDouble() / 1_000_000.0,
              minorProgress.perSecond.toDouble() / 1_000_000.0)
          }
          is InventoryProgressValue.InventoryProgressValueDefinite -> {
            this.resources.getString(R.string.downloadingHTTPProgressMajor,
              majorProgress.current,
              majorProgress.maximum,
              minorProgress.current.toDouble() / 1_000_000.0,
              minorProgress.maximum.toDouble() / 1_000_000.0,
              minorProgress.perSecond.toDouble() / 1_000_000.0)
          }
        }
      }
    }
  }

  private fun minorProgressOnly(minorProgress: InventoryProgressValue): String {
    return when (minorProgress) {
      is InventoryProgressValue.InventoryProgressValueIndefinite -> {
        this.resources.getString(
          R.string.downloadingHTTPProgressMinorIndefinite,
          minorProgress.current.toDouble() / 1_000_000.0,
          minorProgress.perSecond.toDouble() / 1_000_000.0)
      }
      is InventoryProgressValue.InventoryProgressValueDefinite -> {
        this.resources.getString(
          R.string.downloadingHTTPProgressMinorDefinite,
          minorProgress.current.toDouble() / 1_000_000.0,
          minorProgress.maximum.toDouble() / 1_000_000.0,
          minorProgress.perSecond.toDouble() / 1_000_000.0)
      }
    }
  }
}