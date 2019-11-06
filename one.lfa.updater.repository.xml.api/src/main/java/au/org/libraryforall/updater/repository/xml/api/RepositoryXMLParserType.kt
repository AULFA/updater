package au.org.libraryforall.updater.repository.xml.api

import au.org.libraryforall.updater.repository.api.Repository
import au.org.libraryforall.updater.repository.xml.spi.ParseError
import io.reactivex.Observable
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
