package au.org.libraryforall.updater.app

import android.content.Context
import au.org.libraryforall.updater.inventory.api.InventoryStringResourcesType
import au.org.libraryforall.updater.repository.api.Hash
import org.joda.time.Duration
import java.io.File
import java.net.URI
import java.util.UUID

class InventoryStringResources(private val context: Context) : InventoryStringResourcesType {

  override val inventoryRepositoryRequiredUUIDChecking: String
    get() = this.context.getString(R.string.inventory_required_uuid_checking)

  override fun inventoryRepositoryRequiredUUIDCheckingFailed(requiredUUID: UUID, receivedUUID: UUID): String {
    return this.context.getString(R.string.inventory_required_uuid_checking_failed, requiredUUID, receivedUUID)
  }

  override fun inventoryRepositoryRequiredUUIDCheckingOK(requiredUUID: UUID?, receivedUUID: UUID): String {
    return this.context.getString(R.string.inventory_required_uuid_checking_succeeded, requiredUUID ?: "(none)", receivedUUID)
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

  override val inventoryRepositoryRemovingFailed: String =
    this.context.resources.getString(R.string.repository_removing_failed)

  override val inventoryRepositoryRemovingSucceeded: String =
    this.context.resources.getString(R.string.repository_removing_succeeded)

  override val inventoryRepositoryRemoveNonexistent: String =
    this.context.resources.getString(R.string.repository_removing_nonexistent)

  override val inventoryRepositoryRemoving: String =
    this.context.resources.getString(R.string.repository_removing)

  override val inventoryRepositoryAddInProgress: String =
    this.context.resources.getString(R.string.repository_add_in_progress)

  override val inventoryRepositoryAddParseFailed: String =
    this.context.getString(R.string.inventory_repository_add_parse_failed)

  override val inventoryRepositoryAddAlreadyExists: String =
    this.context.getString(R.string.inventory_repository_add_already_exists)

  override fun inventoryRepositoryAddFetching(uri: URI): String =
    this.context.resources.getString(R.string.inventory_repository_add_fetching, uri.toString())

  override fun inventoryRepositoryAddFetched(duration: Duration): String =
    this.context.resources.getString(R.string.inventory_repository_add_fetched, duration.millis)

  override fun inventoryRepositoryAddServerError(
    statusCode: Int,
    message: String,
    contentType: String,
    contentLength: Long
  ): String =
    this.context.getString(
      R.string.install_connected_server_error,
      statusCode,
      message)

  override fun inventoryRepositoryAddConnectionFailed(exception: Exception): String =
    this.context.getString(
      R.string.install_connection_failed,
      exception.localizedMessage,
      exception.javaClass.simpleName)

  override fun inventoryRepositoryAddParsed(duration: Duration): String =
    this.context.resources.getString(R.string.inventory_repository_add_parsed, duration.millis)

  override fun inventoryRepositorySaving(id: UUID): String =
    this.context.getString(R.string.inventory_repository_saving, id.toString())

  override fun inventoryRepositorySavingSucceeded(id: UUID): String =
    this.context.getString(R.string.inventory_repository_save_success, id.toString())

  override fun inventoryRepositorySavingFailed(id: UUID): String =
    this.context.getString(R.string.inventory_repository_save_failure, id.toString())

  override val inventoryRepositoryAddParsing: String =
    this.context.getString(R.string.inventory_repository_add_parsing)

  override val installAPKSucceeded: String =
    this.context.getString(R.string.install_apk_succeeded)

  override fun installAPKFailedWithException(e: Exception): String =
    this.context.getString(R.string.install_apk_failed_exception, e.message, e.javaClass.simpleName)

  override val installAPKStarted: String =
    this.context.getString(R.string.install_apk_started)

  override fun installVerifiedFile(file: File): String =
    this.context.getString(R.string.install_verified_file, file)

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