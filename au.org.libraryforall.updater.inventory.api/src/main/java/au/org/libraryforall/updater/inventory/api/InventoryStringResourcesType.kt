package au.org.libraryforall.updater.inventory.api

import au.org.libraryforall.updater.repository.api.Hash
import org.joda.time.Duration
import java.io.File
import java.net.URI
import java.util.UUID

interface InventoryStringResourcesType {

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

  fun installDownloadingCheckRequired(file: File): String

  fun installDownloadNeededHashFailed(
    expected: Hash,
    received: String
  ): String

  fun installDownloadNeededExceptional(e: Exception): String

  fun installDownloadReservationFailed(e: Exception): String

  fun installVerifying(
    currentBytes: Long,
    maximumBytes: Long
  ): String

  fun installVerifiedFile(file: File): String

  fun installVerificationFailed(
    expected: Hash,
    received: String
  ): String

  fun installVerificationFailedMissing(file: File): String

  fun installVerificationFailedException(e: Exception): String

  fun installAPKSucceeded(status: Int): String

  fun installAPKFailedWithException(e: Exception): String

  fun installAPKFailedWithCode(status: Int): String

  val installAPKStarted: String

  val installVerificationSucceeded: String

  val installDownloadNeeded: String

  val installDownloadNeededNot: String

  val installDownloadingSucceeded: String

  val installAlreadyInstalling: String

  val installStarted: String

  val inventoryRepositoryAddAlreadyExists: String

  fun inventoryRepositoryAddFetching(uri: URI): String

  fun inventoryRepositoryAddFetched(duration: Duration): String

  fun inventoryRepositoryAddServerError(
    statusCode: Int,
    message: String,
    contentType: String,
    contentLength: Long
  ): String

  fun inventoryRepositoryAddConnectionFailed(exception: Exception): String

  fun inventoryRepositoryAddParsed(duration: Duration): String

  fun inventoryRepositorySaving(id: UUID): String

  fun inventoryRepositorySavingSucceeded(id: UUID): String

  fun inventoryRepositorySavingFailed(id: UUID): String

  val inventoryRepositoryAddParseFailed: String

  val inventoryRepositoryAddParsing: String
}