package au.org.libraryforall.updater.tests

import au.org.libraryforall.updater.inventory.api.InventoryProgressValue
import au.org.libraryforall.updater.inventory.api.InventoryStringResourcesType
import au.org.libraryforall.updater.repository.api.Hash
import org.joda.time.Duration
import java.io.File
import java.net.URI
import java.util.UUID

class InventoryStringResources : InventoryStringResourcesType {

  override val installReservingFile: String
    get() = "installReservingFile"

  override val installVerifyingLocalFile: String
    get() = "installVerifyingLocalFile"

  override fun installVerifyingLocalFileProgress(
    bytesCurrent: Long,
    bytesExpected: Long,
    bytesPerSecond: Long
  ): String {
    return "installVerifyingLocalFileProgress $bytesCurrent $bytesExpected $bytesPerSecond"
  }


  override val downloadingHTTPSkipping: String
    get() = "downloadingHTTPSkipping"

  override val downloadingHTTPWaitingBeforeRetrying: String
    get() = "downloadingHTTPWaitingBeforeRetrying"

  override val downloadingHTTPSucceeded: String
    get() = "downloadingHTTPSucceeded"

  override val downloadingHTTPWritingFile: String
    get() = "downloadingHTTPWritingFile"

  override val downloadingHTTPOpeningFile: String
    get() = "downloadingHTTPOpeningFile"

  override fun downloadingHTTPOK(duration: Duration): String {
    return "downloadingHTTPOK $duration"
  }

  override fun downloadingHTTPServerError(
    statusCode: Int,
    message: String,
    contentType: String,
    contentLength: Long?
  ): String {
    return "downloadingHTTPServerError $statusCode $message $contentType $contentLength"
  }

  override fun downloadingHTTPConnectionFailed(exception: Exception): String {
    return "downloadingHTTPConnectionFailed"
  }

  override fun downloadingHTTPRequest(
    uri: URI,
    attemptCurrent: Int,
    attemptMax: Int
  ): String {
    return "downloadingHTTPRequest $uri $attemptCurrent $attemptMax"
  }

  override fun downloadingHTTPOpeningFileFailed(exception: Exception): String {
    return "downloadingHTTPOpeningFileFailed"
  }

  override fun downloadingHTTPProgress(
    majorProgress: InventoryProgressValue?,
    minorProgress: InventoryProgressValue
  ): String {
    return "downloadingHTTPProgress"
  }

  override fun downloadingHTTPRetryingInSeconds(time: Long): String {
    return "downloadingHTTPRetryingInSeconds $time"
  }

  override fun verificationFailed(
    expectedHash: Hash,
    receivedHash: String
  ): String {
    return "verificationFailed ${expectedHash.text} $receivedHash"
  }

  override val verifyCheckSuccess: String
    get() = "verifyCheckSuccess"
  override val verificationSucceeded: String
    get() = "verificationSucceeded"
  override val verificationCancelled: String
    get() = "verificationCancelled"
  override val verifyingLocalFile: String
    get() = "verifyingLocalFile"

  override val repositoryRequiredUUIDChecking: String
    get() = "repositoryRequiredUUIDChecking"

  override fun repositoryRequiredUUIDCheckingFailed(requiredUUID: UUID, receivedUUID: UUID): String {
    return "repositoryRequiredUUIDCheckingFailed"
  }

  override fun repositoryRequiredUUIDCheckingOK(requiredUUID: UUID?, receivedUUID: UUID): String {
    return "repositoryRequiredUUIDCheckingOK"
  }

  override val installVerificationCancelled: String
    get() = "installVerificationCancelled"

  override val installDownloadingCancelled: String
    get() = "installDownloadingCancelled"

  override val installAPKCancelled: String =
    "installAPKCancelled"

  override fun installAPKFailedWithCode(errorCode: Int): String {
    return "installAPKFailedWithCode"
  }

  override fun installDownloading(
    receivedBytesTotal: Long,
    expectedBytesTotal: Long,
    bytesPerSecond: Long
  ): String {
    return "installDownloading"
  }

  override fun installDownloadingIndefinite(
    receivedBytesTotal: Long,
    bytesPerSecond: Long
  ): String {
    return "installDownloadingIndefinite"
  }

