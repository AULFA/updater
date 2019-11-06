package one.lfa.updater.repository.xml.api

import au.org.libraryforall.updater.repository.api.Repository
import one.lfa.updater.xml.spi.SPIFormatVersionedHandlerProviderType
import one.lfa.updater.xml.spi.SPIFormatXMLSerializerType
import java.io.OutputStream
import java.util.ServiceLoader

class RepositoryXMLSerializers private constructor(
  private val formats: List<SPIFormatVersionedHandlerProviderType<Repository>>
) : RepositoryXMLSerializerProviderType {

  override fun createSerializer(outputStream: OutputStream): RepositoryXMLSerializerType {
    val highest =
      this.formats.sortedBy(SPIFormatVersionedHandlerProviderType<Repository>::schemaDefinition)
        .last()

    return Serializer(highest.createSerializer(outputStream))
  }

  private class Serializer(val serializer: SPIFormatXMLSerializerType<Repository>) : RepositoryXMLSerializerType {
    override fun close() {
      this.serializer.close()
    }

    override fun serialize(repository: Repository) {
      this.serializer.serialize(repository)
    }
  }

  companion object {

    /**
     * Create a new serializer provider using the given list of formats.
     */

    fun create(formats: List<SPIFormatVersionedHandlerProviderType<Repository>>): RepositoryXMLSerializerProviderType =
      RepositoryXMLSerializers(formats)

    /**
     * Create a new serializer provider using [java.util.ServiceLoader] to find formats.
     */

    fun createFromServiceLoader(): RepositoryXMLSerializerProviderType {
      val providers =
        ServiceLoader.load(SPIFormatVersionedHandlerProviderType::class.java)
          .filter { provider -> provider.contentClass == Repository::class.java }
          .map { provider -> provider as (SPIFormatVersionedHandlerProviderType<Repository>) }
      return create(providers)
    }
  }
}