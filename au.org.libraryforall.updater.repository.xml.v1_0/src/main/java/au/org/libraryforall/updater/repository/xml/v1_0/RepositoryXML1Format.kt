package au.org.libraryforall.updater.repository.xml.v1_0

import au.org.libraryforall.updater.repository.api.Repository
import au.org.libraryforall.updater.repository.xml.spi.SPIFormatVersionedHandlerProviderType
import au.org.libraryforall.updater.repository.xml.spi.SPIFormatXMLContentHandlerType
import au.org.libraryforall.updater.repository.xml.spi.SPISchemaDefinition
import org.xml.sax.ext.Locator2
import java.net.URI

class RepositoryXML1Format : SPIFormatVersionedHandlerProviderType {

  override fun createContentHandler(
    uri: URI,
    locator: Locator2
  ): SPIFormatXMLContentHandlerType<Repository> =
    XML1RepositoryHandler(uri, locator)

  override val schemaDefinition: SPISchemaDefinition =
    SPISchemaDefinition(
      uri = URI.create("urn:au.org.libraryforall.updater.repository.xml:1.0"),
      fileIdentifier = "schema-1.0.xsd",
      location = RepositoryXML1Format::class.java.getResource("/au/org/libraryforall/updater/repository/xml/v1_0/schema-1.0.xsd"))
}