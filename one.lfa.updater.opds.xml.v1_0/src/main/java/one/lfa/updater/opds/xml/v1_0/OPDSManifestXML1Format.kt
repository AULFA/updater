package one.lfa.updater.opds.xml.v1_0

import one.lfa.updater.opds.api.OPDSManifest
import one.lfa.updater.xml.spi.SPIFormatVersionedHandlerProviderType
import one.lfa.updater.xml.spi.SPIFormatXMLContentHandlerType
import one.lfa.updater.xml.spi.SPIFormatXMLSerializerType
import one.lfa.updater.xml.spi.SPISchemaDefinition
import org.xml.sax.ext.Locator2
import java.io.OutputStream
import java.net.URI

class OPDSManifestXML1Format : SPIFormatVersionedHandlerProviderType<OPDSManifest> {

  override val contentClass: Class<OPDSManifest>
    get() = OPDSManifest::class.java

  companion object {
    val NAMESPACE = URI.create("urn:one.lfa.opdsget.manifest.xml:1:0")
  }

  override fun createSerializer(outputStream: OutputStream): SPIFormatXMLSerializerType<OPDSManifest> {
    return OPDSManifestXML1Serializer(outputStream)
  }

  override fun createContentHandler(
    uri: URI,
    locator: Locator2
  ): SPIFormatXMLContentHandlerType<OPDSManifest> =
    OPDSManifestXML1ManifestHandler(uri, locator)

  override val schemaDefinition: SPISchemaDefinition =
    SPISchemaDefinition(
      versionMajor = 1,
      versionMinor = 0,
      uri = NAMESPACE,
      fileIdentifier = "schema-1.0.xsd",
      location = OPDSManifestXML1Format::class.java.getResource("/one/lfa/updater/opds/xml/v1_0/schema-1.0.xsd"))
}