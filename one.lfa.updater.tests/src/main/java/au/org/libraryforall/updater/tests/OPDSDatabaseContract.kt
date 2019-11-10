package au.org.libraryforall.updater.tests

import one.lfa.updater.opds.api.OPDSFile
import one.lfa.updater.opds.api.OPDSManifest
import one.lfa.updater.opds.database.api.OPDSDatabaseEvent
import one.lfa.updater.opds.database.api.OPDSDatabaseEvent.OPDSDatabaseEntryEvent.DatabaseEntryUpdated
import one.lfa.updater.opds.database.api.OPDSDatabaseException
import one.lfa.updater.opds.database.api.OPDSDatabaseStringsType
import one.lfa.updater.opds.database.api.OPDSDatabaseType
import one.lfa.updater.opds.xml.api.OPDSXMLParserProviderType
import one.lfa.updater.opds.xml.api.OPDSXMLParsers
import one.lfa.updater.opds.xml.api.OPDSXMLSerializerProviderType
import one.lfa.updater.opds.xml.api.OPDSXMLSerializerType
import one.lfa.updater.opds.xml.api.OPDSXMLSerializers
import org.joda.time.DateTime
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.mockito.Mockito
import org.slf4j.Logger
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.URI
import java.util.UUID

abstract class OPDSDatabaseContract {

  @JvmField
  @Rule
  val expectedException = ExpectedException.none()

  private lateinit var events: MutableList<OPDSDatabaseEvent>
  private lateinit var strings: InventoryStringResources
  private lateinit var directory: File

  abstract fun open(
    strings: OPDSDatabaseStringsType,
    parsers: OPDSXMLParserProviderType,
    serializers: OPDSXMLSerializerProviderType,
    directory: File
  ): OPDSDatabaseType

  abstract val logger: Logger

  @Before
  fun testSetup() {
    this.strings = InventoryStringResources()
    this.directory = TestDirectories.temporaryDirectory()
    this.events = mutableListOf()
  }

  /**
   * An empty database contains nothing.
   */

  @Test
  fun testEmpty() {
    val parsers =
      Mockito.mock(OPDSXMLParserProviderType::class.java)
    val serializers =
      Mockito.mock(OPDSXMLSerializerProviderType::class.java)

    val database = this.open(this.strings, parsers, serializers, this.directory)
    database.events.subscribe { event -> this.onEvent(event) }

    Assert.assertTrue(database.catalogs.isEmpty())
    Assert.assertTrue(this.events.isEmpty())
  }

  /**
   * Creating an entry works.
   */

  @Test
  fun testCreateSimple() {
    val parsers =
      OPDSXMLParsers.createFromServiceLoader()
    val serializers =
      OPDSXMLSerializers.createFromServiceLoader()

    val database = this.open(this.strings, parsers, serializers, this.directory)
    database.events.subscribe { event -> this.onEvent(event) }

    val manifest = this.sampleManifest()

    val entry0 = database.createOrUpdate(manifest)
    Assert.assertEquals(manifest, entry0.manifest)
    Assert.assertTrue(database.catalogs.contains(entry0.id))
    val entry1 = database.open(manifest.id)!!
    Assert.assertEquals(entry0.manifest, entry1.manifest)

    this.run {
      val event = this.events.removeAt(0)
      Assert.assertEquals(DatabaseEntryUpdated(manifest.id), event)
    }
    this.run {
      val event = this.events.removeAt(0)
      Assert.assertEquals(DatabaseEntryUpdated(manifest.id), event)
    }

    Assert.assertTrue(this.events.isEmpty())
  }

  /**
   * Creating an entry fails if the serializer fails.
   */

