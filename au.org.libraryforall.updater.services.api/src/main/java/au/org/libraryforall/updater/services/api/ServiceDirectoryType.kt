package au.org.libraryforall.updater.services.api

interface ServiceDirectoryType {

  /**
   * Retrieve a mandatory reference to the service implementing the given class. If multiple
   * services are available, the one that was registered first is picked.
   *
   * @throws ServiceConfigurationException If no service is available implementing the given class
   */

  @Throws(ServiceConfigurationException::class)
  fun <T : Any> requireService(
    serviceClass: Class<T>): T =
    this.requireServices(serviceClass)[0]

  /**
   * Retrieve a list of services implementing the given class. The list is required to be
   * non-empty.
   *
   * @throws ServiceConfigurationException If no service is available implementing the given class
   */

  @Throws(ServiceConfigurationException::class)
  fun <T : Any> requireServices(
    serviceClass: Class<T>): List<T> {
    val services = this.optionalServices(serviceClass)
    if (services.isEmpty()) {
      throw ServiceConfigurationException(buildString {
        this.append("No service implementation is available\n")
        this.append("  Service: ${serviceClass.canonicalName}\n")
      })
    }
    return services
  }

  /**
   * Retrieve an optional reference to the service implementing the given class. If no service
   * is available, the function returns `null`. If multiple
   * services are available, the one that was registered first is picked.
   *
   * @throws ServiceConfigurationException If a circular dependency is detected
   */

  @Throws(ServiceConfigurationException::class)
  fun <T : Any> optionalService(
    serviceClass: Class<T>): T? =
    this.optionalServices(serviceClass).firstOrNull()

  /**
   * Retrieve a list of services implementing the given class. The list may be empty.
   *
   * @throws ServiceConfigurationException If a circular dependency is detected
   */

  @Throws(ServiceConfigurationException::class)
  fun <T : Any> optionalServices(
    serviceClass: Class<T>): List<T>

}