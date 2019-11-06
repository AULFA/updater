package one.lfa.updater.opds.xml.api

import one.lfa.updater.opds.api.OPDSManifest
import one.lfa.updater.xml.spi.SPIFormatVersionedHandlerProviderType
import one.lfa.updater.xml.spi.SPIFormatXMLSerializerType
import java.io.OutputStream
import java.util.ServiceLoader

class OPDSXMLSerializers private constructor(
  private val formats: List<SPIFormatVersionedHandlerProviderType<OPDSManifest>>
) : OPDSXMLSerializerProviderType {

  override fun createSerializer(outputStream: OutputStream): OPDSXMLSerializerType {
    val highest =
      this.formats.sortedBy(SPIFormatVersionedHandlerProviderType<OPDSManifest>::schemaDefinition)
        .last()

    return Serializer(highest.createSerializer(outputStream))
  }

  private class Serializer(val serializer: SPIFormatXMLSerializerType<OPDSManifest>) : OPDSXMLSerializerType {
    override fun close() {
      this.serializer.close()
    }

    override fun serialize(manifest: OPDSManifest) {
      this.serializer.serialize(manifest)
    }
  }

  companion object {

    /**
     * Create a new serializer provider using the given list of formats.
     */

    fun create(formats: List<SPIFormatVersionedHandlerProviderType<OPDSManifest>>): OPDSXMLSerializerProviderType =
      OPDSXMLSerializers(formats)

    /**
     * Create a new serializer provider using [java.util.ServiceLoader] to find formats.
     */

    fun createFromServiceLoader(): OPDSXMLSerializerProviderType {
      val providers =
        ServiceLoader.load(SPIFormatVersionedHandlerProviderType::class.java)
          .filter { provider -> provider.contentClass == OPDSManifest::class.java }
          .map { provider -> provider as (SPIFormatVersionedHandlerProviderType<OPDSManifest>) }
      return create(providers)
    }
  }
}