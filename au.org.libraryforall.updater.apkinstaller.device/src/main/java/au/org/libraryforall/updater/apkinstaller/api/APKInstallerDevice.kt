package au.org.libraryforall.updater.apkinstaller.api

import android.app.Activity
import android.content.Intent
import androidx.core.content.FileProvider
import com.google.common.util.concurrent.SettableFuture
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.random.Random

class APKInstallerDevice : APKInstallerType {

  companion object {
    fun create(): APKInstallerType =
      APKInstallerDevice()
  }

  override fun reportStatus(
    code: Int,
    status: Int) {
    this.logger.debug("received report: code {} status {}", code, status)

    val installTask =
      synchronized(this.requestCodesLock) {
        this.requests.remove(code)
      }

    if (installTask != null) {
      installTask.future.set(status)
    }
  }

  private val logger = LoggerFactory.getLogger(APKInstallerDevice::class.java)
  private val requestCodesLock = Object()
  private val requests = HashMap<Int, InstallTask>()

  inner class InstallTask(
    override val packageName: String,
    override val packageVersionCode: Int,
    override val file: File,
    override val future: SettableFuture<Int>,
    val requestCode: Int
  ) : APKInstallTaskType

  private fun withFreshRequestCode(receiver: (Int) -> InstallTask): InstallTask {
    for (i in 0..10_000) {
      val value = Random.nextInt(1, 65535)
      synchronized(this.requestCodesLock) {
        if (!this.requests.containsKey(value)) {
          val task = receiver.invoke(value)
          this.requests.put(value, task)
          return task
        }
      }
    }

    throw IllegalStateException("Could not generate a fresh request code ID")
  }

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
      SettableFuture.create<Int>()

    val installTask =
      withFreshRequestCode { code ->
        InstallTask(packageName, packageVersionCode, file, future, code)
      }

    val intent = Intent(Intent.ACTION_VIEW)
    intent.setDataAndType(targetFile, "application/vnd.android.package-archive")
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
    activity.startActivityForResult(intent, installTask.requestCode)
    return installTask
  }
}
