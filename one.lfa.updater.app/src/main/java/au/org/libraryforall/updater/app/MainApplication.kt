package au.org.libraryforall.updater.app

import android.app.Application
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.core.rolling.RollingFileAppender
import org.slf4j.LoggerFactory
import java.io.File

class MainApplication : Application() {

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

  override fun onCreate() {
    super.onCreate()
    this.configureLogging()
    this.logger.debug("starting")
    MainServices.initialize(this)
  }
}