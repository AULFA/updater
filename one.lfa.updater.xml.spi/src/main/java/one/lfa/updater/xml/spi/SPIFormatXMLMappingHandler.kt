package one.lfa.updater.xml.spi

import org.xml.sax.Attributes
import org.xml.sax.SAXException

/**
 * A content handler that simply applies a function to the results of another handler.
 *
 * @param <A> The type of source values
 * @param <B> The type of target values
 */

class SPIFormatXMLMappingHandler<A, B>(
  private val handler: SPIFormatXMLContentHandlerType<A>,
  private val function: (A) -> B) : SPIFormatXMLContentHandlerType<B> {

  override fun toString(): String {
    return StringBuilder(128)
      .append("[map ")
      .append(this.handler)
      .append(']')
      .toString()
  }

  override fun <C> map(f: (B) -> C): SPIFormatXMLContentHandlerType<C> {
    return SPIFormatXMLMappingHandler(this, f)
  }

  @Throws(SAXException::class)
  override fun onElementStarted(
    namespace: String,
    name: String,
    qname: String,
    attributes: Attributes) {
    this.handler.onElementStarted(namespace, name, qname, attributes)
  }

  @Throws(SAXException::class)
  override fun onElementFinished(
    namespace: String,
    name: String,
    qname: String): B? {

    return this.handler.onElementFinished(namespace, name, qname)?.let(this.function::invoke)
  }

  @Throws(SAXException::class)
  override fun onCharacters(
    ch: CharArray,
    start: Int,
    length: Int) {
    this.handler.onCharacters(ch, start, length)
  }

  override fun get(): B {
    return this.handler.get()!!.let(this.function::invoke)
  }
}
