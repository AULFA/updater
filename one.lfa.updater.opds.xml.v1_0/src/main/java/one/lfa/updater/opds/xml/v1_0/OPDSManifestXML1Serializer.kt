package one.lfa.updater.opds.xml.v1_0

import one.lfa.updater.opds.api.OPDSFile
import one.lfa.updater.opds.api.OPDSManifest
import one.lfa.updater.xml.spi.SPIFormatXMLSerializerType
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.IOException
import java.io.OutputStream
import java.util.Locale
import java.util.Properties
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class OPDSManifestXML1Serializer(
  private val outputStream: OutputStream
) : SPIFormatXMLSerializerType<OPDSManifest> {

  override val contentClass: Class<OPDSManifest>
    get() = OPDSManifest::class.java

  private val documentBuilders: DocumentBuilderFactory =
    DocumentBuilderFactory.newInstance()

  private fun newDocument(): Document {
    val builder = this.documentBuilders.newDocumentBuilder()
    val document = builder.newDocument()
    document.strictErrorChecking = true
    document.xmlStandalone = true
    return document
  }

  override fun serialize(value: OPDSManifest) {
    if (value.files.isEmpty()) {
      throw IOException("Refusing to serialize a manifest with no files.")
    }

    val document = newDocument()
    val root = ofManifest(document, value)
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

  private fun ofManifest(
    document: Document,
    manifest: OPDSManifest
  ): Element {
    val root =
      document.createElementNS(OPDSManifestXML1Format.NAMESPACE.toString(), "om:Manifest")

    root.setAttribute("id", manifest.id.toString())
    root.setAttribute("rootFile", manifest.rootFile.toString())
    root.setAttribute("updated", manifest.updated.toString())
    
    manifest.baseURI?.let { uri ->
      root.setAttribute("base", uri.toString())
    }
    manifest.searchIndex?.let { uri ->
      root.setAttribute("searchIndex", uri.toString())
    }

    for (file in manifest.files) {
      root.appendChild(ofFile(document, file))
    }
    return root
  }

  private fun ofFile(
    document: Document,
    file: OPDSFile
  ): Element {
    val root =
      document.createElementNS(OPDSManifestXML1Format.NAMESPACE.toString(), "om:File")
    root.setAttribute("name", file.file.toString())
    root.setAttribute("hashAlgorithm", file.hashAlgorithm)
    root.setAttribute("hash", file.hash.toUpperCase(Locale.ROOT))
    return root
  }

  override fun close() {
    this.outputStream.close()
  }
}