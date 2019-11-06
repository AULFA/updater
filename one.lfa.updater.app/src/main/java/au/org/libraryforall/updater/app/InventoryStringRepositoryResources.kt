package au.org.libraryforall.updater.app

import android.content.res.Resources
import one.lfa.updater.inventory.api.InventoryStringRepositoryResourcesType
import org.joda.time.Duration
import java.net.URI
import java.util.UUID

class InventoryStringRepositoryResources(
  val resources: Resources
) : InventoryStringRepositoryResourcesType {

  override val repositoryRequiredUUIDChecking: String
    get() = this.resources.getString(R.string.inventory_required_uuid_checking)

  override fun repositoryRequiredUUIDCheckingFailed(
    requiredUUID: UUID,
    receivedUUID: UUID
  ): String {
    return this.resources.getString(R.string.inventory_required_uuid_checking_failed, requiredUUID, receivedUUID)
  }

  override fun repositoryRequiredUUIDCheckingOK(
    requiredUUID: UUID?,
    receivedUUID: UUID
  ): String {
    return this.resources.getString(R.string.inventory_required_uuid_checking_succeeded, requiredUUID
      ?: "(none)", receivedUUID)
  }

  override val repositoryRemovingFailed: String =
    this.resources.getString(R.string.repository_removing_failed)

  override val repositoryRemovingSucceeded: String =
    this.resources.getString(R.string.repository_removing_succeeded)

  override val repositoryRemoveNonexistent: String =
    this.resources.getString(R.string.repository_removing_nonexistent)

  override val repositoryRemoving: String =
    this.resources.getString(R.string.repository_removing)

  override val repositoryAddInProgress: String =
    this.resources.getString(R.string.repository_add_in_progress)

  override val repositoryAddParseFailed: String =
    this.resources.getString(R.string.inventory_repository_add_parse_failed)

  override val repositoryAddAlreadyExists: String =
    this.resources.getString(R.string.inventory_repository_add_already_exists)

  override fun repositoryAddFetching(uri: URI): String =
    this.resources.getString(R.string.inventory_repository_add_fetching, uri.toString())

  override fun repositoryAddFetched(duration: Duration): String =
    this.resources.getString(R.string.inventory_repository_add_fetched, duration.millis)

  override fun repositoryAddServerError(
    statusCode: Int,
    message: String,
    contentType: String,
    contentLength: Long?
  ): String =
    this.resources.getString(
      R.string.install_connected_server_error,
      statusCode,
      message)

  override fun repositoryAddConnectionFailed(exception: Exception): String =
    this.resources.getString(
      R.string.install_connection_failed,
      exception.localizedMessage,
      exception.javaClass.simpleName)

  override fun repositoryAddParsed(duration: Duration): String =
    this.resources.getString(R.string.inventory_repository_add_parsed, duration.millis)

  override fun repositorySaving(id: UUID): String =
    this.resources.getString(R.string.inventory_repository_saving, id.toString())

  override fun repositorySavingSucceeded(id: UUID): String =
    this.resources.getString(R.string.inventory_repository_save_success, id.toString())

  override fun repositorySavingFailed(id: UUID): String =
    this.resources.getString(R.string.inventory_repository_save_failure, id.toString())

  override val repositoryAddParsing: String =
    this.resources.getString(R.string.inventory_repository_add_parsing)

}