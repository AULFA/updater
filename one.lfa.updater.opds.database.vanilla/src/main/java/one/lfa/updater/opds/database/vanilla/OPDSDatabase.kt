package one.lfa.updater.opds.database.vanilla

import com.google.common.base.Preconditions
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import one.lfa.updater.opds.api.OPDSManifest
import one.lfa.updater.opds.api.OPDSVersionCodes
import one.lfa.updater.opds.database.api.OPDSDatabaseEntryType
import one.lfa.updater.opds.database.api.OPDSDatabaseEvent
import one.lfa.updater.opds.database.api.OPDSDatabaseEvent.OPDSDatabaseEntryEvent.DatabaseEntryDeleted
import one.lfa.updater.opds.database.api.OPDSDatabaseEvent.OPDSDatabaseEntryEvent.DatabaseEntryUpdated
import one.lfa.updater.opds.database.api.OPDSDatabaseException
import one.lfa.updater.opds.database.api.OPDSDatabaseIdException
import one.lfa.updater.opds.database.api.OPDSDatabaseStringsType
import one.lfa.updater.opds.database.api.OPDSDatabaseType
import one.lfa.updater.opds.xml.api.OPDSXMLParserProviderType
import one.lfa.updater.opds.xml.api.OPDSXMLParsers
import one.lfa.updater.opds.xml.api.OPDSXMLSerializerProviderType
import one.lfa.updater.opds.xml.api.OPDSXMLSerializers
import one.lfa.updater.xml.spi.ParseError
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.StringBuilder
import java.util.UUID
import javax.annotation.concurrent.GuardedBy

/**
 * The default implementation of the [OPDSDatabaseType] interface.
 */

