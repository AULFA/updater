package au.org.libraryforall.updater.repository.xml.v1_0

import one.lfa.updater.repository.api.Repository
import one.lfa.updater.xml.spi.SPIFormatVersionedHandlerProviderType
import one.lfa.updater.xml.spi.SPIFormatXMLContentHandlerType
import one.lfa.updater.xml.spi.SPIFormatXMLSerializerType
import one.lfa.updater.xml.spi.SPISchemaDefinition
import org.xml.sax.ext.Locator2
import java.io.OutputStream
import java.net.URI

class RepositoryXML1Format : SPIFormatVersionedHandlerProviderType<Repository> {

  override val contentClass: Class<Repository>
    get() = Repository::class.java

  companion object {
    val NAMESPACE = URI.create("urn:au.org.libraryforall.updater.repository.xml:1.0")
  }

  override fun createSerializer(outputStream: OutputStream): SPIFormatXMLSerializerType<Repository> {
    return XML1Serializer(outputStream)
  }

  override fun createContentHandler(
    uri: URI,
    locator: Locator2
  ): SPIFormatXMLContentHandlerType<Repository> =
    XML1RepositoryHandler(locator)

  override val schemaDefinition: SPISchemaDefinition =
    SPISchemaDefinition(
      versionMajor = 1,
      versionMinor = 0,
      uri = NAMESPACE,
      fileIdentifier = "schema-1.0.xsd",
      location = RepositoryXML1Format::class.java.getResource("/au/org/libraryforall/updater/repository/xml/v1_0/schema-1.0.xsd"))
}