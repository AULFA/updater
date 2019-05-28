package au.org.libraryforall.updater.repository.xml.api

import au.org.libraryforall.updater.repository.api.Repository
import au.org.libraryforall.updater.repository.xml.spi.SPIFormatVersionedHandlerProviderType
import au.org.libraryforall.updater.repository.xml.spi.SPIFormatXMLContentHandlerType
import au.org.libraryforall.updater.repository.xml.spi.ParseError
import io.reactivex.subjects.PublishSubject
import org.slf4j.Logger
import org.xml.sax.Attributes
import org.xml.sax.Locator
import org.xml.sax.SAXException
import org.xml.sax.SAXParseException
import org.xml.sax.ext.DefaultHandler2
import org.xml.sax.ext.Locator2
import java.net.URI

internal class RepositoryXMLVersionedHandlerDispatcher(
  private val logger: Logger,
  private val formats: List<SPIFormatVersionedHandlerProviderType>,
  private val file: URI,
  private val errors: PublishSubject<ParseError>) : DefaultHandler2() {

  var failed: Boolean = false

  private var delegateHandler: SPIFormatXMLContentHandlerType<Repository>? = null
  private lateinit var locator: Locator2

  override fun setDocumentLocator(locator: Locator?) {
    this.locator = locator as Locator2
  }

  override fun startPrefixMapping(prefix: String, uri: String) {
    val provider =
      this.formats.find { format ->
        format.schemaDefinition.uri.toString() == uri
      }

    if (provider != null) {
      this.delegateHandler = provider.createContentHandler(this.file, this.locator)
    }
  }

  @Throws(SAXException::class)
  override fun startElement(
    namespaceURI: String,
    localName: String,
    qualifiedName: String,
    attributes: Attributes) {
    this.logger.trace("startElement: {} {} {}", namespaceURI, localName, qualifiedName)

    if (this.delegateHandler == null) {
      throw SAXException("No usable namespace found in document")
    }

    if (this.failed) {
      return
    }

    this.delegateHandler?.onElementStarted(namespaceURI, localName, qualifiedName, attributes)
  }

  @Throws(SAXException::class)
  override fun endElement(
    namespaceURI: String,
    localName: String,
    qualifiedName: String) {
    this.logger.trace("endElement:   {} {} {}", namespaceURI, localName, qualifiedName)

    if (this.failed) {
      return
    }

    this.delegateHandler?.onElementFinished(namespaceURI, localName, qualifiedName)
  }

  @Throws(SAXException::class)
  override fun characters(
    ch: CharArray,
    start: Int,
    length: Int) {
    if (this.failed) {
      return
    }

    this.delegateHandler?.onCharacters(ch, start, length)
  }

  override fun warning(e: SAXParseException) {
    this.errors.onNext(ParseError(
      line = this.locator.lineNumber,
      column = this.locator.columnNumber,
      file = this.file,
      exception = e,
      message = e.message ?: "No available error message",
      severity = ParseError.Severity.WARNING))
  }

  override fun error(e: SAXParseException) {
    this.failed = true
    this.errors.onNext(ParseError(
      line = this.locator.lineNumber,
      column = this.locator.columnNumber,
      file = this.file,
      exception = e,
      message = e.message ?: "No available error message",
      severity = ParseError.Severity.WARNING))
  }

  override fun fatalError(e: SAXParseException) {
    this.failed = true
    this.errors.onNext(ParseError(
      line = this.locator.lineNumber,
      column = this.locator.columnNumber,
      file = this.file,
      exception = e,
      message = e.message ?: "No available error message",
      severity = ParseError.Severity.WARNING))
    throw e
  }

  fun result(): Repository {
    return this.delegateHandler!!.get()
  }
}