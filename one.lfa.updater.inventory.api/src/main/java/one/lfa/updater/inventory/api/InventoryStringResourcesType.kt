package one.lfa.updater.inventory.api

import one.lfa.updater.repository.api.Hash
import java.io.File
import java.net.URI

interface InventoryStringResourcesType
  : InventoryStringDownloadResourcesType,
  InventoryStringVerificationResourcesType,
  InventoryStringRepositoryResourcesType,
  InventoryStringFileResourcesType,
  InventoryStringOPDSResourcesType {

  fun installOpeningConnectionTo(uri: URI): String

  fun installConnectedOK(
    statusCode: Int,
    message: String,
    contentType: String,
    contentLength: Long
  ): String

  fun installConnectionServerError(
    statusCode: Int,
    message: String,
    contentType: String,
    contentLength: Long
  ): String

  fun installConnectionFailed(exception: Exception): String

  fun installDownloadingTo(file: File): String

  fun installDownloadingFailed(exception: Exception): String

  fun installRenamingTemporyFileTo(
    fileTemporary: File,
    fileOutput: File
  ): String

  fun installDownloading(
    receivedBytesTotal: Long,
    expectedBytesTotal: Long,
    bytesPerSecond: Long
  ): String

  fun installDownloadingIndefinite(
    receivedBytesTotal: Long,
    bytesPerSecond: Long
  ): String

  fun installDownloadingCheckRequired(file: File): String

  fun installDownloadNeededHashFailed(
    expected: Hash,
    received: String
  ): String

  fun installDownloadNeededExceptional(e: Exception): String

  fun installDownloadReservationFailed(e: Exception): String

  fun installVerificationFailed(
    expected: Hash,
    received: String
  ): String

  fun installVerificationFailedMissing(file: File): String

  fun installVerificationFailedException(e: Exception): String

  val uninstallAPKSucceeded: String

  val uninstallAPKCancelled: String

  val uninstallAPKStarted: String

  val installReservingFile: String

  val installVerifyingLocalFile: String

  val installVerificationCancelled: String

  val installAPKCancelled: String

  val installAPKSucceeded: String

  fun installAPKFailedWithException(e: Exception): String

  fun installAPKFailedWithCode(errorCode: Int): String

  fun uninstallAPKFailedWithCode(errorCode: Int): String

  val installWaitingForInstaller: String

  val installAPKStarted: String

  val installVerificationSucceeded: String

  val installDownloadNeeded: String

  val installDownloadNeededNot: String

  val installDownloadingSucceeded: String

  val installDownloadingCancelled: String

  val installAlreadyInstalling: String

  val installStarted: String

}