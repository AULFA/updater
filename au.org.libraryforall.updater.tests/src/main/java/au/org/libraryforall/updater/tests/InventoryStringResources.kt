package au.org.libraryforall.updater.tests

import au.org.libraryforall.updater.inventory.api.InventoryStringResourcesType
import au.org.libraryforall.updater.repository.api.Hash
import org.joda.time.Duration
import java.io.File
import java.net.URI
import java.util.UUID

class InventoryStringResources : InventoryStringResourcesType {
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

  override val inventoryRepositoryRemovingFailed: String =
    "inventoryRepositoryRemovingFailed"

  override val inventoryRepositoryRemovingSucceeded: String =
    "inventoryRepositoryRemovingSucceeded"

  override val inventoryRepositoryRemoveNonexistent: String =
    "inventoryRepositoryRemoveNonexistent"

  override val inventoryRepositoryRemoving: String =
    "inventoryRepositoryRemoving"

  override val inventoryRepositoryAddInProgress: String =
    "inventoryRepositoryAddInProgress"

  override val inventoryRepositoryAddParseFailed: String =
    "inventoryRepositoryAddParseFailed"

  override val inventoryRepositoryAddAlreadyExists: String =
    "inventoryRepositoryAddAlreadyExists"

  override fun inventoryRepositoryAddFetching(uri: URI): String {
    return "inventoryRepositoryAddFetching"
  }

  override fun inventoryRepositoryAddFetched(duration: Duration): String {
    return "inventoryRepositoryAddFetched"
  }

  override fun inventoryRepositoryAddServerError(
    statusCode: Int,
    message: String,
    contentType: String,
    contentLength: Long
  ): String {
    return "inventoryRepositoryAddServerError"
  }

  override fun inventoryRepositoryAddConnectionFailed(exception: Exception): String {
    return "inventoryRepositoryAddConnectionFailed"
  }

  override fun inventoryRepositoryAddParsed(duration: Duration): String {
    return "inventoryRepositoryAddParsed"
  }

  override fun inventoryRepositorySaving(id: UUID): String {
    return "inventoryRepositorySaving"
  }

  override fun inventoryRepositorySavingSucceeded(id: UUID): String {
    return "inventoryRepositorySavingSucceeded"
  }

  override fun inventoryRepositorySavingFailed(id: UUID): String {
    return "inventoryRepositorySavingFailed"
  }

  override val inventoryRepositoryAddParsing: String =
    "inventoryRepositoryAddParsing"


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