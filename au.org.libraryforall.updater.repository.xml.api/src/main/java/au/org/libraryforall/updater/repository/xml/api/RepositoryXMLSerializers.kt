package au.org.libraryforall.updater.repository.xml.api

import au.org.libraryforall.updater.repository.api.Repository
import au.org.libraryforall.updater.repository.xml.spi.SPIFormatVersionedHandlerProviderType
import au.org.libraryforall.updater.repository.xml.spi.SPIFormatXMLSerializerType
import java.io.OutputStream
import java.util.ServiceLoader

class RepositoryXMLSerializers private constructor(
  private val formats: List<SPIFormatVersionedHandlerProviderType>) : RepositoryXMLSerializerProviderType {

  override fun createSerializer(outputStream: OutputStream): RepositoryXMLSerializerType {
    val highest =
      this.formats.sortedBy(SPIFormatVersionedHandlerProviderType::schemaDefinition)
        .last()

    return Serializer(highest.createSerializer(outputStream))
  }

  private class Serializer(val serializer: SPIFormatXMLSerializerType): RepositoryXMLSerializerType {
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

    fun create(formats: List<SPIFormatVersionedHandlerProviderType>): RepositoryXMLSerializerProviderType =
      RepositoryXMLSerializers(formats)

    /**
     * Create a new serializer provider using [java.util.ServiceLoader] to find formats.
     */

    fun createFromServiceLoader(): RepositoryXMLSerializerProviderType =
      this.create(ServiceLoader.load(SPIFormatVersionedHandlerProviderType::class.java).toList())
  }
}