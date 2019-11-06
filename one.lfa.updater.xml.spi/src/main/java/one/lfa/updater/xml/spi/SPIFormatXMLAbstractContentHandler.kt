package one.lfa.updater.xml.spi

import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.SAXParseException
import org.xml.sax.ext.Locator2
import java.util.Objects

/**
 * An abstract implementation of the content handler interface.
 *
 * @param <A> The type of values returned by child handlers
 * @param <B> The type of result values
 */

abstract class SPIFormatXMLAbstractContentHandler<A, B> protected constructor(
  private val locator: Locator2,
  private val direct: String?) : SPIFormatXMLContentHandlerType<B> {

  private var handler: SPIFormatXMLContentHandlerType<A>? = null
  private var result: B? = null

  override fun <C> map(f: (B) -> C): SPIFormatXMLContentHandlerType<C> {
    return SPIFormatXMLMappingHandler(this, f)
  }

  override fun toString(): String {
    return StringBuilder(128)
      .append("[")
      .append(this.onWantHandlerName())
      .append(" ")
      .append(this.direct)
      .append(" [")
      .append(this.onWantChildHandlers().keys.joinToString("|"))
      .append("]]")
      .toString()
  }

  @Throws(SAXException::class)
  override fun onElementStarted(
    namespace: String,
    name: String,
    qname: String,
    attributes: Attributes) {

    val current = this.handler
    if (current != null) {
      current.onElementStarted(namespace, name, qname, attributes)
      return
    }

    val handlerSupplier = this.onWantChildHandlers()[name]
    if (handlerSupplier != null) {
      val newHandler = handlerSupplier.invoke()
      this.handler = newHandler
      newHandler.onElementStarted(namespace, name, qname, attributes)
      return
    }

    val directName = this.direct
    if (directName != null) {
      if (directName == name) {
        this.onElementStartDirectly(namespace, name, qname, attributes)
        return
      }
    }

    val sb = StringBuilder(128)
    sb.append("This handler does not recognize this element")
    sb.append(System.lineSeparator())
    sb.append("  Received: ")
    sb.append(name)
    sb.append(System.lineSeparator())
    sb.append("  Expected: One of ")
    sb.append(this.onWantChildHandlers().keys.joinToString("|"))
    sb.append(System.lineSeparator())

    if (directName != null) {
      sb.append("  Expected: ")
      sb.append(directName)
      sb.append(System.lineSeparator())
    }

    throw SAXParseException(sb.toString(), this.locator)
  }

  private fun finishChildHandlerIfNecessary(childValue: A?) {
    if (childValue != null) {
      this.onChildResultReceived(childValue)
      this.handler = null
    }
  }

  protected fun locator(): Locator2 {
    return this.locator
  }

  protected abstract fun onWantHandlerName(): String

  protected abstract fun onWantChildHandlers(): Map<String, () -> SPIFormatXMLContentHandlerType<A>>

  @Throws(SAXException::class)
  protected abstract fun onElementFinishDirectly(
    namespace: String,
    name: String,
    qname: String): B?

  @Throws(SAXException::class)
  protected abstract fun onElementStartDirectly(
    namespace: String,
    name: String,
    qname: String,
    attributes: Attributes)

  /**
   * A value was received from a child handler.
   *
   * @param value The result value
   */

  protected abstract fun onChildResultReceived(value: A)

  @Throws(SAXException::class)
  override fun onElementFinished(
    namespace: String,
    name: String,
    qname: String): B? {

    val current = this.handler
    if (current != null) {
      val subResult = current.onElementFinished(namespace, name, qname)
      this.finishChildHandlerIfNecessary(subResult)
      return null
    }

    val directName = this.direct
    if (directName != null) {
      if (directName == name) {
        val resultOpt = this.onElementFinishDirectly(namespace, name, qname)
        if (resultOpt != null) {
          this.finish(resultOpt)
        }
        return resultOpt
      }
    }

    throw java.lang.IllegalStateException("Unreachable code")
  }

  protected fun finish(r: B) {
    this.result = Objects.requireNonNull(r, "r")
  }

  @Throws(SAXException::class)
  override fun onCharacters(
    ch: CharArray,
    start: Int,
    length: Int) {

    val current = this.handler
    if (current != null) {
      current.onCharacters(ch, start, length)
      return
    }
  }

  override fun get(): B {
    if (this.result == null) {
      throw IllegalStateException("Handler has not completed")
    }
    return this.result!!
  }
}
