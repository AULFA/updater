package one.lfa.updater.xml.spi

import org.xml.sax.Attributes
import org.xml.sax.SAXException

/**
 * A content handler that produces values of type {@code A}
 *
 * @param <A> The type of produced values
 */

interface SPIFormatXMLContentHandlerType<A> {

  /**
   * Apply `f` to the results of this content handler.
   *
   * @param f   The function
   * @param <B> The type of returned values
   *
   * @return A content handler that produces values of type `B`
   */

  fun <B> map(f: (A) -> B): SPIFormatXMLContentHandlerType<B>

  /**
   * An XML element has been started.
   *
   * @param namespace  The namespace URI
   * @param name       The local element name
   * @param qname      The fully qualified name
   * @param attributes The attributes
   *
   * @throws SAXException On errors
   */

  @Throws(SAXException::class)
  fun onElementStarted(
    namespace: String,
    name: String,
    qname: String,
    attributes: Attributes)

  /**
   * An XML element has finished.
   *
   * @param namespace The namespace URI
   * @param name      The local element name
   * @param qname     The fully qualified name
   *
   * @return A value of `A` if the given element finished the content
   *
   * @throws SAXException On errors
   */

  @Throws(SAXException::class)
  fun onElementFinished(
    namespace: String,
    name: String,
    qname: String): A?

  /**
   * Text was received.
   *
   * @param ch     The character buffer
   * @param start  The offset of the start of the data in the buffer
   * @param length The length of the data in the buffer
   *
   * @throws SAXException On errors
   */

  @Throws(SAXException::class)
  fun onCharacters(
    ch: CharArray,
    start: Int,
    length: Int)

  /**
   * @return The completed value
   */

  fun get(): A

}