package au.org.libraryforall.updater.apkinstaller.api

import android.app.Activity
import android.content.Intent
import androidx.core.content.FileProvider
import com.google.common.util.concurrent.SettableFuture
import org.slf4j.LoggerFactory
import java.io.File

class APKInstallerDevice : APKInstallerType {

  override fun reportAPKRemoved(
    packageName: String,
    packageVersionCode: Int
  ) {
    this.logger.debug("reportAPKRemoved: ${packageName} ${packageVersionCode}: received")

    val key = Pair(packageName, packageVersionCode)
    synchronized(this.requestCodesLock) {
      val task = this.requests[key]
      if (task == null) {
        this.logger.error("reportAPKRemoved: ${packageName} ${packageVersionCode}: no such task!")
        return
      }

      this.logger.debug("reportAPKRemoved: ${packageName} ${packageVersionCode}: finished task")
      this.requests.remove(key)
      task.future.set(false)
    }
  }

  override fun reportAPKInstalled(
    packageName: String,
    packageVersionCode: Int
  ) {
    this.logger.debug("reportAPKInstalled: ${packageName} ${packageVersionCode}: received")

    val key = Pair(packageName, packageVersionCode)
    synchronized(this.requestCodesLock) {
      val task = this.requests[key]
      if (task == null) {
        this.logger.error("reportAPKInstalled: ${packageName} ${packageVersionCode}: no such task!")
        return
      }

      this.logger.debug("reportAPKInstalled: ${packageName} ${packageVersionCode}: finished task")
      this.requests.remove(key)
      task.future.set(true)
    }
  }

  companion object {
    fun create(): APKInstallerType =
      APKInstallerDevice()
  }

  private val logger = LoggerFactory.getLogger(APKInstallerDevice::class.java)
  private val requestCodesLock = Object()
  private val requests = HashMap<Pair<String, Int>, InstallTask>()

  inner class InstallTask(
    override val packageName: String,
    override val packageVersionCode: Int,
    override val file: File,
    override val future: SettableFuture<Boolean>
  ) : APKInstallTaskType

  override fun createInstallTask(
    activity: Any,
    packageName: String,
    packageVersionCode: Int,
    file: File
  ): APKInstallTaskType {

    if (!(activity is Activity)) {
      throw IllegalArgumentException(
        "Activity ${activity} must be a subtype of ${Activity::class.java}")
    }

    val targetFile =
      FileProvider.getUriForFile(
        activity,
        activity.applicationContext.packageName + ".provider",
        file)

    this.logger.debug("resolved content URI: {}", targetFile)

    val future =
      SettableFuture.create<Boolean>()
    val installTask =
      this.InstallTask(packageName, packageVersionCode, file, future)

    synchronized(this.requestCodesLock) {
      val key = Pair(packageName, packageVersionCode)
      if (requests.containsKey(key)) {
        this.logger.debug("reusing existing task for package ${packageName} ${packageVersionCode}")
        return requests[key]!!
      }
      this.requests[key] = installTask
    }

    this.logger.debug("starting installer for ${packageName} ${packageVersionCode}")
    val intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
    intent.setDataAndType(targetFile, "application/vnd.android.package-archive")
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
    intent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
    intent.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, activity.applicationInfo.packageName)
    activity.startActivity(intent)
    return installTask
  }
}
