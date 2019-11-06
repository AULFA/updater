package au.org.libraryforall.updater.repository.xml.spi

/**
 * A specification of whether or not XInclude should be enabled for parsers.
 */

enum class XInclude {

  /**
   * XInclude is enabled.
   */

  XINCLUDE_ENABLED,

  /**
   * XInclude is not enabled.
   */

  XINCLUDE_DISABLED
}