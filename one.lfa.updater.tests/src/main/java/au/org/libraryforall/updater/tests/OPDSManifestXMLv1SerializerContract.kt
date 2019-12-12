package au.org.libraryforall.updater.tests

import one.lfa.updater.opds.api.OPDSFile
import one.lfa.updater.opds.api.OPDSManifest
import one.lfa.updater.opds.xml.api.OPDSXMLSerializerProviderType
import org.joda.time.DateTime
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.slf4j.Logger
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URI
import java.util.UUID

abstract class OPDSManifestXMLv1SerializerContract {

  abstract fun logger(): Logger

  abstract fun xmlSerializers(): OPDSXMLSerializerProviderType

  @JvmField
  @Rule
  val expectedException = ExpectedException.none()

  private lateinit var logger: Logger

  @Before
  fun setup() {
    this.logger = logger()
  }

  private fun sampleManifest(): OPDSManifest {
    return OPDSManifest(
      baseURI = URI.create("base"),
      rootFile = URI.create("x.txt"),
      updated = DateTime.now(),
      searchIndex = URI.create("x.txt"),
      id = UUID.randomUUID(),
      title = "",
      files = listOf(
        OPDSFile(
          file = URI.create("x.txt"),
          hash = "5891B5B522D5DF086D0FF0B110FBD9D21BB4FC7163AF34D08286A2E846F6BE03",
          hashAlgorithm = "SHA-256"
        )
      )
    )
  }

  @Test
  fun testSimple() {
    val serializers =
      this.xmlSerializers()
    val serializer =
      serializers.createSerializer(ByteArrayOutputStream())
    serializer.serialize(this.sampleManifest())
  }

  @Test
  fun testRefuseEmptyFiles() {
    val serializers =
      this.xmlSerializers()
    val serializer =
      serializers.createSerializer(ByteArrayOutputStream())

    this.expectedException.expect(IOException::class.java)
    this.expectedException.expectMessage("Refusing")
    serializer.serialize(this.sampleManifest().copy(files = listOf()))
  }
}