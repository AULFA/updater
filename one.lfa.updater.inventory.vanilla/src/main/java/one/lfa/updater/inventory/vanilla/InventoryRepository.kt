package one.lfa.updater.inventory.vanilla

import com.google.common.base.Preconditions
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.SettableFuture
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import one.lfa.updater.installed.api.InstalledApplicationsType
import one.lfa.updater.inventory.api.InventoryEvent
import one.lfa.updater.inventory.api.InventoryEvent.InventoryRepositoryEvent.InventoryRepositoryItemEvent.ItemBecameInvisible
import one.lfa.updater.inventory.api.InventoryEvent.InventoryRepositoryEvent.InventoryRepositoryItemEvent.ItemBecameVisible
import one.lfa.updater.inventory.api.InventoryEvent.InventoryRepositoryEvent.InventoryRepositoryItemEvent.ItemChanged
import one.lfa.updater.inventory.api.InventoryEvent.InventoryRepositoryEvent.RepositoryChanged
import one.lfa.updater.inventory.api.InventoryProgress
import one.lfa.updater.inventory.api.InventoryRepositoryDatabaseEntryType
import one.lfa.updater.inventory.api.InventoryRepositoryDatabaseEvent
import one.lfa.updater.inventory.api.InventoryRepositoryDatabaseEvent.DatabaseRepositoryAdded
import one.lfa.updater.inventory.api.InventoryRepositoryDatabaseEvent.DatabaseRepositoryRemoved
import one.lfa.updater.inventory.api.InventoryRepositoryDatabaseEvent.DatabaseRepositoryUpdated
import one.lfa.updater.inventory.api.InventoryRepositoryItemType
import one.lfa.updater.inventory.api.InventoryRepositoryState
import one.lfa.updater.inventory.api.InventoryRepositoryState.RepositoryIdle
import one.lfa.updater.inventory.api.InventoryRepositoryState.RepositoryUpdateFailed
import one.lfa.updater.inventory.api.InventoryRepositoryState.RepositoryUpdating
import one.lfa.updater.inventory.api.InventoryRepositoryType
import one.lfa.updater.inventory.vanilla.tasks.InventoryTask
import one.lfa.updater.inventory.vanilla.tasks.InventoryTaskExecutionType
import one.lfa.updater.inventory.vanilla.tasks.InventoryTaskRepositoryAdd
import one.lfa.updater.inventory.vanilla.tasks.InventoryTaskResult
import one.lfa.updater.opds.database.api.OPDSDatabaseEvent
import one.lfa.updater.opds.database.api.OPDSDatabaseEvent.OPDSDatabaseEntryEvent.DatabaseEntryDeleted
import one.lfa.updater.opds.database.api.OPDSDatabaseEvent.OPDSDatabaseEntryEvent.DatabaseEntryUpdated
import one.lfa.updater.opds.database.api.OPDSDatabaseType
import one.lfa.updater.repository.api.Repository
import one.lfa.updater.services.api.ServiceDirectoryType
import org.joda.time.LocalDateTime
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.UUID

