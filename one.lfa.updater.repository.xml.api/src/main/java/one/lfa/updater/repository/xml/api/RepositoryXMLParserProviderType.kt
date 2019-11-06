package one.lfa.updater.repository.xml.api

import java.io.InputStream
import java.net.URI

/**
 * An API for producing XML parsers.
 */

interface RepositoryXMLParserProviderType {

  /**
   * Create a repository XML parser.
   */

  fun createParser(
    uri: URI,
    inputStream: InputStream): RepositoryXMLParserType

}
