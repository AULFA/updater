package au.org.libraryforall.updater.inventory.vanilla

import au.org.libraryforall.updater.apkinstaller.api.APKInstallerType
import au.org.libraryforall.updater.installed.api.InstalledPackage
import au.org.libraryforall.updater.installed.api.InstalledPackagesType
import au.org.libraryforall.updater.inventory.api.InventoryEvent
import au.org.libraryforall.updater.inventory.api.InventoryEvent.InventoryRepositoryEvent.InventoryRepositoryPackageEvent.*
import au.org.libraryforall.updater.inventory.api.InventoryHashIndexedDirectoryType
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryPackageType
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryType
import au.org.libraryforall.updater.inventory.api.InventoryStringResourcesType
import au.org.libraryforall.updater.repository.api.Repository
import com.google.common.util.concurrent.ListeningExecutorService
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import one.irradia.http.api.HTTPAuthentication
import one.irradia.http.api.HTTPClientType
import org.joda.time.LocalDateTime
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.UUID

class InventoryRepository(
  private val resources: InventoryStringResourcesType,
  private val executor: ListeningExecutorService,
  private val installedPackages: InstalledPackagesType,
  private val directory: InventoryHashIndexedDirectoryType,
  private val apkInstaller: APKInstallerType,
  private val http: HTTPClientType,
  private val httpAuthentication: (URI) -> HTTPAuthentication?,
  override val id: UUID,
  titleInitial: String,
  sourceInitial: URI,
  updatedInitial: LocalDateTime) : InventoryRepositoryType {

  @Volatile
  private var titleValue: String = titleInitial
  @Volatile
  private var sourceValue: URI = sourceInitial
  @Volatile
  private var updatedValue: LocalDateTime = updatedInitial

  private val logger = LoggerFactory.getLogger(InventoryRepository::class.java)
  private val packagesLock = Object()
  private val packagesActual = mutableMapOf<String, InventoryRepositoryPackageType>()

  internal fun updateFrom(repository: Repository): InventoryRepository {
    this.titleValue = repository.title
    this.sourceValue = repository.source
    this.updatedValue = repository.updated

    val installed = this.installedPackages.packages()

    val events =
      synchronized(this.packagesLock) {
        mergeRepository(installed, this.packagesActual, repository)
      }

    for (event in events) {
      this.eventSubject.onNext(event)
    }
    return this
  }

  private fun mergeRepository(
    installed: Map<String, InstalledPackage>,
    viewCurrent: MutableMap<String, InventoryRepositoryPackageType>,
    repository: Repository): List<InventoryEvent> {

    val events =
      mutableListOf<InventoryEvent>()

    /*
     * Work out which packages are new.
     */

    for (repositoryPackage in repository.packagesNewest.values) {
      if (!viewCurrent.containsKey(repositoryPackage.name)) {
        val newPackage =
          InventoryRepositoryPackage(
            repositoryId = this.id,
            events = this.eventSubject,
            http = this.http,
            httpAuthentication = this.httpAuthentication,
            directory = this.directory,
            apkInstaller = this.apkInstaller,
            resources = this.resources,
            executor = this.executor,
            repositoryPackage = repositoryPackage)

        viewCurrent[newPackage.id] = newPackage
        this.logger.debug("[{}]: package {} now visible", repository.id, newPackage.id)
        events.add(PackageBecameVisible(
          repositoryId = this.id,
          packageId = repositoryPackage.id))
      }
    }

    /*
     * Work out which packages are no longer visible via this repository.
     */

    val iter = viewCurrent.iterator()
    while (iter.hasNext()) {
      val entry = iter.next()
      val existingPackage = entry.value

      if (!repository.packagesNewest.containsKey(existingPackage.id)) {
        this.logger.debug("[{}]: package {} now invisible", repository.id, existingPackage.id)

        iter.remove()
        events.add(PackageBecameInvisible(
          repositoryId = this.id,
          packageId = existingPackage.id))
      }
    }

    return events.toList()
  }

  override val packages: List<InventoryRepositoryPackageType>
    get() = synchronized(this.packagesLock) {
      this.packagesActual
        .values
        .toList()
        .sortedBy(InventoryRepositoryPackageType::name)
    }

  private val eventSubject: PublishSubject<InventoryEvent> =
    PublishSubject.create()

  override val events: Observable<InventoryEvent>
    get() = this.eventSubject

  override val title: String
    get() = this.titleValue

  override val updated: LocalDateTime
    get() = this.updatedValue

  override val source: URI
    get() = this.sourceValue
}