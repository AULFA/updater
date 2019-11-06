package one.lfa.updater.inventory.vanilla

import au.org.libraryforall.updater.services.api.ServiceDirectoryType
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.SettableFuture
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import one.lfa.updater.inventory.api.InventoryAddException
import one.lfa.updater.inventory.api.InventoryEvent
import one.lfa.updater.inventory.api.InventoryEvent.InventoryStateChanged
import one.lfa.updater.inventory.api.InventoryHashIndexedDirectoryType
import one.lfa.updater.inventory.api.InventoryProgress
import one.lfa.updater.inventory.api.InventoryRemoveException
import one.lfa.updater.inventory.api.InventoryRepositoryAddResult
import one.lfa.updater.inventory.api.InventoryRepositoryDatabaseEntryType
import one.lfa.updater.inventory.api.InventoryRepositoryDatabaseType
import one.lfa.updater.inventory.api.InventoryRepositoryRemoveResult
import one.lfa.updater.inventory.api.InventoryRepositoryType
import one.lfa.updater.inventory.api.InventoryState
import one.lfa.updater.inventory.api.InventoryStringResourcesType
import one.lfa.updater.inventory.api.InventoryTaskStep
import one.lfa.updater.inventory.api.InventoryType
import one.lfa.updater.inventory.vanilla.tasks.InventoryTaskExecutionType
import one.lfa.updater.inventory.vanilla.tasks.InventoryTaskRepositoryAdd
import one.lfa.updater.inventory.vanilla.tasks.InventoryTaskRepositorySave
import one.lfa.updater.inventory.vanilla.tasks.InventoryTaskResult
import one.lfa.updater.repository.api.Repository
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.UUID
import java.util.concurrent.Callable

