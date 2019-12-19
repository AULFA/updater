package one.lfa.updater.services.api

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import java.lang.IllegalStateException
import java.util.concurrent.TimeUnit

object Services : ServiceDirectoryProviderType {

  private val servicesLock : Any = Any()
  private var servicesDirectory: ServiceDirectoryType? = null
  private val servicesFuture: SettableFuture<ServiceDirectoryType> = SettableFuture.create()

  override fun serviceDirectory(): ServiceDirectoryType {
    return synchronized(servicesLock) {
      servicesDirectory ?: throw IllegalStateException("No service directory has been created!")
    }
  }

  fun serviceDirectoryFuture(): ListenableFuture<ServiceDirectoryType> =
    servicesFuture

  fun serviceDirectoryWaiting(
    time: Long,
    timeUnit: TimeUnit
  ): ServiceDirectoryType =
    servicesFuture.get(time, timeUnit)

  fun isInitialized(): Boolean {
    return synchronized(servicesLock) {
      servicesDirectory != null
    }
  }

  fun initialize(services: ServiceDirectoryType) {
    return synchronized(servicesLock) {
      check(servicesDirectory == null) {
        "Service directory has already been initialized!"
      }
      servicesDirectory = services
      servicesFuture.set(services)
    }
  }
}