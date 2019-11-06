package one.lfa.updater.opds.xml.v1_0

import one.lfa.updater.opds.api.OPDSFile
import one.lfa.updater.xml.spi.SPIFormatXMLAbstractContentHandler
import one.lfa.updater.xml.spi.SPIFormatXMLContentHandlerType
import org.slf4j.LoggerFactory
import org.xml.sax.Attributes
import org.xml.sax.SAXParseException
import org.xml.sax.ext.Locator2
import java.net.URI

class OPDSManifestXML1FileHandler(
  locator2: Locator2,
  private val baseURI: URI
): SPIFormatXMLAbstractContentHandler<Unit, OPDSFile>(locator2, "File") {

  private lateinit var hash: String
  private lateinit var hashAlgorithm: String
  private lateinit var name: URI

  private val logger =
    LoggerFactory.getLogger(OPDSManifestXML1FileHandler::class.java)


  override fun onWantHandlerName(): String =
    OPDSManifestXML1FileHandler::class.java.simpleName

  override fun onWantChildHandlers(): Map<String, () -> SPIFormatXMLContentHandlerType<Unit>> =
    mapOf()

  override fun onElementFinishDirectly(
    namespace: String,
    name: String,
    qname: String
  ): OPDSFile? {
    return OPDSFile(
      file = this.name,
      hash = this.hash,
      hashAlgorithm = this.hashAlgorithm
    )
  }

  override fun onElementStartDirectly(
    namespace: String,
    name: String,
    qname: String,
    attributes: Attributes) {
    try {
      this.name =
        URI.create(attributes.getValue("name"))
      this.hashAlgorithm =
        attributes.getValue("hashAlgorithm")
      this.hash =
        attributes.getValue("hash")
    } catch (e: Exception) {
      throw SAXParseException(e.message, this.locator(), e)
    }
  }

  override fun onChildResultReceived(value: Unit) {

  }
}