package one.lfa.updater.inventory.api

import org.joda.time.Duration
import java.net.URI

interface InventoryStringDownloadResourcesType {

  val downloadingHTTPSkipping: String

  val downloadingHTTPWaitingBeforeRetrying: String

  val downloadingHTTPSucceeded: String

  val downloadingHTTPWritingFile: String

  val downloadingHTTPOpeningFile: String

  fun downloadingHTTPOK(
    duration: Duration
  ): String

  fun downloadingHTTPServerError(
    statusCode: Int,
    message: String,
    contentType: String,
    contentLength: Long?
  ): String

  fun downloadingHTTPConnectionFailed(
    exception: Exception
  ): String

  fun downloadingHTTPRequest(
    uri: URI,
    attemptCurrent: Int,
    attemptMax: Int
  ): String

  fun downloadingHTTPOpeningFileFailed(
    exception: Exception
  ): String

  fun downloadingHTTPProgress(
    majorProgress: InventoryProgressValue?,
    minorProgress: InventoryProgressValue
  ): String

  fun downloadingVerifyingProgress(
    majorProgress: InventoryProgressValue?,
    minorProgress: InventoryProgressValue
  ): String

  fun downloadingHTTPRetryingInSeconds(
    time: Long,
    attemptCurrent: Int,
    attemptMax: Int
  ): String
}