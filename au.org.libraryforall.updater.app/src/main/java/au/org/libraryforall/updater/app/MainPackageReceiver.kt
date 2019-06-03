package au.org.libraryforall.updater.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.slf4j.LoggerFactory

class MainPackageReceiver : BroadcastReceiver() {

  private val logger = LoggerFactory.getLogger(MainPackageReceiver::class.java)
  private val apkInstaller = MainServices.apkInstaller()

  init {
    this.logger.debug("started")
  }

  override fun onReceive(context: Context?, intent: Intent?) {
    this.logger.debug("onReceive: {} {}", context, intent)
    
    if (intent != null) {
      val packageNameURI = intent.data
      if (packageNameURI != null) {
        val packageName = packageNameURI.schemeSpecificPart
        val action = intent.action
        this.logger.debug("onReceive: packageName: {}", packageName)
        this.logger.debug("onReceive: action: {}", action)

        return when (action) {
          "android.intent.action.PACKAGE_ADDED" -> {
            val packageInstalledInfo =
              context!!.packageManager.getInstalledPackages(0)
                .find { packageInfo -> packageInfo.packageName == packageName }

            if (packageInstalledInfo == null) {
              throw IllegalStateException(
                """Package receiver received PACKAGE_ADDED for ${packageName},
                  |but the package manager says no such package is installed""".trimMargin())
            }

            val versionCode = packageInstalledInfo.versionCode
            this.logger.debug("package version is {} {}", packageName, versionCode)
            this.apkInstaller.reportAPKInstalled(packageName, versionCode)
          }
          else -> {
            this.logger.error("unrecognized action received")
          }
        }
      } else {
        this.logger.error("no package URI received")
      }
    } else {
      this.logger.error("null intent received")
    }
  }
}