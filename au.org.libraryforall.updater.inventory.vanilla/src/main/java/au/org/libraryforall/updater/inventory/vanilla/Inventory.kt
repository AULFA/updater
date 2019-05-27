package au.org.libraryforall.updater.inventory.vanilla

import au.org.libraryforall.updater.apkinstaller.api.APKInstallerType
import au.org.libraryforall.updater.installed.api.InstalledPackageEvent
import au.org.libraryforall.updater.installed.api.InstalledPackagesType
import au.org.libraryforall.updater.inventory.api.InventoryHashIndexedDirectoryType
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryType
import au.org.libraryforall.updater.inventory.api.InventoryStringResourcesType
import au.org.libraryforall.updater.inventory.api.InventoryType
import au.org.libraryforall.updater.repository.api.Repository
import com.google.common.base.Preconditions
import com.google.common.util.concurrent.ListeningExecutorService
import io.reactivex.disposables.Disposable
import one.irradia.http.api.HTTPAuthentication
import one.irradia.http.api.HTTPClientType
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.UUID

class Inventory private constructor(
  private val resources: InventoryStringResourcesType,
  private val executor: ListeningExecutorService,
  private val installedPackages: InstalledPackagesType,
  private val directory: InventoryHashIndexedDirectoryType,
  private val apkInstaller: APKInstallerType,
  private val http: HTTPClientType,
  private val httpAuthentication: (URI) -> HTTPAuthentication?) : InventoryType {

  private val logger = LoggerFactory.getLogger(Inventory::class.java)

  companion object {
    fun create(
      resources: InventoryStringResourcesType,
      executor: ListeningExecutorService,
      installedPackages: InstalledPackagesType,
      directory: InventoryHashIndexedDirectoryType,
      apkInstaller: APKInstallerType,
      http: HTTPClientType,
      httpAuthentication: (URI) -> HTTPAuthentication?): InventoryType =
      Inventory(
        resources = resources,
        executor = executor,
        http = http,
        httpAuthentication = httpAuthentication,
        directory = directory,
        apkInstaller = apkInstaller,
        installedPackages = installedPackages)
  }

  private var subscription: Disposable
  private val repositoryLock = Object()
  private val repositories = mutableMapOf<UUID, InventoryRepository>()

  init {
    this.subscription = this.installedPackages.events.subscribe(this::onInstalledPackageEvent)
    this.logger.debug("initialized")
  }

  private fun onInstalledPackageEvent(event: InstalledPackageEvent) {
    return when (event) {
      InstalledPackageEvent.InstalledPackagesChanged -> {

      }
    }
  }

  override fun inventoryRepositoryPut(repository: Repository): InventoryRepositoryType {
    this.logger.debug("put: {}", repository.id)

    val existing =
      synchronized(this.repositoryLock) {
        this.repositories[repository.id] ?: InventoryRepository(
          installedPackages = this.installedPackages,
          resources = this.resources,
          executor = this.executor,
          httpAuthentication = this.httpAuthentication,
          http = this.http,
          directory = this.directory,
          apkInstaller = this.apkInstaller,
          id = repository.id,
          titleInitial = repository.title,
          sourceInitial = repository.source,
          updatedInitial = repository.updated)
      }

    this.repositories[repository.id] = existing
    Preconditions.checkState(!this.repositories.isEmpty(), "Repository exists")
    return existing.updateFrom(repository)
  }

  override fun inventoryRepositorySelect(id: UUID): InventoryRepositoryType? =
    synchronized(this.repositoryLock) {
      this.repositories[id]
    }

  override fun inventoryRepositories(): List<UUID> =
    synchronized(this.repositoryLock) {
      this.repositories.keys.toList()
    }

}
