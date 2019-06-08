package au.org.libraryforall.updater.tests

import au.org.libraryforall.updater.repository.xml.api.RepositoryParserFailureException
import au.org.libraryforall.updater.repository.xml.api.RepositoryXMLParserProviderType
import au.org.libraryforall.updater.repository.xml.api.RepositoryXMLSerializerProviderType
import au.org.libraryforall.updater.repository.xml.spi.ParseError
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

abstract class RepositoryXMLParserContract {

  abstract fun logger(): Logger

  abstract fun repositoryXMLParsers(): RepositoryXMLParserProviderType

  abstract fun repositoryXMLSerializers(): RepositoryXMLSerializerProviderType

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
    val parsers = this.repositoryXMLParsers()
    val parser =
      parsers.createParser(
        uri = URI.create("urn:example"),
        inputStream = this.stream("simple.xml"))

    parser.errors.subscribe(loggingConsumer())
    val repository = parser.parse()

    Assert.assertEquals("79b82d4a-02d1-4dd4-81e3-37dc37b8a5d0", repository.id.toString())
    Assert.assertEquals("Example Repository", repository.title)
    Assert.assertEquals(URI.create("http://www.example.com/self"), repository.self)
    Assert.assertEquals("2019-01-01T00:00:00.000", repository.updated.toString())

    Assert.assertEquals(3, repository.packages.size)

    val p0 = repository.packages[0]
    Assert.assertEquals("com.example.p0", p0.id)
    Assert.assertEquals(23, p0.versionCode)
    Assert.assertEquals("1.0.0", p0.versionName)
    Assert.assertEquals("87298cc2f31fba73181ea2a9e6ef10dce21ed95e98bdac9c4e1504ea16f486e4", p0.sha256.text)
    Assert.assertEquals("http://www.example.com/1", p0.source.toASCIIString())

    val p1 = repository.packages[1]
    Assert.assertEquals("com.example.p1", p1.id)
    Assert.assertEquals(22, p1.versionCode)
    Assert.assertEquals("1.0.2", p1.versionName)
    Assert.assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", p1.sha256.text)
    Assert.assertEquals("http://www.example.com/2", p1.source.toASCIIString())

