package au.org.libraryforall.updater.inventory.vanilla

import au.org.libraryforall.updater.apkinstaller.api.APKInstallerType
import au.org.libraryforall.updater.installed.api.InstalledPackage
import au.org.libraryforall.updater.installed.api.InstalledPackagesType
import au.org.libraryforall.updater.inventory.api.InventoryAPKDirectoryType
import au.org.libraryforall.updater.inventory.api.InventoryEvent
import au.org.libraryforall.updater.inventory.api.InventoryEvent.InventoryRepositoryEvent.InventoryRepositoryPackageEvent.PackageBecameInvisible
import au.org.libraryforall.updater.inventory.api.InventoryEvent.InventoryRepositoryEvent.InventoryRepositoryPackageEvent.PackageBecameVisible
import au.org.libraryforall.updater.inventory.api.InventoryEvent.InventoryRepositoryEvent.RepositoryChanged
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryDatabaseEntryType
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryDatabaseEvent
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryDatabaseEvent.DatabaseRepositoryAdded
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryDatabaseEvent.DatabaseRepositoryRemoved
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryDatabaseEvent.DatabaseRepositoryUpdated
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryPackageType
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryState
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryState.RepositoryIdle
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryState.RepositoryUpdateFailed
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryState.RepositoryUpdating
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryType
import au.org.libraryforall.updater.inventory.api.InventoryStringResourcesType
import au.org.libraryforall.updater.inventory.vanilla.InventoryTaskMonad.InventoryTaskFailed
import au.org.libraryforall.updater.inventory.vanilla.InventoryTaskMonad.InventoryTaskSuccess
import au.org.libraryforall.updater.repository.api.Repository
import au.org.libraryforall.updater.repository.xml.api.RepositoryXMLParserProviderType
import com.google.common.base.Preconditions
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.SettableFuture
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import one.irradia.http.api.HTTPAuthentication
import one.irradia.http.api.HTTPClientType
import org.joda.time.LocalDateTime
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.UUID
import java.util.concurrent.Callable

class InventoryRepository(
  private val resources: InventoryStringResourcesType,
  private val executor: ListeningExecutorService,
  private val installedPackages: InstalledPackagesType,
  private val apkDirectory: InventoryAPKDirectoryType,
  private val apkInstaller: APKInstallerType,
  private val http: HTTPClientType,
  private val httpAuthentication: (URI) -> HTTPAuthentication?,
  private val repositoryParsers: RepositoryXMLParserProviderType,
  private val databaseEntry: InventoryRepositoryDatabaseEntryType,
  private val eventSubject: PublishSubject<InventoryEvent>) : InventoryRepositoryType {

  private val subscription: Disposable
  private val logger = LoggerFactory.getLogger(InventoryRepository::class.java)

  private val stateLock = Object()
  private var stateActual: InventoryRepositoryState =
    RepositoryIdle(this.databaseEntry.repository.id)
  private val packagesActual =
    mutableMapOf<String, InventoryRepositoryPackageType>()

  init {
    this.logger.debug("create: {}", this.databaseEntry.repository.id)

    this.subscription =
      this.databaseEntry.database.events.subscribe { event ->
        if (event.repositoryID == this.databaseEntry.repository.id) {
          this.onRepositoryDatabaseEvent(event)
        }
      }

    this.updateFrom(this.databaseEntry.repository)
  }

  private fun onRepositoryDatabaseEvent(event: InventoryRepositoryDatabaseEvent) {
    Preconditions.checkArgument(
      event.repositoryID == this.databaseEntry.repository.id,
      "Event must correspond to this repository")

    return when (event) {
      is DatabaseRepositoryAdded -> {
        this.logger.debug("database repository added")
      }
      is DatabaseRepositoryRemoved -> {
        this.logger.debug("database repository added")
      }
      is DatabaseRepositoryUpdated -> {
        this.logger.debug("database repository updated")
        this.updateFrom(this.databaseEntry.repository)
        Unit
      }
    }
  }

  private fun updateFrom(repository: Repository): InventoryRepository {
    this.logger.debug("update: {}", repository.id)

    val installed =
      this.installedPackages.packages()

    val events =
      synchronized(this.stateLock) {
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
            directory = this.apkDirectory,
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
    get() = synchronized(this.stateLock) {
      this.packagesActual
        .values
        .toList()
        .sortedBy(InventoryRepositoryPackageType::name)
    }

  override val events: Observable<InventoryEvent>
    get() = this.eventSubject

  override val id: UUID
    get() = this.databaseEntry.repository.id

  override val title: String
    get() = this.databaseEntry.repository.title

  override val updated: LocalDateTime
    get() = this.databaseEntry.repository.updated

  override val updateURI: URI
    get() = this.databaseEntry.repository.self

  override fun update(): ListenableFuture<Unit> {
    synchronized(this.stateLock) {
      when (this.stateActual) {
        is RepositoryUpdating -> {
          this.logger.debug("updating already")
          return SettableFuture.create()
        }
        is RepositoryUpdateFailed,
        is RepositoryIdle -> {
          this.stateActual = RepositoryUpdating(this.id)
          this.eventSubject.onNext(RepositoryChanged(this.id))
        }
      }
    }

    return this.executor.submit(Callable {
      this.logger.debug("starting update")

      when (val result = updateTask()) {
        is InventoryTaskSuccess -> {
          this.logger.debug("update succeeded")
          synchronized(this.stateLock) {
            this.stateActual = RepositoryIdle(this.id)
            this.eventSubject.onNext(RepositoryChanged(this.id))
          }
        }
        is InventoryTaskFailed -> {
          this.logger.debug("update failed")
          this.stateActual = RepositoryUpdateFailed(this.id, result.steps)
          this.eventSubject.onNext(RepositoryChanged(this.id))
        }
      }
    })
  }

  private fun updateTask(): InventoryTaskMonad<Unit> {
    return InventoryTaskRepositoryAdd(
      resources = this.resources,
      http = this.http,
      httpAuthentication = this.httpAuthentication,
      repositoryParsers = this.repositoryParsers,
      database = this.databaseEntry.database,
      uri = this.databaseEntry.repository.self)
      .execute()
      .flatMap { InventoryTaskSuccess(Unit) }
  }

  override val state: InventoryRepositoryState
    get() = synchronized(this.stateLock, this::stateActual)

}