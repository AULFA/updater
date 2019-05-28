package au.org.libraryforall.updater.repository.xml.spi

import java.lang.Exception
import java.net.URI

data class ParseError(
  val line: Int,
  val column: Int,
  val file: URI,
  val message: String,
  val exception: Exception?,
  val severity: Severity) {

  enum class Severity {
    WARNING,
    ERROR
  }
}