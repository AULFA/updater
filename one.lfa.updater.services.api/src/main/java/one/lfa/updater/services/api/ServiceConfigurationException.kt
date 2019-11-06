package one.lfa.updater.services.api

/**
 * An exception relating to a service.
 */

class ServiceConfigurationException(
  override val message: String,
  override val cause: Exception? = null)
  : RuntimeException(message, cause)
