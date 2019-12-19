package au.org.libraryforall.updater.app

import android.app.Application
import au.org.libraryforall.updater.app.boot.BootEvent
import au.org.libraryforall.updater.app.boot.BootLoader
import au.org.libraryforall.updater.app.boot.BootLoaderType
import au.org.libraryforall.updater.app.boot.BootProcessType
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.core.rolling.RollingFileAppender
import com.google.common.util.concurrent.FluentFuture
import com.google.common.util.concurrent.ListenableFuture
import one.lfa.updater.services.api.ServiceDirectoryType
import org.slf4j.LoggerFactory
import java.io.File

class MainApplication : Application() {

  private lateinit var bootFuture: FluentFuture<ServiceDirectoryType>
  private val logger = LoggerFactory.getLogger(MainApplication::class.java)

  /**
   * We apparently can't rely on the paths configured in logback.xml to actually work
   * correctly across different devices. This bit of code tries to configure the path
   * of the log file directly.
   */

  private fun configureLogging() {
    try {
      val context = LoggerFactory.getILoggerFactory() as LoggerContext
      val outputFile = File(externalCacheDir, "log.txt").absolutePath
      for (logger in context.loggerList) {
        val index = logger.iteratorForAppenders()
        while (index.hasNext()) {
          val appender = index.next()
          if (appender is RollingFileAppender<*>) {
            (appender as RollingFileAppender<*>).file = outputFile
            appender.start()
          }
        }
      }
      this.logger.debug("logging is configured to {}", outputFile)
    } catch (e: Exception) {
      this.logger.error("could not configure logging: ", e)
    }
  }

  private val boot : BootLoaderType<ServiceDirectoryType> =
    BootLoader(
      bootStringResources = ::MainBootStrings,
      bootProcess = object: BootProcessType<ServiceDirectoryType> {
        override fun execute(onProgress: (BootEvent) -> Unit): ServiceDirectoryType {
          return MainBootServices.setup(this@MainApplication, onProgress)
        }
      }
    )

  override fun onCreate() {
    super.onCreate()
    this.configureLogging()
    this.logger.debug("starting")
    this.bootFuture = this.boot.start(this)
    INSTANCE = this
  }

  /**
   * @return A future representing the application's boot process.
   */

  val servicesBooting: ListenableFuture<ServiceDirectoryType>
    get() = this.bootFuture

  companion object {

    @Volatile
    private lateinit var INSTANCE: MainApplication

    @JvmStatic
    val application: MainApplication
      get() = this.INSTANCE
  }
}