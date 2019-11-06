package one.lfa.updater.opds.xml.api

import io.reactivex.Observable
import one.lfa.updater.opds.api.OPDSManifest
import one.lfa.updater.xml.spi.ParseError
import java.io.Closeable

/**
 * An XML parser.
 */

interface OPDSXMLParserType : Closeable {

  /**
   * An observable stream of parse errors.
   */

  val errors: Observable<ParseError>

  /**
   * Parse a repository.
   */

  @Throws(OPDSParserFailureException::class)
  fun parse(): OPDSManifest

}
