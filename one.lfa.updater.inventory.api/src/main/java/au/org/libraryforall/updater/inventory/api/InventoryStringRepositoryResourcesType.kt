package au.org.libraryforall.updater.inventory.api

import org.joda.time.Duration
import java.net.URI
import java.util.UUID

interface InventoryStringRepositoryResourcesType {

  val repositoryAddAlreadyExists: String

  fun repositoryAddFetching(uri: URI): String

  fun repositoryAddFetched(duration: Duration): String

  val repositoryRequiredUUIDChecking: String

  fun repositoryAddServerError(
    statusCode: Int,
    message: String,
    contentType: String,
    contentLength: Long?
  ): String

  val repositoryAddInProgress: String

  fun repositoryAddConnectionFailed(exception: Exception): String

  fun repositoryAddParsed(duration: Duration): String

  fun repositorySaving(id: UUID): String

  fun repositorySavingSucceeded(id: UUID): String

  fun repositorySavingFailed(id: UUID): String

  fun repositoryRequiredUUIDCheckingFailed(
    requiredUUID: UUID,
    receivedUUID: UUID
  ): String

  fun repositoryRequiredUUIDCheckingOK(
    requiredUUID: UUID?,
    receivedUUID: UUID
  ): String

  val repositoryAddParseFailed: String

  val repositoryAddParsing: String

  val repositoryRemovingFailed: String

  val repositoryRemovingSucceeded: String

  val repositoryRemoveNonexistent: String

  val repositoryRemoving: String

}
