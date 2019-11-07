package one.lfa.updater.credentials.api

import org.slf4j.LoggerFactory
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.InputStream
import java.net.URI
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Functions to load bundled credentials.
 */

object BundledCredentials {

  private val logger =
    LoggerFactory.getLogger(BundledCredentials::class.java)

  /**
   * Parse credentials from the given stream.
   */

  fun parse(
    uri: URI,
    stream: InputStream
  ): List<Credential> {
    return try {
      this.logger.debug("loading bundled credentials")

      val source = InputSource(stream)
      source.publicId = uri.toString()

      val documentBuilders = DocumentBuilderFactory.newInstance()
      documentBuilders.isNamespaceAware = true
      documentBuilders.isExpandEntityReferences = false
      documentBuilders.isValidating = false
      documentBuilders.isIgnoringComments = true
      documentBuilders.isIgnoringElementContentWhitespace = true

      val documentBuilder =
        documentBuilders.newDocumentBuilder()
      val document =
        documentBuilder.parse(source)
      val rootElement =
        document.documentElement
      val credentialElements =
        rootElement.getElementsByTagName("Credential")

      val credentials = mutableListOf<Credential>()

      for (index in 0 until credentialElements.length) {
        val credentialElement =
          credentialElements.item(index) as Element
        val uriPrefix =
          credentialElement.getAttribute("uriPrefix")
        val userName =
          credentialElement.getAttribute("userName")
        val password =
          credentialElement.getAttribute("password")

        if (uriPrefix != null && userName != null && password != null) {
          credentials.add(Credential(
            uriPrefix = uriPrefix,
            userName = userName,
            password = password
          ))
        }
      }

      this.logger.debug("loaded {} bundled credential values", credentials.size)
      for (credential in credentials) {
        this.logger.debug(
          "loaded user {} for URI prefix {}",
          credential.userName,
          credential.uriPrefix
        )
      }

      credentials.toList()
    } catch (e: Exception) {
      this.logger.error("could not load bundled credentials: ", e)
      listOf()
    }
  }
}
