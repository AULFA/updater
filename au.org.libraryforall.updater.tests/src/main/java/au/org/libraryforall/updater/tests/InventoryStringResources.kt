package au.org.libraryforall.updater.tests

import au.org.libraryforall.updater.inventory.api.InventoryStringResourcesType
import au.org.libraryforall.updater.repository.api.Hash
import java.io.File
import java.net.URI

class InventoryStringResources : InventoryStringResourcesType {
  override fun installVerifying(currentBytes: Long, maximumBytes: Long): String {
    return "installVerifying"
  }

  override fun installVerifiedFile(file: File): String {
    return "installVerifiedFile"
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

  override fun installAPKSucceeded(status: Int): String {
    return "installAPKSucceeded"
  }

  override fun installAPKFailedWithException(e: Exception): String {
    return "installAPKFailedWithException"
  }

  override fun installAPKFailedWithCode(status: Int): String {
    return "installAPKFailedWithCode"
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