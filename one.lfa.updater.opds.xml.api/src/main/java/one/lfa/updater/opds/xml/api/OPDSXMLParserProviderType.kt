package one.lfa.updater.opds.xml.api

import java.io.InputStream
import java.net.URI

/**
 * An API for producing XML parsers.
 */

interface OPDSXMLParserProviderType {

  /**
   * Create a repository XML parser.
   */

  fun createParser(
    uri: URI,
    inputStream: InputStream
  ): OPDSXMLParserType

}
