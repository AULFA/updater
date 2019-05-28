package au.org.libraryforall.updater.repository.xml.spi

import au.org.libraryforall.updater.repository.api.Repository
import org.xml.sax.ext.Locator2
import java.net.URI

interface SPIFormatVersionedHandlerProviderType {

  fun createContentHandler(
    uri: URI,
    locator: Locator2): SPIFormatXMLContentHandlerType<Repository>

  val schemaDefinition: SPISchemaDefinition

}