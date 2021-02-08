package one.lfa.updater.repository.xml.v3_0

import one.lfa.updater.repository.api.Repository
import one.lfa.updater.xml.spi.SPIFormatVersionedHandlerProviderType
import one.lfa.updater.xml.spi.SPIFormatXMLContentHandlerType
import one.lfa.updater.xml.spi.SPIFormatXMLSerializerType
import one.lfa.updater.xml.spi.SPISchemaDefinition
import org.xml.sax.ext.Locator2
import java.io.OutputStream
import java.net.URI

class RepositoryXML3Format : SPIFormatVersionedHandlerProviderType<Repository> {

  override val contentClass: Class<Repository>
    get() = Repository::class.java

  companion object {
    val NAMESPACE = URI.create("urn:one.lfa.updater.repository.xml:3.0")
  }

  override fun createSerializer(outputStream: OutputStream): SPIFormatXMLSerializerType<Repository> {
    return XML3Serializer(outputStream)
  }

  override fun createContentHandler(
    uri: URI,
    locator: Locator2
  ): SPIFormatXMLContentHandlerType<Repository> =
    XML3RepositoryHandler(locator)

  override val schemaDefinition: SPISchemaDefinition =
    SPISchemaDefinition(
      versionMajor = 3,
      versionMinor = 0,
      uri = NAMESPACE,
      fileIdentifier = "schema-3.0.xsd",
      location = RepositoryXML3Format::class.java.getResource("/one/lfa/updater/repository/xml/v3_0/schema-3.0.xsd"))
}