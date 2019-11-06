package au.org.libraryforall.updater.repository.xml.spi

import one.lfa.updater.repository.api.Repository
import java.io.Closeable

interface SPIFormatXMLSerializerType : Closeable {

  fun serialize(repository: Repository)

}