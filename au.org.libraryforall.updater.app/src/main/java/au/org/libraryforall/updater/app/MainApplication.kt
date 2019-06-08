package au.org.libraryforall.updater.app

import android.app.Application
import org.slf4j.LoggerFactory

class MainApplication : Application() {

  private val logger = LoggerFactory.getLogger(MainApplication::class.java)

  override fun onCreate() {
    super.onCreate()
    this.logger.debug("starting")
    MainServices.initialize(this)
  }
}