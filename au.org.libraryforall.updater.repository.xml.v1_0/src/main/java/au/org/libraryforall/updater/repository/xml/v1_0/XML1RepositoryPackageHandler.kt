package au.org.libraryforall.updater.repository.xml.v1_0

import au.org.libraryforall.updater.repository.api.Hash
import au.org.libraryforall.updater.repository.api.RepositoryItem
import au.org.libraryforall.updater.repository.xml.spi.SPIFormatXMLAbstractContentHandler
import au.org.libraryforall.updater.repository.xml.spi.SPIFormatXMLContentHandlerType
import org.slf4j.LoggerFactory
import org.xml.sax.Attributes
import org.xml.sax.SAXParseException
import org.xml.sax.ext.Locator2
import java.net.URI

class XML1RepositoryPackageHandler(
  locator2: Locator2,
  private val baseURI: URI)
  : SPIFormatXMLAbstractContentHandler<Unit, RepositoryItem>(locator2, "package") {

  private val logger =
    LoggerFactory.getLogger(XML1RepositoryPackageHandler::class.java)

  private lateinit var source: URI
  private lateinit var sha256: Hash
  private lateinit var name: String
  private lateinit var versionName: String
  private lateinit var id: String
  private var versionCode: Long = 0L

  override fun onWantHandlerName(): String =
    XML1RepositoryPackageHandler::class.java.simpleName

  override fun onWantChildHandlers(): Map<String, () -> SPIFormatXMLContentHandlerType<Unit>> =
    mapOf()

  override fun onElementFinishDirectly(
    namespace: String,
    name: String,
    qname: String): RepositoryItem? {
    return RepositoryItem.RepositoryAndroidPackage(
      id = this.id,
      versionName = this.versionName,
      versionCode = this.versionCode,
      name = this.name,
      sha256 = this.sha256,
      source = this.source)
  }

  override fun onElementStartDirectly(
    namespace: String,
    name: String,
    qname: String,
    attributes: Attributes) {
    try {
      this.id =
        attributes.getValue("id")
      this.versionCode =
        attributes.getValue("versionCode").toLong()
      this.versionName =
        attributes.getValue("versionName")
      this.name =
        attributes.getValue("name")
      this.sha256 =
        Hash(attributes.getValue("sha256"))

      val relativeSource =
        URI.create(attributes.getValue("source"))

      val resolvedSource =
        if (!relativeSource.isAbsolute) {
          this.baseURI.resolve(relativeSource)
        } else {
          relativeSource
        }

      this.logger.debug("resolved package source: {}", resolvedSource)
      this.source = resolvedSource
    } catch (e: Exception) {
      throw SAXParseException(e.message, this.locator(), e)
    }
  }

  override fun onChildResultReceived(value: Unit) {

  }
}