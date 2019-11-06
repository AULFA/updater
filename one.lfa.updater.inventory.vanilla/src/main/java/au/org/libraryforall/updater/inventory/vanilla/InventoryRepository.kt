package au.org.libraryforall.updater.inventory.vanilla

import one.lfa.updater.installed.api.InstalledItemsType
import au.org.libraryforall.updater.inventory.api.InventoryEvent
import au.org.libraryforall.updater.inventory.api.InventoryEvent.InventoryRepositoryEvent.InventoryRepositoryItemEvent.ItemBecameInvisible
import au.org.libraryforall.updater.inventory.api.InventoryEvent.InventoryRepositoryEvent.InventoryRepositoryItemEvent.ItemBecameVisible
import au.org.libraryforall.updater.inventory.api.InventoryEvent.InventoryRepositoryEvent.InventoryRepositoryItemEvent.ItemChanged
import au.org.libraryforall.updater.inventory.api.InventoryEvent.InventoryRepositoryEvent.RepositoryChanged
import au.org.libraryforall.updater.inventory.api.InventoryProgress
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryDatabaseEntryType
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryDatabaseEvent
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryDatabaseEvent.DatabaseRepositoryAdded
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryDatabaseEvent.DatabaseRepositoryRemoved
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryDatabaseEvent.DatabaseRepositoryUpdated
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryItemType
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryState
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryState.RepositoryIdle
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryState.RepositoryUpdateFailed
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryState.RepositoryUpdating
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryType
import au.org.libraryforall.updater.inventory.vanilla.tasks.InventoryTask
import au.org.libraryforall.updater.inventory.vanilla.tasks.InventoryTaskExecutionType
import au.org.libraryforall.updater.inventory.vanilla.tasks.InventoryTaskRepositoryAdd
import au.org.libraryforall.updater.inventory.vanilla.tasks.InventoryTaskResult
import one.lfa.updater.repository.api.Repository
import com.google.common.base.Preconditions
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.SettableFuture
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import au.org.libraryforall.updater.services.api.ServiceDirectoryType
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

  private val logger = LoggerFactory.getLogger(InventoryRepository::class.java)

  private val installedPackages =
    this.services.requireService(InstalledItemsType::class.java)

  private val subscription: Disposable
  private val stateLock = Object()
  private var stateActual: InventoryRepositoryState =
    RepositoryIdle(this.databaseEntry.repository.id)
  private val packagesActual =
    mutableMapOf<String, InventoryRepositoryItemType>()

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

  private fun mergeRepository(
    viewCurrent: MutableMap<String, InventoryRepositoryItemType>,
    repository: Repository): List<InventoryEvent> {

    val installed = this.installedPackages.items()
    val events = mutableListOf<InventoryEvent>()

    /*
     * Work out which items are new.
     */

    for (repositoryPackage in repository.itemsNewest.values) {
      if (!viewCurrent.containsKey(repositoryPackage.id)) {
        val installedPackage = installed[repositoryPackage.id]
        val installedVersion =
          installedPackage?.let { pack ->
            NamedVersion(pack.versionName, pack.versionCode)
          }

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
          val installedPackage = installed[repositoryPackage.id]
          val installedVersion =
            installedPackage?.let { pack ->
              NamedVersion(pack.versionName, pack.versionCode)
            }

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

    val future = SettableFuture.create<Unit>()

    val executionContext = object: InventoryTaskExecutionType {
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
            this.eventSubject.onNext(RepositoryChanged(this.id))
          }
        }
        is InventoryTaskResult.InventoryTaskCancelled -> {
          this.logger.debug("update cancelled")
          synchronized(this.stateLock) {
            this.stateActual = RepositoryIdle(this.id)
            this.eventSubject.onNext(RepositoryChanged(this.id))
          }
        }
        is InventoryTaskResult.InventoryTaskFailed -> {
          this.logger.debug("update failed")
          this.stateActual = RepositoryUpdateFailed(this.id, result.steps)
          this.eventSubject.onNext(RepositoryChanged(this.id))
        }
      }
    }

    return future
  }

  private fun receiveProgressUpdate(progress: InventoryProgress) {

  }

  private fun updateTask(): InventoryTask<InventoryRepositoryDatabaseEntryType> {
    return InventoryTaskRepositoryAdd.create(
      uri = this.databaseEntry.repository.self,
      requiredUUID = this.id
    )
  }

  override val state: InventoryRepositoryState
    get() = synchronized(this.stateLock, this::stateActual)

}