package one.lfa.updater.xml.spi

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