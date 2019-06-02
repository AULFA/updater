package au.org.libraryforall.updater.repository.xml.v1_0

import au.org.libraryforall.updater.repository.api.Repository
import au.org.libraryforall.updater.repository.api.RepositoryPackage
import au.org.libraryforall.updater.repository.xml.spi.SPIFormatXMLAbstractContentHandler
import au.org.libraryforall.updater.repository.xml.spi.SPIFormatXMLContentHandlerType
import org.joda.time.LocalDateTime
import org.joda.time.format.ISODateTimeFormat
import org.xml.sax.Attributes
import org.xml.sax.SAXParseException
import org.xml.sax.ext.Locator2
import java.net.URI
import java.util.UUID

class XML1RepositoryHandler(locator2: Locator2)
  : SPIFormatXMLAbstractContentHandler<RepositoryPackage, Repository>(locator2, "repository") {

  private lateinit var packages: MutableList<RepositoryPackage>
  private lateinit var updated: LocalDateTime
  private lateinit var title: String
  private lateinit var id: UUID
  private lateinit var self: URI

  override fun onWantHandlerName(): String =
    XML1RepositoryHandler::class.java.simpleName

  override fun onWantChildHandlers(): Map<String, () -> SPIFormatXMLContentHandlerType<RepositoryPackage>> =
    mapOf(Pair("package", { XML1RepositoryPackageHandler(super.locator(), this.self) }))

  override fun onElementFinishDirectly(
    namespace: String,
    name: String,
    qname: String): Repository? =
    Repository(
      id = this.id,
      title = this.title,
      updated = this.updated,
      packages = this.packages.toList(),
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
      this.packages = mutableListOf()
    } catch (e: Exception) {
      throw SAXParseException(e.message, this.locator(), e)
    }
  }

  override fun onChildResultReceived(value: RepositoryPackage) {
    this.packages.add(value)
  }

}