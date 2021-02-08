package one.lfa.updater.repository.xml.v2_0

import one.lfa.updater.repository.api.Hash
import one.lfa.updater.repository.api.RepositoryItem
import one.lfa.updater.xml.spi.SPIFormatXMLAbstractContentHandler
import one.lfa.updater.xml.spi.SPIFormatXMLContentHandlerType
import org.slf4j.LoggerFactory
import org.xml.sax.Attributes
import org.xml.sax.SAXParseException
import org.xml.sax.ext.Locator2
import java.net.URI

class XML2RepositoryAndroidPackageHandler(
  locator2: Locator2,
  private val baseURI: URI)
  : SPIFormatXMLAbstractContentHandler<Unit, RepositoryItem>(locator2, "AndroidPackage") {

  private val logger =
    LoggerFactory.getLogger(XML2RepositoryAndroidPackageHandler::class.java)

  private lateinit var source: URI
  private lateinit var sha256: Hash
  private lateinit var name: String
  private lateinit var versionName: String
  private lateinit var id: String
  private var versionCode: Long = 0L

  override fun onWantHandlerName(): String =
    XML2RepositoryAndroidPackageHandler::class.java.simpleName

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
      source = this.source,
      installPasswordSha256 = null
    )
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

      this.logger.debug("resolved android package source: {}", resolvedSource)
      this.source = resolvedSource
    } catch (e: Exception) {
      throw SAXParseException(e.message, this.locator(), e)
    }
  }

  override fun onChildResultReceived(value: Unit) {

  }
}