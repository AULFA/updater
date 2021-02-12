package one.lfa.updater.services.api

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import java.util.concurrent.TimeUnit

object Services : ServiceDirectoryProviderType {

  private val servicesLock : Any = Any()
  private var servicesDirectory: ServiceDirectoryType? = null
  private val servicesFuture: SettableFuture<ServiceDirectoryType> = SettableFuture.create()

  override fun serviceDirectory(): ServiceDirectoryType {
    return this.servicesFuture.get(1L, TimeUnit.MINUTES)
  }

  fun serviceDirectoryFuture(): ListenableFuture<ServiceDirectoryType> =
    this.servicesFuture

  fun isInitialized(): Boolean {
    return synchronized(this.servicesLock) {
      this.servicesDirectory != null
    }
  }

  fun initialize(services: ServiceDirectoryType) {
    return synchronized(this.servicesLock) {
      check(this.servicesDirectory == null) {
        "Service directory has already been initialized!"
      }
      this.servicesDirectory = services
      this.servicesFuture.set(services)
    }
  }
}