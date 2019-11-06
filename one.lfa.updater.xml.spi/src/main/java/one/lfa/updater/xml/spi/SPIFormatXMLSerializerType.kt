package one.lfa.updater.xml.spi

import java.io.Closeable

interface SPIFormatXMLSerializerType<T> : Closeable {

  val contentClass: Class<T>

  fun serialize(value: T)

}