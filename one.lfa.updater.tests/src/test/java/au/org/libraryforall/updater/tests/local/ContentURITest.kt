package au.org.libraryforall.updater.tests.local

import org.junit.Test
import org.slf4j.LoggerFactory
import java.util.regex.Pattern

class ContentURITest {

  private val logger = LoggerFactory.getLogger(ContentURITest::class.java)

  @Test
  fun test()
  {
    val leadingSlashes =
      Pattern.compile("^/+").toRegex()

    val input = "/cc745b6f-b0b0-4b08-aa1e-2761dee2542a/feeds/0131EF0BAC13737EB4D63EDE9AA9FDB69F921EA0BE86737D9E762E6D491F1F27.atom"
    val removeLeading = input.replace(leadingSlashes, "")
    this.logger.debug("removeLeading: {}", removeLeading)

    val firstSlash =
      removeLeading.indexOf('/')
    val before =
      removeLeading.substring(0, firstSlash)
    val rest =
      removeLeading.substring(firstSlash)

    this.logger.debug("before: {}", before)
    this.logger.debug("rest:   {}", rest)
  }
}