class Inventory private constructor(
  private val executor: ListeningExecutorService,
  private val services: ServiceDirectoryType
) : InventoryType {

  private val logger = LoggerFactory.getLogger(Inventory::class.java)

  private val stringResources =
    this.services.requireService(InventoryStringResourcesType::class.java)
  private val inventoryDatabase =
    this.services.requireService(InventoryRepositoryDatabaseType::class.java)
  private val apkDirectory =
    this.services.requireService(InventoryHashIndexedDirectoryType::class.java)

  companion object {

    /**
     * Open an inventory.
     */

    fun open(
      services: ServiceDirectoryType,
      executor: ListeningExecutorService
    ): InventoryType {
      return Inventory(
        services = services,
        executor = executor
      )
    }
  }

  private val eventSubject = PublishSubject.create<InventoryEvent>()
  private val repositoryLock = Object()
  private val repositories = mutableMapOf<UUID, InventoryRepository>()
  private var stateActual: InventoryState = InventoryState.InventoryIdle

  override val events: Observable<InventoryEvent>
    get() = this.eventSubject

  override val state: InventoryState
    get() = synchronized(this.repositoryLock, this::stateActual)

  init {
    for (entry in this.inventoryDatabase.entries) {
      this.putRepositoryForEntry(entry)
    }

    val size = synchronized(this.repositoryLock, this.repositories::size)
    this.logger.debug("initialized {} repositories", size)
  }

  override fun inventoryRepositorySelect(id: UUID): InventoryRepositoryType? =
    synchronized(this.repositoryLock) {
      this.repositories[id]
    }

  override fun inventoryRepositories(): List<InventoryRepositoryType> =
    synchronized(this.repositoryLock) {
      this.repositories.values.toList()
        .sortedBy { r -> r.title }
    }

  override fun inventoryRepositoryPut(
    repository: Repository
  ): ListenableFuture<InventoryRepositoryAddResult> {

    val future =
      SettableFuture.create<InventoryRepositoryAddResult>()

    val execution = object : InventoryTaskExecutionType {
      override val services: ServiceDirectoryType
        get() = this@Inventory.services
      override val isCancelRequested: Boolean
        get() = future.isCancelled
      override val onProgress: (InventoryProgress) -> Unit
        get() = this@Inventory::onInventoryProgress
    }

    this.executor.execute {
      val result =
        InventoryTaskRepositorySave.create(repository)
          .evaluate(execution)

      future.set(when (result) {
        is InventoryTaskResult.InventoryTaskSucceeded -> {
          InventoryRepositoryAddResult(
            uri = repository.self,
            steps = result.steps,
            repository = this.putRepositoryForEntry(result.result))
        }
        is InventoryTaskResult.InventoryTaskCancelled,
        is InventoryTaskResult.InventoryTaskFailed -> {
          InventoryRepositoryAddResult(
            uri = repository.self,
            steps = result.steps,
            repository = null)
        }
      })
    }

    return future
  }

  private fun onInventoryProgress(progress: InventoryProgress) {
    this.logger.debug("onInventoryProgress: {}", progress.status)
  }

  override fun inventoryRepositoryRemove(id: UUID): ListenableFuture<InventoryRepositoryRemoveResult> {
    synchronized(this.repositoryLock) {
      this.repositories[id] ?: return Futures.immediateFailedFuture(
        InventoryRemoveException(
          id = id,
          message = this.stringResources.repositoryRemoveNonexistent))
    }

    return this.executor.submit(Callable {
      try {
        this.inventoryDatabase.delete(id)
        synchronized(this.repositoryLock) { this.repositories.remove(id) }
        this.eventSubject.onNext(InventoryStateChanged)
        InventoryRepositoryRemoveResult(
          id, listOf(InventoryTaskStep(
          description = this.stringResources.repositoryRemoving,
          resolution = this.stringResources.repositoryRemovingSucceeded,
          exception = null,
          failed = false)))
      } catch (e: Exception) {
        this.logger.error("inventoryRepositoryRemove: ", e)
        InventoryRepositoryRemoveResult(
          id, listOf(InventoryTaskStep(
          description = this.stringResources.repositoryRemoving,
          resolution = this.stringResources.repositoryRemovingFailed,
          exception = e,
          failed = true)))
      }
    })
  }

  override fun inventoryDeleteCachedData(): ListenableFuture<List<InventoryHashIndexedDirectoryType.Deleted>> {
    return this.executor.submit(Callable { this.apkDirectory.clear() })
  }

  override fun inventoryRepositoryAdd(
    uri: URI,
    requiredUUID: UUID?
  ): ListenableFuture<InventoryRepositoryAddResult> {

    val future =
      SettableFuture.create<InventoryRepositoryAddResult>()

    synchronized(this.repositoryLock) {
      when (this.stateActual) {
        InventoryState.InventoryIdle,
        is InventoryState.InventoryAddingRepositoryFailed -> Unit
        is InventoryState.InventoryAddingRepository -> {
          future.setException(
            InventoryAddException(uri, this.stringResources.repositoryAddInProgress))
          return future
        }
      }

      val existing =
        this.repositories.values.find { repos -> repos.updateURI == uri }

      if (existing != null) {
        future.setException(
          InventoryAddException(uri, this.stringResources.repositoryAddAlreadyExists))
        return future
      }

      this.stateActual = InventoryState.InventoryAddingRepository(uri)
      this.eventSubject.onNext(InventoryStateChanged)
    }

    val executionContext = object : InventoryTaskExecutionType {
      override val services: ServiceDirectoryType
        get() = this@Inventory.services
      override val onProgress: (InventoryProgress) -> Unit
        get() = this@Inventory::onInventoryProgress
      override val isCancelRequested: Boolean
        get() = future.isCancelled
    }

    this.executor.execute {
      val task =
        InventoryTaskRepositoryAdd.create(uri, requiredUUID)
          .map { entry -> this.putRepositoryForEntry(entry) }

      val result = task.evaluate(executionContext)
      future.set(
        when (result) {
          is InventoryTaskResult.InventoryTaskSucceeded -> {
            this.stateActual = InventoryState.InventoryIdle
            this.eventSubject.onNext(InventoryStateChanged)
            InventoryRepositoryAddResult(
              uri = uri,
              steps = result.steps,
              repository = result.result)
          }

          is InventoryTaskResult.InventoryTaskFailed -> {
            this.stateActual = InventoryState.InventoryAddingRepositoryFailed(uri, result.steps)
            this.eventSubject.onNext(InventoryStateChanged)
            InventoryRepositoryAddResult(
              uri = uri,
              steps = result.steps,
              repository = null)
          }

          is InventoryTaskResult.InventoryTaskCancelled -> {
            this.stateActual = InventoryState.InventoryIdle
            this.eventSubject.onNext(InventoryStateChanged)
            InventoryRepositoryAddResult(
              uri = uri,
              steps = result.steps,
              repository = null)
          }
        })
    }

    return future
  }

  private fun putRepositoryForEntry(
    entry: InventoryRepositoryDatabaseEntryType
  ): InventoryRepository {

    val inventoryRepository =
      InventoryRepository(
        databaseEntry = entry,
        eventSubject = this.eventSubject,
        executor = this.executor,
        services = this.services
      )

    synchronized(this.repositoryLock) {
      this.repositories[entry.repository.id] = inventoryRepository
    }
    return inventoryRepository
  }
}
