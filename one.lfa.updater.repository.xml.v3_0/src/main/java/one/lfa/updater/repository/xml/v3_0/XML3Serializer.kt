package one.lfa.updater.repository.xml.v3_0

import one.lfa.updater.repository.api.Repository
import one.lfa.updater.repository.api.RepositoryItem
import one.lfa.updater.xml.spi.SPIFormatXMLSerializerType
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.OutputStream
import java.util.Properties
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class XML3Serializer(
  private val outputStream: OutputStream
) : SPIFormatXMLSerializerType<Repository> {

  override val contentClass: Class<Repository>
    get() = Repository::class.java

  private val documentBuilders: DocumentBuilderFactory =
    DocumentBuilderFactory.newInstance()

  private fun newDocument(): Document {
    val builder = this.documentBuilders.newDocumentBuilder();
    val document = builder.newDocument();
    document.strictErrorChecking = true;
    document.xmlStandalone = true;
    return document;
  }

  override fun serialize(repository: Repository) {
    val document = newDocument()
    val root = ofRepository(document, repository)
    document.appendChild(root)

    val factory = TransformerFactory.newInstance()
    val transformer = factory.newTransformer()
    val outFormat = Properties()
    outFormat.setProperty(OutputKeys.INDENT, "yes")
    outFormat.setProperty(OutputKeys.METHOD, "xml")
    outFormat.setProperty(OutputKeys.OMIT_XML_DECLARATION, "no")
    outFormat.setProperty(OutputKeys.VERSION, "1.0")
    outFormat.setProperty(OutputKeys.ENCODING, "UTF-8")
    transformer.outputProperties = outFormat

    val domSource = DOMSource(document)
    val result = StreamResult(this.outputStream)
    transformer.transform(domSource, result)

    this.outputStream.flush()
  }

  private fun ofRepository(document: Document, repository: Repository): Element {
    val root =
      document.createElementNS(RepositoryXML3Format.NAMESPACE.toString(), "r:Repository");

    root.setAttribute("id", repository.id.toString())
    root.setAttribute("updated", repository.updated.toString())
    root.setAttribute("title", repository.title)
    root.setAttribute("self", repository.self.toString())

    for (pack in repository.items) {
      when (pack) {
        is RepositoryItem.RepositoryAndroidPackage -> {
          root.appendChild(ofAndroidPackage(document, pack))
        }
        is RepositoryItem.RepositoryOPDSPackage -> {
          root.appendChild(ofOPDSPackage(document, pack))
        }
      }
    }

    return root
  }

  private fun ofAndroidPackage(
    document: Document,
    pack: RepositoryItem.RepositoryAndroidPackage
  ): Element {
    val root =
      document.createElementNS(RepositoryXML3Format.NAMESPACE.toString(), "r:AndroidPackage");

    root.setAttribute("id", pack.id)
    root.setAttribute("name", pack.name)
    root.setAttribute("versionName", pack.versionName)
    root.setAttribute("versionCode", pack.versionCode.toString())
    root.setAttribute("sha256", pack.sha256.text)
    root.setAttribute("source",pack.source.toString())

    val passHash = pack.installPasswordSha256
    if (passHash != null) {
      root.setAttribute("installPasswordSha256", passHash.text)
    }

    return root
  }

  private fun ofOPDSPackage(
    document: Document,
    pack: RepositoryItem.RepositoryOPDSPackage
  ): Element {
    val root =
      document.createElementNS(RepositoryXML3Format.NAMESPACE.toString(), "r:OPDSPackage");

    root.setAttribute("id", pack.id)
    root.setAttribute("name", pack.name)
    root.setAttribute("versionName", pack.versionName)
    root.setAttribute("versionCode", pack.versionCode.toString())
    root.setAttribute("sha256", pack.sha256.text)
    root.setAttribute("source",pack.source.toString())

    val passHash = pack.installPasswordSha256
    if (passHash != null) {
      root.setAttribute("installPasswordSha256", passHash.text)
    }

    return root
  }

  override fun close() {
    this.outputStream.close()
  }
}