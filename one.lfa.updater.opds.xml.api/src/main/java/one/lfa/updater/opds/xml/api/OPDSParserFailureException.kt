package one.lfa.updater.opds.xml.api

/**
 * Parsing failed.
 */

class OPDSParserFailureException(
  message: String,
  cause: Exception? = null
) : Exception(message, cause)
