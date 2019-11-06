package one.lfa.updater.xml.spi

import org.xml.sax.ext.Locator2
import java.io.OutputStream
import java.net.URI

interface SPIFormatVersionedHandlerProviderType<T> {

  val contentClass: Class<T>

  fun createContentHandler(
    uri: URI,
    locator: Locator2
  ): SPIFormatXMLContentHandlerType<T>

  fun createSerializer(
    outputStream: OutputStream
  ): SPIFormatXMLSerializerType<T>

  val schemaDefinition: SPISchemaDefinition

}