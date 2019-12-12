package one.lfa.updater.opds.xml.v1_0

import one.lfa.updater.opds.api.OPDSFile
import one.lfa.updater.opds.api.OPDSManifest
import one.lfa.updater.xml.spi.SPIFormatXMLAbstractContentHandler
import one.lfa.updater.xml.spi.SPIFormatXMLContentHandlerType
import org.joda.time.DateTime
import org.joda.time.LocalDateTime
import org.joda.time.format.ISODateTimeFormat
import org.xml.sax.Attributes
import org.xml.sax.SAXParseException
import org.xml.sax.ext.Locator2
import java.net.URI
import java.util.UUID

class OPDSManifestXML1ManifestHandler(
  private val baseURIDefault: URI,
  locator2: Locator2)
  : SPIFormatXMLAbstractContentHandler<OPDSFile, OPDSManifest>(locator2, "Manifest") {

  private lateinit var id: UUID
  private lateinit var items: MutableList<OPDSFile>
  private lateinit var rootFile: URI
  private lateinit var title: String
  private lateinit var updated: DateTime
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
    qname: String
  ): OPDSManifest? {
    return OPDSManifest(
      baseURI = this.baseURI,
      rootFile = this.rootFile,
      updated = this.updated,
      searchIndex = this.searchIndex,
      id = this.id,
      files = this.items.toList(),
      title = this.title
    )
  }

  override fun onElementStartDirectly(
    namespace: String,
    name: String,
    qname: String,
    attributes: Attributes
  ) {

    val formatter = ISODateTimeFormat.dateTimeParser()

    try {
      this.id = UUID.fromString(attributes.getValue("id"))
      this.updated = formatter.parseDateTime(attributes.getValue("updated"))

      val titleOpt = attributes.getValue("title")
      if (titleOpt != null) {
        this.title = titleOpt
      } else {
        this.title = ""
      }

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