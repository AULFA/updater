package au.org.libraryforall.updater.tests

import one.lfa.updater.opds.xml.api.OPDSXMLParserProviderType
import one.lfa.updater.opds.xml.api.OPDSXMLSerializerProviderType
import one.lfa.updater.xml.spi.ParseError
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.slf4j.Logger
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URI

abstract class OPDSManifestXMLv1ParserContract {

  abstract fun logger(): Logger

  abstract fun xmlParsers(): OPDSXMLParserProviderType

  abstract fun xmlSerializers(): OPDSXMLSerializerProviderType

  @JvmField
  @Rule
  val expectedException = ExpectedException.none()

  private lateinit var logger: Logger

  @Before
  fun setup() {
    this.logger = logger()
  }

  @Test
  fun testSimple() {
    val parsers = this.xmlParsers()
    val parser =
      parsers.createParser(
        uri = URI.create("urn:example"),
        inputStream = this.stream("simple.xml"))

    parser.errors.subscribe(loggingConsumer())
    val opds = parser.parse()
  }

  @Test
  fun testRoundTrip() {
    val parsers = this.xmlParsers()
    val serializers = this.xmlSerializers()

    val parser =
      parsers.createParser(
        uri = URI.create("urn:example"),
        inputStream = this.stream("simple.xml"))

    parser.errors.subscribe(loggingConsumer())
    val opdsOriginal = parser.parse()

    val temporary =
      File.createTempFile("opds-xml", ".xml")

    this.logger.debug("temporary: ${temporary}")

    FileOutputStream(temporary).use { stream ->
      val serializer = serializers.createSerializer(stream)
      serializer.serialize(opdsOriginal)
    }

    FileInputStream(temporary).use { stream ->
      val parser =
        parsers.createParser(uri = URI.create("urn:example"), inputStream = stream)
      parser.errors.subscribe(loggingConsumer())
      val opdsSerialized = parser.parse()
      Assert.assertEquals(opdsOriginal, opdsSerialized)
    }
  }

  private fun loggingConsumer(): (ParseError) -> Unit {
    return { error ->
      when (error.severity) {
        ParseError.Severity.WARNING ->
          this.logger.warn("{}:{}: {}", error.line, error.column, error.message)
        ParseError.Severity.ERROR ->
          this.logger.error("{}:{}: {}", error.line, error.column, error.message)
      }
    }
  }

  private fun stream(name: String): InputStream {
    return OPDSManifestXMLv1ParserContract::class.java.getResourceAsStream(
      "/au/org/libraryforall/updater/tests/opds/v1/${name}")!!
  }

}