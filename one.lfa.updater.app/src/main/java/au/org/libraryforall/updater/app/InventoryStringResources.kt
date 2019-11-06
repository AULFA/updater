package au.org.libraryforall.updater.app

import android.content.Context
import one.lfa.updater.inventory.api.InventoryStringDownloadResourcesType
import one.lfa.updater.inventory.api.InventoryStringFileResourcesType
import one.lfa.updater.inventory.api.InventoryStringRepositoryResourcesType
import one.lfa.updater.inventory.api.InventoryStringResourcesType
import one.lfa.updater.inventory.api.InventoryStringVerificationResourcesType
import one.lfa.updater.repository.api.Hash
import java.io.File
import java.net.URI

class InventoryStringResources(
  private val context: Context,
  private val verificationStrings: InventoryStringVerificationResourcesType,
  private val downloadStrings: InventoryStringDownloadResourcesType,
  private val repositoryStrings: InventoryStringRepositoryResourcesType,
  private val fileStrings: InventoryStringFileResourcesType
) : InventoryStringResourcesType,
  InventoryStringDownloadResourcesType by downloadStrings,
  InventoryStringVerificationResourcesType by verificationStrings,
  InventoryStringRepositoryResourcesType by repositoryStrings,
  InventoryStringFileResourcesType by fileStrings {
  override val installReservingFile: String
    get() = this.context.getString(R.string.installReservingFile)

  override val installVerifyingLocalFile: String
    get() = this.context.getString(R.string.install_verifying)

  override fun installVerifyingLocalFileProgress(
    bytesCurrent: Long,
    bytesExpected: Long,
    bytesPerSecond: Long
  ): String {
    return this.context.getString(R.string.install_verifying_status,
      bytesCurrent.toDouble() / 1_000_000.0,
      bytesExpected.toDouble() / 1_000_000.0,
      bytesPerSecond.toDouble() / 1_000_000.0)
  }

  override val installVerificationCancelled: String
    get() = this.context.getString(R.string.install_verify_cancelled)

  override val installDownloadingCancelled: String
    get() = this.context.getString(R.string.install_downloading_cancelled)

  override val installAPKCancelled: String =
    this.context.getString(R.string.install_apk_cancelled)

  override fun installAPKFailedWithCode(errorCode: Int): String =
    this.context.getString(R.string.install_apk_failed_code, errorCode)

  override val installWaitingForInstaller: String =
    "Waiting for system installer."

  override val installAPKSucceeded: String =
    this.context.getString(R.string.install_apk_succeeded)

  override fun installAPKFailedWithException(e: Exception): String =
    this.context.getString(R.string.install_apk_failed_exception, e.message, e.javaClass.simpleName)

  override val installAPKStarted: String =
    this.context.getString(R.string.install_apk_started)

  override fun installVerificationFailed(expected: Hash, received: String): String =
    this.context.getString(R.string.install_verification_failed, expected.text, received)

  override fun installVerificationFailedMissing(file: File): String =
    this.context.getString(R.string.install_verification_failed_missing, file)

  override fun installVerificationFailedException(e: Exception): String =
    this.context.getString(R.string.install_verification_failed_exception, e.message, e.javaClass.simpleName)

  override val installVerificationSucceeded: String =
    this.context.getString(R.string.install_verification_succeeded)

  override fun installVerifying(
    currentBytes: Long,
    maximumBytes: Long
  ): String =
    this.context.getString(
      R.string.install_verifying,
      currentBytes.toDouble() / 1_000_000.0,
      maximumBytes.toDouble() / 1_000_000.0)

  override fun installDownloading(
    receivedBytesTotal: Long,
    expectedBytesTotal: Long,
    bytesPerSecond: Long
  ): String =
    this.context.getString(
      R.string.install_downloading,
      receivedBytesTotal.toDouble() / 1_000_000.0,
      expectedBytesTotal.toDouble() / 1_000_000.0,
      bytesPerSecond.toDouble() / 1_000_000.0)

  override fun installDownloadingIndefinite(
    receivedBytesTotal: Long,
    bytesPerSecond: Long
  ): String =
    this.context.getString(
      R.string.install_downloading_indefinite,
      receivedBytesTotal.toDouble() / 1_000_000.0,
      bytesPerSecond.toDouble() / 1_000_000.0)

  override fun installDownloadingCheckRequired(file: File): String =
    this.context.getString(R.string.install_download_check_required, file)

  override fun installDownloadNeededHashFailed(expected: Hash, received: String): String =
    this.context.getString(
      R.string.install_download_needed_hash_failed,
      expected.text,
      received)

  override fun installDownloadNeededExceptional(e: Exception): String =
    this.context.getString(
      R.string.install_download_needed_exception,
      e.message,
      e.javaClass.simpleName)

  override fun installDownloadReservationFailed(e: Exception): String =
    this.context.getString(
      R.string.install_download_reservation_failed,
      e.message,
      e.javaClass.simpleName)

  override val installDownloadNeeded: String =
    this.context.getString(R.string.install_download_needed)

  override val installDownloadNeededNot: String =
    this.context.getString(R.string.install_download_needed_not)

  override fun installRenamingTemporyFileTo(
    fileTemporary: File,
    fileOutput: File
  ): String =
    this.context.getString(
      R.string.install_renaming_to,
      fileTemporary,
      fileOutput)

  override val installDownloadingSucceeded: String =
    this.context.getString(R.string.install_downloading_succeeded)

  override fun installDownloadingTo(file: File): String =
    this.context.getString(R.string.install_downloading_to, file)

  override fun installDownloadingFailed(exception: Exception): String =
    this.context.getString(
      R.string.install_downloading_failed,
      exception.message,
      exception.javaClass.simpleName)

  override fun installOpeningConnectionTo(uri: URI): String =
    this.context.getString(R.string.install_opening_connection_to, uri.toString())

  override fun installConnectedOK(
    statusCode: Int,
    message: String,
    contentType: String,
    contentLength: Long
  ): String =
    this.context.getString(
      R.string.install_connected_ok,
      statusCode,
      message,
      contentLength,
      contentType)

  override fun installConnectionServerError(
    statusCode: Int,
    message: String,
    contentType: String,
    contentLength: Long
  ): String =
    this.context.getString(
      R.string.install_connected_server_error,
      statusCode,
      message)

  override fun installConnectionFailed(exception: Exception): String =
    this.context.getString(
      R.string.install_connection_failed,
      exception.localizedMessage,
      exception.javaClass.simpleName)

  override val installAlreadyInstalling: String =
    this.context.getString(R.string.install_already_running)

  override val installStarted: String =
    this.context.getString(R.string.install_started)

}