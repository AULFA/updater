package au.org.libraryforall.updater.repository.xml.spi

import org.apache.xerces.jaxp.SAXParserFactoryImpl
import org.xml.sax.SAXException
import org.xml.sax.XMLReader
import java.io.File
import javax.xml.XMLConstants
import javax.xml.parsers.ParserConfigurationException
import javax.xml.parsers.SAXParserFactory

/**
 * A provider of hardened SAX parsers.
 */

class SPIHardenedSAXParsers {

  private val parsers: SAXParserFactory = SAXParserFactoryImpl()

  /**
   * Create a non-validating XML reader.
   *
   * @param baseDirectory A directory that will contain parsed resources, if any
   * @param xinclude       A specification of whether or not XInclude should be enabled
   *
   * @return A new non-validating XML reader
   *
   * @throws ParserConfigurationException On parser configuration errors
   * @throws SAXException                 On SAX parser errors
   */

  @Throws(ParserConfigurationException::class, SAXException::class)
  fun createXMLReaderNonValidating(
    baseDirectory: File?,
    xinclude: XInclude): XMLReader {

    val parser = this.parsers.newSAXParser()
    val reader = parser.xmlReader

    /*
     * Turn on "secure processing". Sets various resource limits to prevent
     * various denial of service attacks.
     */

    reader.setFeature(
      XMLConstants.FEATURE_SECURE_PROCESSING,
      true)

    /*
     * Don't load DTDs at all.
     */

    reader.setFeature(
      "http://apache.org/xml/features/nonvalidating/load-external-dtd",
      false)

    /*
     * Enable XInclude.
     */

    reader.setFeature(
      "http://apache.org/xml/features/xinclude",
      xinclude === XInclude.XINCLUDE_ENABLED)

    /*
     * Ensure namespace processing is enabled.
     */

    reader.setFeature(
      "http://xml.org/sax/features/namespaces",
      true)

    /*
     * Disable validation.
     */

    reader.setFeature(
      "http://xml.org/sax/features/validation",
      false)
    reader.setFeature(
      "http://apache.org/xml/features/validation/schema",
      false)

    /*
     * Tell the parser to use the full EntityResolver2 interface (by default,
     * the extra EntityResolver2 methods will not be called - only those of
     * the original EntityResolver interface would be called).
     */

    reader.setFeature(
      "http://xml.org/sax/features/use-entity-resolver2",
      true)

    reader.entityResolver =
      SPIHardenedDispatchingResolver.create(baseDirectory, SPISchemaResolutionMappings(mapOf()))
    return reader
  }

  /**
   * Create a XSD-validating XML reader.
   *
   * @param xinclude       A specification of whether or not XInclude should be enabled for parsers
   * @param baseDirectory A directory that will contain parsed resources
   * @param schemaMappings     A set of schemas that will be consulted for validation
   *
   * @return A new XSD-validating XML reader
   *
   * @throws ParserConfigurationException On parser configuration errors
   * @throws SAXException                 On SAX parser errors
   */

  @Throws(ParserConfigurationException::class, SAXException::class)
  fun createXMLReader(
    baseDirectory: File?,
    xinclude: XInclude,
    schemaMappings: SPISchemaResolutionMappings): XMLReader {

    val parser = this.parsers.newSAXParser()
    val reader = parser.xmlReader

    /*
     * Turn on "secure processing". Sets various resource limits to prevent
     * various denial of service attacks.
     */

    reader.setFeature(
      XMLConstants.FEATURE_SECURE_PROCESSING,
      true)

    /*
     * Don't load DTDs at all.
     */

    reader.setFeature(
      "http://apache.org/xml/features/nonvalidating/load-external-dtd",
      false)

    /*
     * Enable XInclude.
     */

    reader.setFeature(
      "http://apache.org/xml/features/xinclude",
      xinclude === XInclude.XINCLUDE_ENABLED)

    /*
     * Ensure namespace processing is enabled.
     */

    reader.setFeature(
      "http://xml.org/sax/features/namespaces",
      true)

    /*
     * Enable validation and, more to the point, enable XSD schema validation.
     */

    reader.setFeature(
      "http://xml.org/sax/features/validation",
      true)
    reader.setFeature(
      "http://apache.org/xml/features/validation/schema",
      true)

    /*
     * Create a space separated list of mappings from namespace URIs to
     * schema system IDs. This will indicate to the parser that when it encounters
     * a given namespace, it should ask the _entity resolver_ to resolve the
     * corresponding system ID specified here.
     */

    val locations = StringBuilder(128)
    schemaMappings.mappings.forEach { (uri, schema) ->
      locations.append(uri)
      locations.append(' ')
      locations.append(schema.fileIdentifier)
      locations.append(' ')
    }

    reader.setProperty(
      "http://apache.org/xml/properties/schema/external-schemaLocation",
      locations.toString())

    /*
     * Tell the parser to use the full EntityResolver2 interface (by default,
     * the extra EntityResolver2 methods will not be called - only those of
     * the original EntityResolver interface would be called).
     */

    reader.setFeature(
      "http://xml.org/sax/features/use-entity-resolver2",
      true)

    reader.entityResolver = SPIHardenedDispatchingResolver.create(baseDirectory, schemaMappings)
    return reader
  }
}
