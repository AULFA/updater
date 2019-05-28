package au.org.libraryforall.updater.repository.xml.spi

import java.net.URI
import java.net.URL

data class SPISchemaDefinition(
  val versionMajor: Int,
  val versionMinor: Int,
  val uri: URI,
  val fileIdentifier: String,
  val location: URL) : Comparable<SPISchemaDefinition> {

  override fun compareTo(other: SPISchemaDefinition): Int {
    val cMajor = this.versionMajor.compareTo(other.versionMajor)
    return if (cMajor == 0) {
      this.versionMinor.compareTo(other.versionMinor)
    } else {
      cMajor
    }
  }
}
