package au.org.libraryforall.updater.tests

import au.org.libraryforall.updater.services.api.ServiceDirectoryType
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class MutableServiceDirectory : ServiceDirectoryType {

  private val logger = LoggerFactory.getLogger(MutableServiceDirectory::class.java)
  private val services = ConcurrentHashMap<Class<*>, List<Any>>()

  fun <T : Any> registerService(
    serviceClass: Class<T>,
    service: T
  ) {
    this.logger.debug("registering service {}: {}", serviceClass.canonicalName, service)
    val existing = (this.services[serviceClass] ?: listOf()).plus(service)
    this.services[serviceClass] = existing
  }

  fun <T : Any> registerAll(
    service: T,
    vararg serviceClasses: Class<in T>
  ) {
    for (serviceClass in serviceClasses) {
      this.registerService(serviceClass as Class<T>, service)
    }
  }

  override fun <T : Any> optionalServices(serviceClass: Class<T>): List<T> {
    return (this.services[serviceClass] as List<T>?) ?: listOf()
  }
}