class OPDSDatabase private constructor(
  private val strings: OPDSDatabaseStringsType,
  private val parsers: OPDSXMLParserProviderType,
  private val serializers: OPDSXMLSerializerProviderType,
  private val directory: File
) : OPDSDatabaseType {

  private val logger = LoggerFactory.getLogger(OPDSDatabase::class.java)
  private val eventSubject = PublishSubject.create<OPDSDatabaseEvent>().toSerialized()
  private val entriesLock = Any()
  private val entries = mutableMapOf<UUID, DatabaseEntry>()

  init {
    this.logger.debug("loading catalogs from {}", this.directory)
    this.directory.mkdirs()

    val catalogDirectories: List<File> =
      (this.directory.listFiles()?.toList()
        ?: listOf())
        .filter { file -> file.isDirectory && fileIsUUID(file) }

    for (catalogDirectory in catalogDirectories) {
      val manifestFile =
        File("${catalogDirectory}.ocmx")
      val manifestFileTmp =
        File("${catalogDirectory}.ocmx.tmp")

      this.logger.debug("loading manifest {}", manifestFile)

      if (manifestFile.isFile) {
        val manifest = this.parseManifestFile(manifestFile)
        if (manifest != null) {
          synchronized(this.entriesLock) {
            this.entries[manifest.id] =
              DatabaseEntry(
                database = this,
                directory = catalogDirectory,
                manifestFile = manifestFile,
                manifestFileTmp = manifestFileTmp,
                manifestInitial = manifest
              )
          }
        }
      } else {
        this.logger.debug("manifest {} does not exist or is not a file", manifestFile)
      }
    }
  }

  private fun fileIsUUID(file: File): Boolean {
    return try {
      UUID.fromString(file.name)
      true
    } catch (e: java.lang.Exception) {
      false
    }
  }

  override val catalogs: Set<UUID>
    get() = synchronized(this.entriesLock) {
      this.entries.keys.toSet()
    }

  override fun open(id: UUID): OPDSDatabaseEntryType? {
    return synchronized(this.entriesLock) {
      this.entries[id]
    }
  }

  override fun createOrUpdate(manifest: OPDSManifest): OPDSDatabaseEntryType {
    val entry = this.open(manifest.id) ?: this.createEntry(manifest)
    entry.update(manifest)
    return entry
  }

  override fun delete(id: UUID) {
    val deleted = synchronized(this.entriesLock) {
      val entry = this.entries[id]
      if (entry != null) {
        entry.deleted = true
        entry.manifestFile.delete()
        this.entries.remove(id)
        true
      } else {
        false
      }
    }

    if (deleted) {
      this.eventSubject.onNext(DatabaseEntryDeleted(id))
    }
  }

  private fun createEntry(manifest: OPDSManifest): OPDSDatabaseEntryType {
    val catalogDirectory =
      File(this.directory, manifest.id.toString())
    val manifestFile =
      File("${catalogDirectory}.ocmx")
    val manifestFileTmp =
      File("${catalogDirectory}.ocmx.tmp")

    val entry = synchronized(this.entriesLock) {
      this.serializeManifest(manifest, manifestFile, manifestFileTmp)
      val entry =
        DatabaseEntry(
          database = this,
          directory = catalogDirectory,
          manifestFile = manifestFile,
          manifestFileTmp = manifestFileTmp,
          manifestInitial = manifest
        )
      this.entries[manifest.id] = entry
      entry
    }

    this.eventSubject.onNext(DatabaseEntryUpdated(manifest.id))
    return entry
  }

  @GuardedBy("this.entriesLock")
  private fun serializeManifest(
    manifest: OPDSManifest,
    manifestFile: File,
    manifestFileTmp: File
  ) {
    try {
      this.logger.debug("writing manifest {} -> {}", manifestFileTmp, manifestFile)

      FileOutputStream(manifestFileTmp).use { outputStream ->
        this.serializers.createSerializer(outputStream).use { serializer ->
          serializer.serialize(manifest)
        }
        manifestFileTmp.renameTo(manifestFile)
      }
    } catch (e: Exception) {
      this.logger.error("could not serialize manifest: ", e)
      throw OPDSDatabaseException(e)
    }
  }

  private class DatabaseEntry(
    private val database: OPDSDatabase,
    override val directory: File,
    internal val manifestFile: File,
    internal val manifestFileTmp: File,
    manifestInitial: OPDSManifest
  ) : OPDSDatabaseEntryType {

    @Volatile
    internal var deleted: Boolean = false

    @Volatile
    override var manifest: OPDSManifest = manifestInitial

    override val versionCode: Long
      get() = OPDSVersionCodes.timeToVersion(this.manifest.updated)

    override fun update(newManifest: OPDSManifest) {
      Preconditions.checkArgument(
        !this.deleted,
        "Database entry must not have been deleted")

      if (this.manifest.id == newManifest.id) {
        synchronized(this.database.entriesLock) {
          this.database.serializeManifest(
            manifest = newManifest,
            manifestFile = this.manifestFile,
            manifestFileTmp = this.manifestFileTmp
          )
          this.manifest = newManifest
        }
        this.database.eventSubject.onNext(DatabaseEntryUpdated(newManifest.id))
      } else {
        throw OPDSDatabaseIdException(
          strings = this.database.strings,
          expected = this.manifest.id,
          received = newManifest.id
        )
      }
    }
  }

  private fun parseManifestFile(manifestFile: File): OPDSManifest? {
    return try {
      FileInputStream(manifestFile).use { inputStream ->
        val parser =
          this.parsers.createParser(manifestFile.toURI(), inputStream)
        val errorSubscription =
          parser.errors.subscribe(this::logParseError)

        try {
          parser.parse()
        } finally {
          errorSubscription.dispose()
        }
      }
    } catch (e: Exception) {
      this.logger.error("could not parse {}: ", manifestFile, e)
      null
    }
  }

  private fun logParseError(error: ParseError) {
    when (error.severity) {
      ParseError.Severity.WARNING -> {
        this.logger.warn(
          "{}:{}:{}: {}: ",
          error.file,
          error.line,
          error.column,
          error.message,
          error.exception
        )
      }
      ParseError.Severity.ERROR -> {
        this.logger.error(
          "{}:{}:{}: {}: ",
          error.file,
          error.line,
          error.column,
          error.message,
          error.exception
        )
      }
    }
  }

  override val events: Observable<OPDSDatabaseEvent>
    get() = this.eventSubject

  companion object {

    /**
     * Open (or create) a database, loading dependencies via [ServiceLoader].
     */

    fun open(
      strings: OPDSDatabaseStringsType,
      directory: File
    ): OPDSDatabaseType {
      return this.open(
        strings = strings,
        parsers = OPDSXMLParsers.createFromServiceLoader(),
        serializers = OPDSXMLSerializers.createFromServiceLoader(),
        directory = directory.absoluteFile
      )
    }

    /**
     * Open (or create) a database.
     */

    fun open(
      strings: OPDSDatabaseStringsType,
      parsers: OPDSXMLParserProviderType,
      serializers: OPDSXMLSerializerProviderType,
      directory: File
    ): OPDSDatabaseType {
      return OPDSDatabase(
        strings = strings,
        parsers = parsers,
        serializers = serializers,
        directory = directory.absoluteFile
      )
    }
  }

}
