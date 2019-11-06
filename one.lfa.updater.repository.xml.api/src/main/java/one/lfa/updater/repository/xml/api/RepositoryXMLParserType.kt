package one.lfa.updater.repository.xml.api

import au.org.libraryforall.updater.repository.api.Repository
import io.reactivex.Observable
import one.lfa.updater.xml.spi.ParseError
import java.io.Closeable

/**
 * An XML parser.
 */

interface RepositoryXMLParserType : Closeable {

  /**
   * An observable stream of parse errors.
   */

  val errors: Observable<ParseError>

  /**
   * Parse a repository.
   */

  @Throws(RepositoryParserFailureException::class)
  fun parse(): Repository

}