  @Test
  fun testCreateSerializerFails0() {
    val parsers =
      OPDSXMLParsers.createFromServiceLoader()

    val serializers =
      object: OPDSXMLSerializerProviderType {
        override fun createSerializer(outputStream: OutputStream): OPDSXMLSerializerType {
          throw IOException()
        }
      }

    val database = this.open(this.strings, parsers, serializers, this.directory)
    database.events.subscribe { event -> this.onEvent(event) }

    val manifest = this.sampleManifest()

    try {
      database.createOrUpdate(manifest)
      Assert.fail("Unreachable!")
    } catch (e: OPDSDatabaseException) {
      Assert.assertEquals(IOException::class.java, e.cause!!.javaClass)
    }

    Assert.assertTrue(database.catalogs.isEmpty())
    Assert.assertTrue(this.events.isEmpty())
  }

  /**
   * Creating an entry fails if the serializer fails.
   */

  @Test
  fun testCreateSerializerFails1() {
    val parsers =
      OPDSXMLParsers.createFromServiceLoader()

    val serializer =
      object: OPDSXMLSerializerType {
        override fun serialize(manifest: OPDSManifest) {
          throw IOException()
        }

        override fun close() {

        }
      }

    val serializers =
      object: OPDSXMLSerializerProviderType {
        override fun createSerializer(outputStream: OutputStream): OPDSXMLSerializerType {
          return serializer
        }
      }

    val database = this.open(this.strings, parsers, serializers, this.directory)
    database.events.subscribe { event -> this.onEvent(event) }

    val manifest = this.sampleManifest()

    try {
      database.createOrUpdate(manifest)
      Assert.fail("Unreachable!")
    } catch (e: OPDSDatabaseException) {
      Assert.assertEquals(IOException::class.java, e.cause!!.javaClass)
    }

    Assert.assertTrue(database.catalogs.isEmpty())
    Assert.assertTrue(this.events.isEmpty())
  }

  /**
   * The database doesn't fail to open even if manifests can't be parsed.
   */

  @Test
  fun testDatabaseCorrupt0() {
    val parsers =
      Mockito.mock(OPDSXMLParserProviderType::class.java)
    val serializers =
      Mockito.mock(OPDSXMLSerializerProviderType::class.java)

    File(this.directory, "${UUID.randomUUID()}.ocmx").writeText("Uh oh!")

    val database = this.open(this.strings, parsers, serializers, this.directory)
    database.events.subscribe { event -> this.onEvent(event) }

    Assert.assertTrue(database.catalogs.isEmpty())
    Assert.assertTrue(this.events.isEmpty())
  }

  /**
   * Opening a database with existing entries works.
   */

  @Test
  fun testOpenExisting() {
    val parsers =
      OPDSXMLParsers.createFromServiceLoader()
    val serializers =
      OPDSXMLSerializers.createFromServiceLoader()

    val manifest = this.sampleManifest()

    File(this.directory, "${manifest.id}").mkdirs()
    FileOutputStream(File(this.directory, "${manifest.id}.ocmx")).use { outputStream ->
      serializers.createSerializer(outputStream)
        .serialize(manifest)
    }

    val database = this.open(this.strings, parsers, serializers, this.directory)
    database.events.subscribe { event -> this.onEvent(event) }

    val entry1 = database.open(manifest.id)!!
    Assert.assertEquals(manifest, entry1.manifest)
    Assert.assertTrue(this.events.isEmpty())
  }

  private fun sampleManifest(): OPDSManifest {
    return OPDSManifest(
      baseURI = null,
      rootFile = URI.create("x.txt"),
      updated = DateTime.now(),
      searchIndex = null,
      id = UUID.randomUUID(),
      files = listOf(
        OPDSFile(
          file = URI.create("x.txt"),
          hash = "5891B5B522D5DF086D0FF0B110FBD9D21BB4FC7163AF34D08286A2E846F6BE03",
          hashAlgorithm = "SHA-256"
        )
      )
    )
  }

  private fun onEvent(event: OPDSDatabaseEvent) {
    this.logger.debug("event: {}", event)
    this.events.add(event)
  }
}