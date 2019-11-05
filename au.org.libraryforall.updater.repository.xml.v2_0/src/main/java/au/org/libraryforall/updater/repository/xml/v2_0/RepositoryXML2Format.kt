package au.org.libraryforall.updater.repository.xml.v2_0

import au.org.libraryforall.updater.repository.api.Repository
import au.org.libraryforall.updater.repository.xml.spi.SPIFormatVersionedHandlerProviderType
import au.org.libraryforall.updater.repository.xml.spi.SPIFormatXMLContentHandlerType
import au.org.libraryforall.updater.repository.xml.spi.SPIFormatXMLSerializerType
import au.org.libraryforall.updater.repository.xml.spi.SPISchemaDefinition
import org.xml.sax.ext.Locator2
import java.io.OutputStream
import java.net.URI

class RepositoryXML2Format : SPIFormatVersionedHandlerProviderType {

  companion object {
    val NAMESPACE = URI.create("urn:au.org.libraryforall.updater.repository.xml:2.0")
  }

  override fun createSerializer(outputStream: OutputStream): SPIFormatXMLSerializerType {
    return XML2Serializer(outputStream)
  }

  override fun createContentHandler(
    uri: URI,
    locator: Locator2
  ): SPIFormatXMLContentHandlerType<Repository> =
    XML2RepositoryHandler(locator)

  override val schemaDefinition: SPISchemaDefinition =
    SPISchemaDefinition(
      versionMajor = 2,
      versionMinor = 0,
      uri = NAMESPACE,
      fileIdentifier = "schema-2.0.xsd",
      location = RepositoryXML2Format::class.java.getResource("/au/org/libraryforall/updater/repository/xml/v2_0/schema-2.0.xsd"))
}