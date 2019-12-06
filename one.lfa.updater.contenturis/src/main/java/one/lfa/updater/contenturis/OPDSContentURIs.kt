package one.lfa.updater.contenturis

import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.regex.Pattern

/**
 * Functions to parse content URIs.
 */

object OPDSContentURIs {

  private val logger =
    LoggerFactory.getLogger(OPDSContentURIs::class.java)

  private val leadingSlashes : Regex =
    Pattern.compile("^/+").toRegex()

  /**
   * Parse a content URI of the form: `/${UUID}/${FILE}`
   */

  fun parseContentURI(
    input: String
  ): OPDSContentURI? {

    val removeLeading =
      input.replace(this.leadingSlashes, "")

    val firstSlash =
      removeLeading.indexOf('/')

    if (firstSlash == -1) {
      this.logger.error("input path does not contain a slash")
      return null
    }

    val before =
      removeLeading.substring(0, firstSlash)
    val rest =
      removeLeading.substring(firstSlash)

    return try {
      OPDSContentURI(
        catalogId = UUID.fromString(before),
        path = rest.replace(this.leadingSlashes, "")
      )
    } catch (e: Exception) {
      this.logger.error("could not parse: ", e)
      null
    }
  }
}