  override val installAPKSucceeded: String =
    "installAPKSucceeded"

  override val installWaitingForInstaller: String =
    "installWaitingForInstaller"

  override val repositoryRemovingFailed: String =
    "repositoryRemovingFailed"

  override val repositoryRemovingSucceeded: String =
    "repositoryRemovingSucceeded"

  override val repositoryRemoveNonexistent: String =
    "repositoryRemoveNonexistent"

  override val repositoryRemoving: String =
    "repositoryRemoving"

  override val repositoryAddInProgress: String =
    "repositoryAddInProgress"

  override val repositoryAddParseFailed: String =
    "repositoryAddParseFailed"

  override val repositoryAddAlreadyExists: String =
    "repositoryAddAlreadyExists"

  override fun repositoryAddFetching(uri: URI): String {
    return "repositoryAddFetching"
  }

  override fun repositoryAddFetched(duration: Duration): String {
    return "repositoryAddFetched"
  }

  override fun repositoryAddServerError(
    statusCode: Int,
    message: String,
    contentType: String,
    contentLength: Long?
  ): String {
    return "repositoryAddServerError"
  }

  override fun repositoryAddConnectionFailed(exception: Exception): String {
    return "repositoryAddConnectionFailed"
  }

  override fun repositoryAddParsed(duration: Duration): String {
    return "repositoryAddParsed"
  }

  override fun repositorySaving(id: UUID): String {
    return "repositorySaving"
  }

  override fun repositorySavingSucceeded(id: UUID): String {
    return "repositorySavingSucceeded"
  }

  override fun repositorySavingFailed(id: UUID): String {
    return "repositorySavingFailed"
  }

  override val repositoryAddParsing: String =
    "repositoryAddParsing"


  override fun installVerifying(currentBytes: Long, maximumBytes: Long): String {
    return "installVerifying"
  }

  override fun installVerificationFailed(expected: Hash, received: String): String {
    return "installVerificationFailed"
  }

  override fun installVerificationFailedMissing(file: File): String {
    return "installVerificationFailedMissing"
  }

  override fun installVerificationFailedException(e: Exception): String {
    return "installVerificationFailedException"
  }

  override fun installAPKFailedWithException(e: Exception): String {
    return "installAPKFailedWithException"
  }

  override val installAPKStarted: String =
    "installAPKStarted"
  override val installVerificationSucceeded: String =
    "installVerificationSucceeded"

  override fun installDownloadingCheckRequired(file: File): String {
    return "installDownloadingCheckRequired"
  }

  override fun installDownloadNeededHashFailed(expected: Hash, received: String): String {
    return "installDownloadNeededHashFailed"
  }

  override fun installDownloadNeededExceptional(e: Exception): String {
    return "installDownloadNeededExceptional"
  }

  override fun installDownloadReservationFailed(e: Exception): String {
    return "installDownloadReservationFailed"
  }

  override val installDownloadNeeded: String =
    "installDownloadNeeded"

  override val installDownloadNeededNot: String =
    "installDownloadNeededNot"

  override fun installOpeningConnectionTo(uri: URI): String {
    return "installOpeningConnectionTo"
  }

  override fun installConnectedOK(
    statusCode: Int,
    message: String,
    contentType: String,
    contentLength: Long
  ): String {
    return "installConnectedOK"
  }

  override fun installConnectionServerError(
    statusCode: Int,
    message: String,
    contentType: String,
    contentLength: Long
  ): String {
    return "installConnectionServerError"
  }

  override fun installConnectionFailed(exception: Exception): String {
    return "installConnectionFailed"
  }

  override fun installDownloadingTo(file: File): String {
    return "installDownloadingTo"
  }

  override fun installDownloadingFailed(exception: Exception): String {
    return "installDownloadingFailed"
  }

  override fun installRenamingTemporyFileTo(
    fileTemporary: File,
    fileOutput: File
  ): String {
    return "installRenamingTemporyFileTo"
  }

  override val installDownloadingSucceeded: String = "installDownloadingSucceeded"
  override val installAlreadyInstalling = "installAlreadyInstalling"
  override val installStarted = "installStarted"
}