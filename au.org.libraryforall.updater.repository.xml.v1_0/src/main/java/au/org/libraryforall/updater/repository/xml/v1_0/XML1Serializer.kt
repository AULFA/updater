package au.org.libraryforall.updater.repository.xml.v1_0

import au.org.libraryforall.updater.repository.api.Repository
import au.org.libraryforall.updater.repository.api.RepositoryPackage
import au.org.libraryforall.updater.repository.xml.spi.SPIFormatXMLSerializerType
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.OutputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class XML1Serializer(private val outputStream: OutputStream) : SPIFormatXMLSerializerType {

  companion object {
    private val XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>".toByteArray()
  }

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

    val transformers = TransformerFactory.newInstance();
    transformers.setAttribute("indent-number", Integer.valueOf(4));

    val transformer = transformers.newTransformer();
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

    this.outputStream.write(XML_DECLARATION);
    this.outputStream.write("\r\n".toByteArray());

    val source = DOMSource(document);
    val result = StreamResult(this.outputStream);
    transformer.transform(source, result);
  }

  private fun ofRepository(document: Document, repository: Repository): Element {
    val root =
      document.createElementNS(RepositoryXML1Format.NAMESPACE.toString(), "r:repository");

    root.setAttribute("id", repository.id.toString())
    root.setAttribute("updated", repository.updated.toString())
    root.setAttribute("title", repository.title)

    for (pack in repository.packages) {
      root.appendChild(ofPackage(document, pack))
    }

    return root
  }

  private fun ofPackage(document: Document, pack: RepositoryPackage): Element {
    val root =
      document.createElementNS(RepositoryXML1Format.NAMESPACE.toString(), "r:package");

    root.setAttribute("id", pack.id)
    root.setAttribute("name", pack.name)
    root.setAttribute("versionName", pack.versionName)
    root.setAttribute("versionCode", pack.versionCode.toString())
    root.setAttribute("sha256", pack.sha256.text)
    root.setAttribute("source",pack.source.toString())
    return root
  }

  override fun close() {
    this.outputStream.close()
  }
}