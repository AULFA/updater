package au.org.libraryforall.updater.repository.xml.api

import java.io.OutputStream

/**
 * A provider of XML serializers.
 */

interface RepositoryXMLSerializerProviderType {

  /**
   * Create a serializer for the given output stream.
   */

  fun createSerializer(
    outputStream: OutputStream): RepositoryXMLSerializerType

}