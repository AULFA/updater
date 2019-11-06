package au.org.libraryforall.updater.repository.xml.api

/**
 * Parsing failed.
 */

class RepositoryParserFailureException(
  message: String,
  cause: Exception? = null
) : Exception(message, cause)
