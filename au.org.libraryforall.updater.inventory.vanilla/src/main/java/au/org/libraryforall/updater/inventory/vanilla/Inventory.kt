package au.org.libraryforall.updater.inventory.vanilla

import au.org.libraryforall.updater.apkinstaller.api.APKInstallerType
import au.org.libraryforall.updater.installed.api.InstalledPackageEvent
import au.org.libraryforall.updater.installed.api.InstalledPackagesType
import au.org.libraryforall.updater.inventory.api.InventoryAPKDirectoryType
import au.org.libraryforall.updater.inventory.api.InventoryAddException
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryAddResult
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryDatabaseEntryType
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryDatabaseEvent
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryDatabaseEvent.DatabaseRepositoryAdded
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryDatabaseEvent.DatabaseRepositoryRemoved
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryDatabaseEvent.DatabaseRepositoryUpdated
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryDatabaseType
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryType
import au.org.libraryforall.updater.inventory.api.InventoryStringResourcesType
import au.org.libraryforall.updater.inventory.api.InventoryType
import au.org.libraryforall.updater.inventory.vanilla.InventoryTaskMonad.InventoryTaskFailed
import au.org.libraryforall.updater.inventory.vanilla.InventoryTaskMonad.InventoryTaskSuccess
import au.org.libraryforall.updater.repository.api.Repository
import au.org.libraryforall.updater.repository.xml.api.RepositoryXMLParserProviderType
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import io.reactivex.disposables.Disposable
import one.irradia.http.api.HTTPAuthentication
import one.irradia.http.api.HTTPClientType
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.UUID
import java.util.concurrent.Callable

class Inventory private constructor(
  private val resources: InventoryStringResourcesType,
  private val executor: ListeningExecutorService,
  private val installedPackages: InstalledPackagesType,
  private val inventoryDatabase: InventoryRepositoryDatabaseType,
  private val apkDirectory: InventoryAPKDirectoryType,
  private val apkInstaller: APKInstallerType,
  private val http: HTTPClientType,
  private val httpAuthentication: (URI) -> HTTPAuthentication?,
  private val repositoryParsers: RepositoryXMLParserProviderType) : InventoryType {

  private val logger = LoggerFactory.getLogger(Inventory::class.java)

  companion object {

    /**
     * Open an inventory.
     */

    fun open(
      resources: InventoryStringResourcesType,
      executor: ListeningExecutorService,
      installedPackages: InstalledPackagesType,
      inventoryDatabase: InventoryRepositoryDatabaseType,
      apkDirectory: InventoryAPKDirectoryType,
      apkInstaller: APKInstallerType,
      repositoryParsers: RepositoryXMLParserProviderType,
      http: HTTPClientType,
      httpAuthentication: (URI) -> HTTPAuthentication?): InventoryType =
      Inventory(
        resources = resources,
        executor = executor,
        installedPackages = installedPackages,
        inventoryDatabase = inventoryDatabase,
        apkDirectory = apkDirectory,
        apkInstaller = apkInstaller,
        http = http,
        httpAuthentication = httpAuthentication,
        repositoryParsers = repositoryParsers)
  }

  private val installedSubscription: Disposable
  private val databaseSubscription: Disposable
  private val repositoryLock = Object()
  private val repositories = mutableMapOf<UUID, InventoryRepository>()

  init {
    this.installedSubscription =
      this.installedPackages.events.subscribe(this::onInstalledPackageEvent)
    this.databaseSubscription =
      this.inventoryDatabase.events.subscribe(this::onDatabaseEvent)

    this.logger.debug("initialized")
  }

  private fun onDatabaseEvent(event: InventoryRepositoryDatabaseEvent) {
    return when (event) {
      is DatabaseRepositoryAdded -> {

      }
      is DatabaseRepositoryRemoved -> {

      }
      is DatabaseRepositoryUpdated -> {

      }
    }
  }

  private fun onInstalledPackageEvent(event: InstalledPackageEvent) {
    return when (event) {
      InstalledPackageEvent.InstalledPackagesChanged -> {

      }
    }
  }

  override fun inventoryRepositorySelect(id: UUID): InventoryRepositoryType? =
    synchronized(this.repositoryLock) {
      this.repositories[id]
    }

  override fun inventoryRepositories(): List<UUID> =
    synchronized(this.repositoryLock) {
      this.repositories.keys.toList()
    }

  override fun inventoryRepositoryPut(repository: Repository): ListenableFuture<InventoryRepositoryAddResult> {
    return this.executor.submit(Callable<InventoryRepositoryAddResult> {
      val result =
        InventoryTaskRepositorySave(this.resources, this.inventoryDatabase, repository)
          .execute()
          .flatMap { entry -> InventoryTaskSuccess(this.putRepositoryForEntry(entry)) }

      InventoryRepositoryAddResult(
        uri = repository.source,
        steps = result.steps,
        repository = when (result) {
          is InventoryTaskSuccess -> result.value
          is InventoryTaskFailed -> null
        })
    })
  }

  override fun inventoryRepositoryAdd(uri: URI): ListenableFuture<InventoryRepositoryAddResult> {
    synchronized(this.repositoryLock) {
      val existing =
        this.repositories.values.find { repos -> repos.source == uri }

      if (existing != null) {
        return Futures.immediateFailedFuture(
          InventoryAddException(
            uri = uri,
            message = this.resources.inventoryRepositoryAddAlreadyExists))
      }
    }

    return this.executor.submit(Callable<InventoryRepositoryAddResult> {
      val result =
        InventoryTaskRepositoryAdd(
          resources = this.resources,
          http = this.http,
          httpAuthentication = this.httpAuthentication,
          repositoryParsers = this.repositoryParsers,
          database = this.inventoryDatabase,
          uri = uri
        ).execute()
          .flatMap { entry -> InventoryTaskSuccess(this.putRepositoryForEntry(entry)) }

      InventoryRepositoryAddResult(
        uri = uri,
        steps = result.steps,
        repository = when (result) {
          is InventoryTaskSuccess -> result.value
          is InventoryTaskFailed -> null
        })
    })
  }

  private fun putRepositoryForEntry(
    entry: InventoryRepositoryDatabaseEntryType): InventoryRepository {

    val inventoryRepository =
      InventoryRepository(
        resources = this.resources,
        executor = this.executor,
        installedPackages = this.installedPackages,
        apkDirectory = this.apkDirectory,
        apkInstaller = this.apkInstaller,
        http = this.http,
        httpAuthentication = this.httpAuthentication,
        repositoryParsers = this.repositoryParsers,
        databaseEntry = entry)

    synchronized(this.repositoryLock) {
      this.repositories[entry.repository.id] = inventoryRepository
    }
    return inventoryRepository
  }
}