class InventoryRepository internal constructor(
  private val services: ServiceDirectoryType,
  private val executor: ListeningExecutorService,
  private val databaseEntry: InventoryRepositoryDatabaseEntryType,
  private val eventSubject: PublishSubject<InventoryEvent>
) : InventoryRepositoryType {

  private val logger =
    LoggerFactory.getLogger(InventoryRepository::class.java)
  private val installedApplications =
    this.services.requireService(InstalledApplicationsType::class.java)
  private val opdsDatabase =
    this.services.requireService(OPDSDatabaseType::class.java)

  private val repositorySubscription: Disposable
  private val opdsSubscription: Disposable

  private val stateLock = Object()
  private var stateActual: InventoryRepositoryState =
    RepositoryIdle(this.databaseEntry.repository.id)

  private val packagesActual =
    mutableMapOf<String, InventoryRepositoryItem>()

  init {
    this.logger.debug("create: {}", this.databaseEntry.repository.id)

    this.repositorySubscription =
      this.databaseEntry.database.events.subscribe { event ->
        if (event.repositoryID == this.databaseEntry.repository.id) {
          this.onRepositoryDatabaseEvent(event)
        }
      }

    this.opdsSubscription = this.opdsDatabase.events.subscribe(this::onOPDSDatabaseEvent)
    this.updateFrom(this.databaseEntry.repository)
  }

  private fun onOPDSDatabaseEvent(event: OPDSDatabaseEvent) {
    return when (event) {
      is DatabaseEntryUpdated -> {
        this.logger.debug("OPDS database entry updated: {}", event.id)
        this.updateFrom(this.databaseEntry.repository)
        Unit
      }
      is DatabaseEntryDeleted -> {
        this.logger.debug("OPDS database entry deleted: {}", event.id)
        this.updateFrom(this.databaseEntry.repository)
        Unit
      }
    }
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
        this.logger.debug("database repository removed")
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

    val events =
      synchronized(this.stateLock) {
        this.mergeRepository(this.packagesActual, repository)
      }

    for (event in events) {
      this.eventSubject.onNext(event)
    }
    return this
  }

  private fun findInstalledVersionOf(id: String): NamedVersion? {

    /*
     * Check the set of installed applications to see if there's a package with a matching ID.
     */

    val installed = this.installedApplications.items()
    val installedApp = installed[id]
    if (installedApp != null) {
      return NamedVersion(installedApp.versionName, installedApp.versionCode)
    }

    /*
     * Check the OPDS database to see if there's a matching installed catalog.
     */

    val uuid = try {
      UUID.fromString(id)
    } catch (e: IllegalArgumentException) {
      null
    }

    if (uuid != null) {
      val opdsEntry = this.opdsDatabase.open(uuid)
      if (opdsEntry != null) {
        return NamedVersion(opdsEntry.versionName, opdsEntry.versionCode)
      }
    }

    return null
  }

  private fun mergeRepository(
    viewCurrent: MutableMap<String, InventoryRepositoryItem>,
    repository: Repository): List<InventoryEvent> {

    val events = mutableListOf<InventoryEvent>()

    /*
     * Work out which items are new.
     */

    for (repositoryPackage in repository.itemsNewest.values) {
      if (!viewCurrent.containsKey(repositoryPackage.id)) {
        val installedVersion =
          this.findInstalledVersionOf(repositoryPackage.id)

        val newPackage =
          InventoryRepositoryItem(
            events = this.eventSubject,
            executor = this.executor,
            initiallyInstalledVersion = installedVersion,
            repositoryId = this.id,
            item = repositoryPackage,
            services = this.services
          )

        viewCurrent[newPackage.item.id] = newPackage
        this.logger.debug("[{}]: package {} now visible", repository.id, newPackage.item.id)
        events.add(ItemBecameVisible(repositoryId = this.id, itemId = repositoryPackage.id))
      }
    }

    /*
     * Work out which items are no longer visible via this repository.
     */

    val iter = viewCurrent.iterator()
    while (iter.hasNext()) {
      val entry = iter.next()
      val existingPackage = entry.value

      if (!repository.itemsNewest.containsKey(existingPackage.item.id)) {
        this.logger.debug("[{}]: package {} now invisible", repository.id, existingPackage.item.id)
        iter.remove()
        events.add(ItemBecameInvisible(repositoryId = this.id, itemId = existingPackage.item.id))
      }
    }

    /*
     * Work out which items now have new versions.
     */

    for (repositoryPackage in repository.itemsNewest.values) {
      val existing = viewCurrent[repositoryPackage.id]
      if (existing != null) {
        if (repositoryPackage.versionCode > existing.item.versionCode) {
          val installedVersion =
            this.findInstalledVersionOf(repositoryPackage.id)

          val newPackage =
            InventoryRepositoryItem(
              events = this.eventSubject,
              executor = this.executor,
              initiallyInstalledVersion = installedVersion,
              repositoryId = this.id,
              item = repositoryPackage,
              services = this.services
            )

          viewCurrent[newPackage.item.id] = newPackage
          this.logger.debug("[{}]: package {} upgrade available", repository.id, newPackage.item.id)
          events.add(ItemChanged(repositoryId = this.id, itemId = repositoryPackage.id))
        }
      }
    }

    return events.toList()
  }

  override val items: List<InventoryRepositoryItemType>
    get() = synchronized(this.stateLock) {
      this.packagesActual
        .values
        .toList()
        .sortedBy { item ->
          item.item.name
        }
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
    this.eventSubject.onNext(synchronized(this.stateLock) {
      when (this.stateActual) {
        is RepositoryUpdating -> {
          this.logger.debug("updating already")
          return SettableFuture.create()
        }
        is RepositoryUpdateFailed,
        is RepositoryIdle -> {
          this.stateActual = RepositoryUpdating(this.id)
          RepositoryChanged(this.id)
        }
      }
    })

    val future = SettableFuture.create<Unit>()

    val executionContext = object : InventoryTaskExecutionType {
      override val services: ServiceDirectoryType
        get() = this@InventoryRepository.services
      override val onProgress: (InventoryProgress) -> Unit
        get() = this@InventoryRepository::receiveProgressUpdate
      override val isCancelRequested: Boolean
        get() = future.isCancelled
    }

    this.executor.execute {
      this.logger.debug("starting update")

      val task = this.updateTask()
      when (val result = task.evaluate(executionContext)) {
        is InventoryTaskResult.InventoryTaskSucceeded -> {
          this.logger.debug("update succeeded")
          synchronized(this.stateLock) {
            this.stateActual = RepositoryIdle(this.id)
          }
          this.eventSubject.onNext(RepositoryChanged(this.id))
        }
        is InventoryTaskResult.InventoryTaskCancelled -> {
          this.logger.debug("update cancelled")
          synchronized(this.stateLock) {
            this.stateActual = RepositoryIdle(this.id)
          }
          this.eventSubject.onNext(RepositoryChanged(this.id))
        }
        is InventoryTaskResult.InventoryTaskFailed -> {
          this.logger.debug("update failed")
          synchronized(this.stateLock) {
            this.stateActual = RepositoryUpdateFailed(this.id, result.steps)
          }
          this.eventSubject.onNext(RepositoryChanged(this.id))
        }
      }
    }

    return future
  }

  override val isTesting: Boolean =
    this.databaseEntry.repository.self.toString().contains("TESTING", ignoreCase = true)

  private fun receiveProgressUpdate(progress: InventoryProgress) {

  }

  private fun updateTask(): InventoryTask<InventoryRepositoryDatabaseEntryType> {
    return InventoryTaskRepositoryAdd.create(
      uri = this.databaseEntry.repository.self,
      requiredUUID = this.id
    )
  }

  internal fun dispose() {
    this.logger.debug("[{}]: disposing", this.id)
    this.opdsSubscription.dispose()
    this.repositorySubscription.dispose()

    synchronized(this.stateLock) {
      this.packagesActual.values.forEach {
        item -> item.dispose()
      }
    }
  }

  override val state: InventoryRepositoryState
    get() = synchronized(this.stateLock, this::stateActual)

}