    val p2 = repository.packages[2]
    Assert.assertEquals("com.example.p2", p2.id)
    Assert.assertEquals(24, p2.versionCode)
    Assert.assertEquals("1.0.4", p2.versionName)
    Assert.assertEquals("47ea70cf08872bdb4afad3432b01d963ac7d165f6b575cd72ef47498f4459a90", p2.sha256.text)
    Assert.assertEquals("http://www.example.com/3", p2.source.toASCIIString())
  }

  @Test
  fun testInvalidWrongNamespace() {
    val parsers = this.repositoryXMLParsers()
    val parser =
      parsers.createParser(
        uri = URI.create("urn:example"),
        inputStream = this.stream("wrong-namespace.xml"))

    parser.errors.subscribe(loggingConsumer())
    this.expectedException.expect(RepositoryParserFailureException::class.java)
    val repository = parser.parse()
  }

  @Test
  fun testInvalidMissingNamespace() {
    val parsers = this.repositoryXMLParsers()
    val parser =
      parsers.createParser(
        uri = URI.create("urn:example"),
        inputStream = this.stream("missing-namespace.xml"))

    parser.errors.subscribe(loggingConsumer())
    this.expectedException.expect(RepositoryParserFailureException::class.java)
    val repository = parser.parse()
  }

  @Test
  fun testInvalidBadRepository0() {
    val parsers = this.repositoryXMLParsers()
    val parser =
      parsers.createParser(
        uri = URI.create("urn:example"),
        inputStream = this.stream("bad-repository-0.xml"))

    parser.errors.subscribe(loggingConsumer())
    this.expectedException.expect(RepositoryParserFailureException::class.java)
    parser.parse()
  }

  @Test
  fun testInvalidBadRepository1() {
    val parsers = this.repositoryXMLParsers()
    val parser =
      parsers.createParser(
        uri = URI.create("urn:example"),
        inputStream = this.stream("bad-repository-1.xml"))

    parser.errors.subscribe(loggingConsumer())
    this.expectedException.expect(RepositoryParserFailureException::class.java)
    parser.parse()
  }

  @Test
  fun testInvalidBadRepository2() {
    val parsers = this.repositoryXMLParsers()
    val parser =
      parsers.createParser(
        uri = URI.create("urn:example"),
        inputStream = this.stream("bad-repository-2.xml"))

    parser.errors.subscribe(loggingConsumer())
    this.expectedException.expect(RepositoryParserFailureException::class.java)
    parser.parse()
  }

  @Test
  fun testInvalidBadRepository3() {
    val parsers = this.repositoryXMLParsers()
    val parser =
      parsers.createParser(
        uri = URI.create("urn:example"),
        inputStream = this.stream("bad-repository-3.xml"))

    parser.errors.subscribe(loggingConsumer())
    this.expectedException.expect(RepositoryParserFailureException::class.java)
    parser.parse()
  }

  @Test
  fun testInvalidBadPackage0() {
    val parsers = this.repositoryXMLParsers()
    val parser =
      parsers.createParser(
        uri = URI.create("urn:example"),
        inputStream = this.stream("bad-package-0.xml"))

    parser.errors.subscribe(loggingConsumer())
    this.expectedException.expect(RepositoryParserFailureException::class.java)
    parser.parse()
  }

  @Test
  fun testInvalidBadPackage1() {
    val parsers = this.repositoryXMLParsers()
    val parser =
      parsers.createParser(
        uri = URI.create("urn:example"),
        inputStream = this.stream("bad-package-1.xml"))

    parser.errors.subscribe(loggingConsumer())
    this.expectedException.expect(RepositoryParserFailureException::class.java)
    parser.parse()
  }

  @Test
  fun testInvalidBadPackage2() {
    val parsers = this.repositoryXMLParsers()
    val parser =
      parsers.createParser(
        uri = URI.create("urn:example"),
        inputStream = this.stream("bad-package-2.xml"))

    parser.errors.subscribe(loggingConsumer())
    this.expectedException.expect(RepositoryParserFailureException::class.java)
    parser.parse()
  }

  @Test
  fun testInvalidBadPackage3() {
    val parsers = this.repositoryXMLParsers()
    val parser =
      parsers.createParser(
        uri = URI.create("urn:example"),
        inputStream = this.stream("bad-package-3.xml"))

    parser.errors.subscribe(loggingConsumer())
    this.expectedException.expect(RepositoryParserFailureException::class.java)
    parser.parse()
  }

  @Test
  fun testRoundTrip() {
    val parsers = this.repositoryXMLParsers()
    val serializers = this.repositoryXMLSerializers()

    val parser =
      parsers.createParser(
        uri = URI.create("urn:example"),
        inputStream = this.stream("simple.xml"))

    parser.errors.subscribe(loggingConsumer())
    val repositoryOriginal = parser.parse()

    val temporary =
      File.createTempFile("repository-xml", ".xml")

    this.logger.debug("temporary: ${temporary}")

    FileOutputStream(temporary).use { stream ->
      val serializer = serializers.createSerializer(stream)
      serializer.serialize(repositoryOriginal)
    }

    FileInputStream(temporary).use { stream ->
      val parser =
        parsers.createParser(uri = URI.create("urn:example"), inputStream = stream)
      parser.errors.subscribe(loggingConsumer())
      val repositorySerialized = parser.parse()
      Assert.assertEquals(repositoryOriginal, repositorySerialized)
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
    return RepositoryXMLParserContract::class.java.getResourceAsStream(
      "/au/org/libraryforall/updater/tests/${name}")!!
  }

}