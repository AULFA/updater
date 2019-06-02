package au.org.libraryforall.updater.inventory.vanilla

import au.org.libraryforall.updater.inventory.api.InventoryRepositoryDatabaseEntryType
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryDatabaseEvent
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryDatabaseEvent.*
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryDatabaseType
import au.org.libraryforall.updater.repository.api.Repository
import au.org.libraryforall.updater.repository.xml.api.RepositoryXMLParserProviderType
import au.org.libraryforall.updater.repository.xml.api.RepositoryXMLSerializerProviderType
import au.org.libraryforall.updater.repository.xml.spi.ParseError
import com.google.common.base.Preconditions
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

class InventoryRepositoryDatabase private constructor(
  private val parsers: RepositoryXMLParserProviderType,
  private val serializers: RepositoryXMLSerializerProviderType,
  private val directory: File)
  : InventoryRepositoryDatabaseType {

  private val logger = LoggerFactory.getLogger(InventoryRepositoryDatabase::class.java)

  private val eventSubject = PublishSubject.create<InventoryRepositoryDatabaseEvent>()
  private val entriesLock = Object()
  private val entriesCurrent: MutableMap<UUID, Entry> = mutableMapOf()

  init {
    if (!this.directory.isDirectory) {
      val ok = this.directory.mkdirs()
      if (!ok) {
        throw IOException("Could not create directory: ${this.directory}")
      }
    }

    this.openEntries()
  }

  private fun openEntries() {
    val files = this.directory.listFiles()
    for (file in files) {
      val entry = this.openEntry(file)
      if (entry != null) {
        synchronized(this.entriesLock) {
          this.entriesCurrent[entry.repository.id] = entry
        }
      }
    }

    val size = synchronized(this.entriesLock, this.entriesCurrent::size)
    this.logger.debug("opened {} entries", size)
  }

  private fun delete(file: File) {
    this.logger.debug("delete (recursive): {}", file)
    file.deleteRecursively()
  }

  private fun openRepository(
    baseDirectory: File
  ): Repository {
    val file = File(baseDirectory, "repository.xml")
    return FileInputStream(file).use { stream ->
      this.parsers.createParser(file.toURI(), stream)
        .use { parser ->
          val errors = mutableListOf<ParseError>()
          parser.errors.subscribe { error -> errors.add(error) }
          try {
            parser.parse()
          } catch (e: Exception) {
            for (error in errors) {
              this.logger.error("{}:{}:{}: {}", error.file, error.line, error.column, error.message)
              if (error.exception != null) {
                this.logger.error("exception: ", error.exception)
              }
            }
            throw e
          }
        }
    }
  }

  private fun openEntry(baseDirectory: File): Entry? {
    this.logger.debug("openEntry: {}", baseDirectory)

    val name = baseDirectory.name
    if (!this.nameIsUUID(name)) {
      this.logger.error("directory name {} is not a UUID", name)
      this.delete(baseDirectory)
      return null
    }

    return try {
      baseDirectory.mkdirs()
      return this.Entry(baseDirectory, openRepository(baseDirectory))
    } catch (e: Exception) {
      this.logger.error("openEntry: ", e)
      this.delete(baseDirectory)
      null
    }
  }

  private fun nameIsUUID(name: String): Boolean {
    return try {
      UUID.fromString(name)
      true
    } catch (e: Exception) {
      false
    }
  }

  private inner class Entry(
    private val baseDirectory: File,
    repositoryInitial: Repository)
    : InventoryRepositoryDatabaseEntryType {

    val fileXML =
      File(this.baseDirectory, "repository.xml")
    val fileXMLTemp =
      File(this.baseDirectory, "repository.xml.tmp")

    private val entryLock = Object()
    private var entryRepositoryCurrent = repositoryInitial

    override val repository: Repository
      get() = synchronized(this.entryLock, this::entryRepositoryCurrent)

    override val database: InventoryRepositoryDatabaseType =
      this@InventoryRepositoryDatabase

    override fun update(repository: Repository) {
      this@InventoryRepositoryDatabase.logger.debug("update: {}", repository.id)

      this.baseDirectory.mkdirs()

      synchronized(this.entryLock) {
        FileOutputStream(this.fileXMLTemp).use { output ->
          this@InventoryRepositoryDatabase.serializers.createSerializer(output).use { serializer ->
            serializer.serialize(repository)
            output.flush()
          }
          this.fileXMLTemp.renameTo(this.fileXML)
          this.entryRepositoryCurrent = repository
        }
      }
    }

    fun delete() {
      this.baseDirectory.deleteRecursively()
    }
  }

  override fun createOrUpdate(repository: Repository): InventoryRepositoryDatabaseEntryType {
    val existing =
      synchronized(this.entriesLock) {
        this.entriesCurrent[repository.id]
      }

    return if (existing != null) {
      this.logger.debug("createOrUpdate: {} exists", repository.id)
      Preconditions.checkArgument(
        existing.repository.id == repository.id,
        "Repository IDs must match")

      existing.update(repository)
      this.eventSubject.onNext(DatabaseRepositoryUpdated(repository.id))
      existing
    } else {
      this.logger.debug("createOrUpdate: {} does not exist", repository.id)
      val baseDirectory = File(this.directory, repository.id.toString())
      val entry =
        this.Entry(baseDirectory, repository)
      entry.update(repository)
      this.eventSubject.onNext(DatabaseRepositoryAdded(repository.id))
      entry
    }
  }

  override fun delete(id: UUID) {
    val existing =
      synchronized(this.entriesLock) {
        this.entriesCurrent[id]
      }

    return if (existing != null) {
      this.logger.debug("delete: {} exists", id)
      Preconditions.checkArgument(
        existing.repository.id == id,
        "Repository IDs must match")

      existing.delete()
      this.eventSubject.onNext(DatabaseRepositoryRemoved(id))
      synchronized(this.entriesLock) { this.entriesCurrent.remove(id) }
      Unit
    } else {
      this.logger.debug("delete: {} does not exist", id)
    }
  }

  override val events: Observable<InventoryRepositoryDatabaseEvent>
    get() = this.eventSubject

  override val entries: List<InventoryRepositoryDatabaseEntryType>
    get() = synchronized(this.entriesLock) {
      this.entriesCurrent.values.toList()
        .sortedBy { e -> e.repository.title }
    }

  companion object {

    fun create(
      parsers: RepositoryXMLParserProviderType,
      serializers: RepositoryXMLSerializerProviderType,
      directory: File): InventoryRepositoryDatabaseType =
      InventoryRepositoryDatabase(
        parsers = parsers,
        serializers = serializers,
        directory = directory)
  }
}