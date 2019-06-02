package au.org.libraryforall.updater.inventory.vanilla

import au.org.libraryforall.updater.inventory.api.InventoryRepositoryDatabaseEntryType
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryDatabaseType
import au.org.libraryforall.updater.inventory.api.InventoryStringResourcesType
import au.org.libraryforall.updater.repository.xml.api.RepositoryXMLParserProviderType
import one.irradia.http.api.HTTPAuthentication
import one.irradia.http.api.HTTPClientType
import java.net.URI

class InventoryTaskRepositoryAdd(
  private val resources: InventoryStringResourcesType,
  private val http: HTTPClientType,
  private val httpAuthentication: (URI) -> HTTPAuthentication?,
  private val repositoryParsers: RepositoryXMLParserProviderType,
  private val database: InventoryRepositoryDatabaseType,
  private val uri: URI) {

  fun execute(): InventoryTaskMonad<InventoryRepositoryDatabaseEntryType> {
    return InventoryTaskRepositoryFetch(
      resources = resources,
      http = http,
      httpAuthentication = httpAuthentication,
      repositoryParsers = repositoryParsers,
      uri = uri
    ).execute()
      .flatMap { repository ->
        InventoryTaskRepositorySave(this.resources, this.database, repository).execute()
      }
  }
}