package one.lfa.updater.opds.xml.api

import java.io.OutputStream

/**
 * A provider of XML serializers.
 */

interface OPDSXMLSerializerProviderType {

  /**
   * Create a serializer for the given output stream.
   */

  fun createSerializer(
    outputStream: OutputStream
  ): OPDSXMLSerializerType

}