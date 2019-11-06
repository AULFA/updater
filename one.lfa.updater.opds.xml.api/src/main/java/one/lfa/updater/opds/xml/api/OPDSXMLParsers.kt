package one.lfa.updater.opds.xml.api

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import one.lfa.updater.opds.api.OPDSManifest
import one.lfa.updater.xml.spi.ParseError
import one.lfa.updater.xml.spi.SPIFormatVersionedHandlerProviderType
import one.lfa.updater.xml.spi.SPIHardenedSAXParsers
import one.lfa.updater.xml.spi.SPISchemaResolutionMappings
import one.lfa.updater.xml.spi.XInclude
import org.slf4j.LoggerFactory
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.XMLReader
import java.io.InputStream
import java.net.URI
import java.util.ServiceLoader
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The default API for XML parsers.
 */

class OPDSXMLParsers private constructor(
  private val formats: List<SPIFormatVersionedHandlerProviderType<OPDSManifest>>
) : OPDSXMLParserProviderType {

  private var mappings: SPISchemaResolutionMappings
  private val parsers = SPIHardenedSAXParsers()
  private val logger = LoggerFactory.getLogger(OPDSXMLParsers::class.java)

  init {
    this.logger.debug("{} format providers available", this.formats.size)

    this.formats.forEach { provider ->
      this.logger.debug("format provider: {}: {}",
        provider.javaClass.canonicalName,
        provider.schemaDefinition.uri)
    }

    this.mappings =
      SPISchemaResolutionMappings(
        this.formats.map { provider ->
          Pair(provider.schemaDefinition.uri, provider.schemaDefinition)
        }.toMap())
  }

  private inner class Parser(
    private val uri: URI,
    private val inputStream: InputStream,
    private val reader: XMLReader) : OPDSXMLParserType {

    private val events = PublishSubject.create<ParseError>()
    private val closed = AtomicBoolean(false)

    override fun close() {
      if (this.closed.compareAndSet(false, true)) {
        this.inputStream.close()
        this.events.onComplete()
      }
    }

    override val errors: Observable<ParseError> =
      this.events

    override fun parse(): OPDSManifest {
      if (this.closed.get()) {
        throw IllegalStateException("Parser is closed!")
      }

      val handler =
        OPDSXMLVersionedHandlerDispatcher(
          logger = this@OPDSXMLParsers.logger,
          formats = this@OPDSXMLParsers.formats,
          file = this.uri,
          errors = events)

      this.reader.contentHandler = handler
      this.reader.errorHandler = handler

      try {
        this.reader.parse(InputSource(this.inputStream))
        if (handler.failed) {
          throw OPDSParserFailureException("Parsing failed")
        }

        return handler.result()
      } catch (e: SAXException) {
        throw OPDSParserFailureException(e.message
          ?: "No available exception message", e)
      }
    }
  }

  override fun createParser(uri: URI, inputStream: InputStream): OPDSXMLParserType {
    val reader =
      this.parsers.createXMLReader(
        baseDirectory = null,
        xinclude = XInclude.XINCLUDE_DISABLED,
        schemaMappings = this.mappings)

    return Parser(uri, inputStream, reader)
  }

  companion object {

    /**
     * Create a new parser provider using the given list of formats.
     */

    fun create(formats: List<SPIFormatVersionedHandlerProviderType<OPDSManifest>>): OPDSXMLParserProviderType =
      OPDSXMLParsers(formats)

    /**
     * Create a new parser provider using [java.util.ServiceLoader] to find formats.
     */

    fun createFromServiceLoader(): OPDSXMLParserProviderType {
      val providers =
        ServiceLoader.load(SPIFormatVersionedHandlerProviderType::class.java)
          .filter { provider -> provider.contentClass == OPDSManifest::class.java }
          .map { provider -> provider as (SPIFormatVersionedHandlerProviderType<OPDSManifest>) }

      return create(providers)
    }
  }
}