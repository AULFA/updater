package one.lfa.updater.opds.xml.v1_0

import one.lfa.updater.opds.api.OPDSFile
import one.lfa.updater.opds.api.OPDSManifest
import one.lfa.updater.xml.spi.SPIFormatXMLAbstractContentHandler
import one.lfa.updater.xml.spi.SPIFormatXMLContentHandlerType
import org.joda.time.LocalDateTime
import org.joda.time.format.ISODateTimeFormat
import org.xml.sax.Attributes
import org.xml.sax.SAXParseException
import org.xml.sax.ext.Locator2
import java.net.URI

class OPDSManifestXML1ManifestHandler(
  private val baseURIDefault: URI,
  locator2: Locator2)
  : SPIFormatXMLAbstractContentHandler<OPDSFile, OPDSManifest>(locator2, "Manifest") {

  private lateinit var id: URI
  private lateinit var items: MutableList<OPDSFile>
  private lateinit var rootFile: URI
  private lateinit var updated: LocalDateTime
  private var baseURI: URI? = null
  private var searchIndex: URI? = null

  override fun onWantHandlerName(): String =
    OPDSManifestXML1ManifestHandler::class.java.simpleName

  override fun onWantChildHandlers(): Map<String, () -> SPIFormatXMLContentHandlerType<OPDSFile>> =
    mapOf(Pair("File", { OPDSManifestXML1FileHandler(super.locator(), this.baseURIOrDefault()) }))

  private fun baseURIOrDefault(): URI {
    return this.baseURI ?: this.baseURIDefault
  }

  override fun onElementFinishDirectly(
    namespace: String,
    name: String,
    qname: String): OPDSManifest? {
    return OPDSManifest(
      baseURI = this.baseURI,
      rootFile = this.rootFile,
      updated = this.updated,
      searchIndex = this.searchIndex,
      feedURI = this.id,
      files = this.items.toList()
    )
  }

  override fun onElementStartDirectly(
    namespace: String,
    name: String,
    qname: String,
    attributes: Attributes) {

    val formatter = ISODateTimeFormat.dateTimeParser()

    try {
      this.id = URI.create(attributes.getValue("id"))
      this.updated = formatter.parseLocalDateTime(attributes.getValue("updated"))

      val baseOpt = attributes.getValue("base")
      if (baseOpt != null) {
        this.baseURI = URI.create(baseOpt)
      }

      val searchOpt = attributes.getValue("searchIndex")
      if (searchOpt != null) {
        this.searchIndex = URI.create(searchOpt)
      }

      this.rootFile = URI.create(attributes.getValue("rootFile"))
      this.items = mutableListOf()
    } catch (e: Exception) {
      throw SAXParseException(e.message, this.locator(), e)
    }
  }

  override fun onChildResultReceived(value: OPDSFile) {
    this.items.add(value)
  }
}