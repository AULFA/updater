package one.lfa.updater.repository.xml.v3_0

import one.lfa.updater.repository.api.Hash
import one.lfa.updater.repository.api.RepositoryItem
import one.lfa.updater.xml.spi.SPIFormatXMLAbstractContentHandler
import one.lfa.updater.xml.spi.SPIFormatXMLContentHandlerType
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import org.xml.sax.Attributes
import org.xml.sax.SAXParseException
import org.xml.sax.ext.Locator2
import java.net.URI

class XML3RepositoryOPDSPackageHandler(
  locator2: Locator2,
  private val baseURI: URI
): SPIFormatXMLAbstractContentHandler<Unit, RepositoryItem>(locator2, "OPDSPackage") {

  private val logger =
    LoggerFactory.getLogger(XML3RepositoryOPDSPackageHandler::class.java)

  private lateinit var source: URI
  private lateinit var sha256: Hash
  private lateinit var name: String
  private lateinit var versionName: String
  private lateinit var id: String
  private var versionCode: Long = 0L
  private var installPasswordSha256: Hash? = null

  override fun onWantHandlerName(): String =
    XML3RepositoryOPDSPackageHandler::class.java.simpleName

  override fun onWantChildHandlers(): Map<String, () -> SPIFormatXMLContentHandlerType<Unit>> =
    mapOf()

  override fun onElementFinishDirectly(
    namespace: String,
    name: String,
    qname: String
  ): RepositoryItem? {
    return RepositoryItem.RepositoryOPDSPackage(
      id = this.id,
      versionName = this.versionName,
      versionCode = this.versionCode,
      name = this.name,
      sha256 = this.sha256,
      source = this.source,
      installPasswordSha256 = this.installPasswordSha256
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
        DateTime.parse(attributes.getValue("versionName")).toString()
      this.name =
        attributes.getValue("name")
      this.sha256 =
        Hash(attributes.getValue("sha256"))

      this.installPasswordSha256 =
        if (attributes.getValue("installPasswordSha256") != null) {
          Hash(attributes.getValue("installPasswordSha256"))
        } else {
          null
        }

      val relativeSource =
        URI.create(attributes.getValue("source"))

      val resolvedSource =
        if (!relativeSource.isAbsolute) {
          this.baseURI.resolve(relativeSource)
        } else {
          relativeSource
        }

      this.logger.debug("resolved OPDS package source: {}", resolvedSource)
      this.source = resolvedSource
    } catch (e: Exception) {
      throw SAXParseException(e.message, this.locator(), e)
    }
  }

  override fun onChildResultReceived(value: Unit) {

  }
}