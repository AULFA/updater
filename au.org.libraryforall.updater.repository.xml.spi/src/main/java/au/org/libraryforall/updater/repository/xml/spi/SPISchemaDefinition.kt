package au.org.libraryforall.updater.repository.xml.spi

import java.net.URI
import java.net.URL

data class SPISchemaDefinition(
  val uri: URI,
  val fileIdentifier: String,
  val location: URL)
