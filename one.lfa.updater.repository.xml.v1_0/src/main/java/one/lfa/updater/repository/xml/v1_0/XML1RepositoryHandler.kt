package one.lfa.updater.repository.xml.v1_0

import one.lfa.updater.repository.api.Repository
import one.lfa.updater.repository.api.RepositoryItem
import one.lfa.updater.xml.spi.SPIFormatXMLAbstractContentHandler
import one.lfa.updater.xml.spi.SPIFormatXMLContentHandlerType
import org.joda.time.LocalDateTime
import org.joda.time.format.ISODateTimeFormat
import org.xml.sax.Attributes
import org.xml.sax.SAXParseException
import org.xml.sax.ext.Locator2
import java.net.URI
import java.util.UUID

class XML1RepositoryHandler(locator2: Locator2)
  : SPIFormatXMLAbstractContentHandler<RepositoryItem, Repository>(locator2, "repository") {

  private lateinit var items: MutableList<RepositoryItem>
  private lateinit var updated: LocalDateTime
  private lateinit var title: String
  private lateinit var id: UUID
  private lateinit var self: URI

  override fun onWantHandlerName(): String =
    XML1RepositoryHandler::class.java.simpleName

  override fun onWantChildHandlers(): Map<String, () -> SPIFormatXMLContentHandlerType<RepositoryItem>> =
    mapOf(Pair("package", { XML1RepositoryPackageHandler(super.locator(), this.self) }))

  override fun onElementFinishDirectly(
    namespace: String,
    name: String,
    qname: String): Repository? =
    Repository(
      id = this.id,
      title = this.title,
      updated = this.updated,
      items = this.items.toList(),
      self = this.self)

  override fun onElementStartDirectly(
    namespace: String,
    name: String,
    qname: String,
    attributes: Attributes) {

    val formatter = ISODateTimeFormat.dateTimeParser()

    try {
      this.id = UUID.fromString(attributes.getValue("id"))
      this.updated = formatter.parseLocalDateTime(attributes.getValue("updated"))
      this.title = attributes.getValue("title")
      this.self = URI(attributes.getValue("self"))
      this.items = mutableListOf()
    } catch (e: Exception) {
      throw SAXParseException(e.message, this.locator(), e)
    }
  }

  override fun onChildResultReceived(value: RepositoryItem) {
    this.items.add(value)
  }

}