package au.org.libraryforall.updater.repository.xml.spi

import one.lfa.updater.repository.api.Repository
import org.xml.sax.ext.Locator2
import java.io.OutputStream
import java.net.URI

interface SPIFormatVersionedHandlerProviderType {

  fun createContentHandler(
    uri: URI,
    locator: Locator2): SPIFormatXMLContentHandlerType<Repository>

  fun createSerializer(
    outputStream: OutputStream): SPIFormatXMLSerializerType

  val schemaDefinition: SPISchemaDefinition

}