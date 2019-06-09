package au.org.libraryforall.updater.apkinstaller.api

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import au.org.libraryforall.updater.apkinstaller.api.APKInstallTaskType.Status
import au.org.libraryforall.updater.apkinstaller.api.APKInstallTaskType.Status.Cancelled
import au.org.libraryforall.updater.apkinstaller.api.APKInstallTaskType.Status.Failed
import au.org.libraryforall.updater.apkinstaller.api.APKInstallTaskType.Status.Succeeded
import au.org.libraryforall.updater.installed.api.InstalledPackageEvent
import au.org.libraryforall.updater.installed.api.InstalledPackagesType
import com.google.common.util.concurrent.SettableFuture
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.random.Random

class APKInstallerDevice private constructor(
  private val installedPackages: InstalledPackagesType) : APKInstallerType {

  init {
    this.installedPackages.events.subscribe(this::onInstalledPackageEvent)
  }

  private fun onInstalledPackageEvent(event: InstalledPackageEvent) =
    when (event) {
      is InstalledPackageEvent.InstalledPackagesChanged.InstalledPackageAdded ->
        this.reportAPKInstalled(event.installedPackage.id, event.installedPackage.versionCode)
      is InstalledPackageEvent.InstalledPackagesChanged.InstalledPackageRemoved ->
        this.reportAPKRemoved(event.installedPackage.id)
      is InstalledPackageEvent.InstalledPackagesChanged.InstalledPackageUpdated ->
        this.reportAPKInstalled(event.installedPackage.id, event.installedPackage.versionCode)
    }

  override fun reportStatus(requestCode: Int, resultCode: Int) {
    synchronized(this.requestCodesLock) {
      val task = this.requests.find { r -> r.requestCode == requestCode }
      if (task != null) {
        task.future.set(when (resultCode) {
          Activity.RESULT_OK -> Succeeded
          Activity.RESULT_CANCELED -> Cancelled
          else -> Failed(resultCode)
        })
        this.requests.remove(task)
      }
    }
  }

  override fun reportAPKRemoved(packageName: String) {
    this.logger.debug("reportAPKRemoved: ${packageName}: received")

    synchronized(this.requestCodesLock) {
      val iter = this.requests.iterator()
      while (iter.hasNext()) {
        val request = iter.next()
        if (request.packageName == packageName) {
          this.logger.debug("reportAPKRemoved: ${packageName}: finished task")
          request.future.set(Cancelled)
          iter.remove()
        }
      }
    }
  }

  override fun reportAPKInstalled(
    packageName: String,
    packageVersionCode: Int) {
    this.logger.debug("reportAPKInstalled: ${packageName} ${packageVersionCode}: received")

    synchronized(this.requestCodesLock) {
      val task = this.findTaskLocked(packageName, packageVersionCode)
      if (task == null) {
        this.logger.error("reportAPKInstalled: ${packageName} ${packageVersionCode}: no such task!")
        return
      }

      this.logger.debug("reportAPKInstalled: ${packageName} ${packageVersionCode}: finished task")
      task.future.set(Succeeded)
      this.requests.remove(task)
    }
  }

  companion object {
    fun create(installedPackages: InstalledPackagesType): APKInstallerType =
      APKInstallerDevice(installedPackages)
  }

  private val logger = LoggerFactory.getLogger(APKInstallerDevice::class.java)
  private val requestCodesLock = Object()
  private val requests = mutableListOf<InstallTask>()

  inner class InstallTask(
    override val packageName: String,
    override val packageVersionCode: Int,
    override val file: File,
    override val future: SettableFuture<Status>,
    val requestCode: Int
  ) : APKInstallTaskType

  private fun withFreshRequestCode(receiver: (Int) -> InstallTask): InstallTask {
    for (i in 0..10_000) {
      val value = Random.nextInt(1, 65535)
      synchronized(this.requestCodesLock) {
        if (this.requests.find { r -> r.requestCode == value } == null) {
          val task = receiver.invoke(value)
          this.requests.add(task)
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
    if (Build.VERSION.SDK_INT < 24) {
      this.logger.debug("running on Android ${Build.VERSION.SDK_INT}: can only use file:// URIs")
      // https://code.google.com/p/android/issues/detail?id=205827
      file.toUri()
    } else {
      this.logger.debug("running on modern Android ${Build.VERSION.SDK_INT}: resolving content URI")
      FileProvider.getUriForFile(
        activity,
        activity.applicationContext.packageName + ".provider",
        file)
    }

    this.logger.debug("resolved content URI: {}", targetFile)

    val future =
      SettableFuture.create<Status>()

    val installTask =
      synchronized(this.requestCodesLock) {
        val existingTask = this.findTaskLocked(packageName, packageVersionCode)
        if (existingTask != null) {
          this.logger.debug("reusing existing task for package ${packageName} ${packageVersionCode}")
          return existingTask
        }

        this.logger.debug("registering task for package ${packageName} ${packageVersionCode}")
        this.withFreshRequestCode { code ->
          this.InstallTask(packageName, packageVersionCode, file, future, code)
        }
      }

    this.logger.debug("starting installer for ${packageName} ${packageVersionCode}")
    val intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
    intent.setDataAndType(targetFile, "application/vnd.android.package-archive")
    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    intent.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, activity.applicationInfo.packageName)
    intent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
    activity.startActivityForResult(intent, installTask.requestCode)
    return installTask
  }

  private fun findTaskLocked(packageName: String, packageVersionCode: Int): InstallTask? {
    return this.requests.find { t ->
      t.packageName == packageName && t.packageVersionCode == packageVersionCode
    }
  }
}
