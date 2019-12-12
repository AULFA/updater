package one.lfa.updater.apkinstaller.device

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import one.lfa.updater.installed.api.InstalledApplicationEvent
import one.lfa.updater.installed.api.InstalledApplicationsType
import com.google.common.util.concurrent.SettableFuture
import one.lfa.updater.apkinstaller.api.APKInstallTaskType
import one.lfa.updater.apkinstaller.api.APKInstallerStatus
import one.lfa.updater.apkinstaller.api.APKInstallerType
import one.lfa.updater.apkinstaller.api.APKUninstallTaskType
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.random.Random

/**
 * An APK installer that calls the real Android API to install files.
 */

class APKInstallerDevice private constructor(
  private val installedItems: InstalledApplicationsType
) : APKInstallerType {

  init {
    this.installedItems.events.subscribe(this::onInstalledItemEvent)
  }

  private val logger =
    LoggerFactory.getLogger(APKInstallerDevice::class.java)

  private val installRequestRange = 1 until 1000
  private val uninstallRequestRange = 1000 until 2000
  
  private fun onInstalledItemEvent(event: InstalledApplicationEvent) =
    when (event) {
      is InstalledApplicationEvent.InstalledApplicationsChanged.InstalledApplicationAdded ->
        this.reportAPKInstalled(event.installedApplication.id, event.installedApplication.versionCode.toInt())
      is InstalledApplicationEvent.InstalledApplicationsChanged.InstalledApplicationRemoved ->
        this.reportAPKRemoved(event.installedApplication.id)
      is InstalledApplicationEvent.InstalledApplicationsChanged.InstalledApplicationUpdated ->
        this.reportAPKInstalled(event.installedApplication.id, event.installedApplication.versionCode.toInt())
    }

  override fun reportStatus(
    requestCode: Int,
    resultCode: Int
  ) {
    if (this.installRequestRange.contains(requestCode)) {
      return synchronized(this.installRequestCodesLock) {
        val task =
          this.installRequests.find { r -> r.requestCode == requestCode }
        if (task != null) {
          task.future.set(when (resultCode) {
            Activity.RESULT_OK -> APKInstallerStatus.Succeeded
            Activity.RESULT_CANCELED -> APKInstallerStatus.Cancelled
            else -> APKInstallerStatus.Failed(resultCode)
          })
          this.installRequests.remove(task)
        }
      }
    }
    
    if (this.uninstallRequestRange.contains(requestCode)) {
      return synchronized(this.uninstallRequestCodesLock) {
        val task =
          this.uninstallRequests.find { r -> r.requestCode == requestCode }
        if (task != null) {
          task.future.set(when (resultCode) {
            Activity.RESULT_OK -> APKInstallerStatus.Succeeded
            Activity.RESULT_CANCELED -> APKInstallerStatus.Cancelled
            else -> APKInstallerStatus.Failed(resultCode)
          })
          this.uninstallRequests.remove(task)
        }
      }
    }
  }

  override fun reportAPKRemoved(packageName: String) {
    this.logger.debug("reportAPKRemoved: ${packageName}: received")

    synchronized(this.installRequestCodesLock) {
      val iter = this.installRequests.iterator()
      while (iter.hasNext()) {
        val request = iter.next()
        if (request.packageName == packageName) {
          this.logger.debug("reportAPKRemoved: ${packageName}: finished task")
          request.future.set(APKInstallerStatus.Cancelled)
          iter.remove()
        }
      }
    }
  }

  override fun reportAPKInstalled(
    packageName: String,
    packageVersionCode: Int) {
    this.logger.debug("reportAPKInstalled: ${packageName} ${packageVersionCode}: received")

    synchronized(this.installRequestCodesLock) {
      val task = this.findInstallTaskLocked(packageName, packageVersionCode)
      if (task == null) {
        this.logger.error("reportAPKInstalled: ${packageName} ${packageVersionCode}: no such task!")
        return
      }

      this.logger.debug("reportAPKInstalled: ${packageName} ${packageVersionCode}: finished task")
      task.future.set(APKInstallerStatus.Succeeded)
      this.installRequests.remove(task)
    }
  }

  companion object {

    /**
     * Create a new installer.
     */

    fun create(installedItems: InstalledApplicationsType): APKInstallerType =
      APKInstallerDevice(installedItems)
  }
  
  private val installRequestCodesLock = Object()
  private val installRequests = mutableListOf<InstallTask>()
  private val uninstallRequestCodesLock = Object()
  private val uninstallRequests = mutableListOf<UninstallTask>()
  
  inner class InstallTask(
    override val packageName: String,
    override val packageVersionCode: Int,
    override val file: File,
    override val future: SettableFuture<APKInstallerStatus>,
    val requestCode: Int
  ) : APKInstallTaskType

  inner class UninstallTask(
    override val packageName: String,
    override val future: SettableFuture<APKInstallerStatus>,
    val requestCode: Int
  ) : APKUninstallTaskType

  private fun withFreshInstallRequestCode(receiver: (Int) -> InstallTask): InstallTask {
    for (i in 0..1000) {
      val value =
        Random.nextInt(this.installRequestRange.first, this.installRequestRange.last)
      synchronized(this.installRequestCodesLock) {
        if (this.installRequests.find { r -> r.requestCode == value } == null) {
          val task = receiver.invoke(value)
          this.installRequests.add(task)
          return task
        }
      }
    }

    throw IllegalStateException("Could not generate a fresh request code ID")
  }

  private fun withFreshUninstallRequestCode(receiver: (Int) -> UninstallTask): UninstallTask {
    for (i in 0..1000) {
      val value =
        Random.nextInt(this.uninstallRequestRange.first, this.uninstallRequestRange.last)
      synchronized(this.uninstallRequestCodesLock) {
        if (this.uninstallRequests.find { r -> r.requestCode == value } == null) {
          val task = receiver.invoke(value)
          this.uninstallRequests.add(task)
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
          "au.org.libraryforall.updater.app",
          file)
      }

    this.logger.debug("resolved content URI: {}", targetFile)

    val future =
      SettableFuture.create<APKInstallerStatus>()

    val installTask =
      synchronized(this.installRequestCodesLock) {
        val existingTask = this.findInstallTaskLocked(packageName, packageVersionCode)
        if (existingTask != null) {
          this.logger.debug("reusing existing task for package ${packageName} ${packageVersionCode}")
          return existingTask
        }

        this.logger.debug("registering task for package ${packageName} ${packageVersionCode}")
        this.withFreshInstallRequestCode { code ->
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

  override fun createUninstallTask(
    activity: Any,
    packageName: String
  ): APKUninstallTaskType {

    if (!(activity is Activity)) {
      throw IllegalArgumentException(
        "Activity ${activity} must be a subtype of ${Activity::class.java}")
    }

    val future =
      SettableFuture.create<APKInstallerStatus>()

    val uninstallTask =
      synchronized(this.uninstallRequestCodesLock) {
        val existingTask = this.findUninstallTaskLocked(packageName)
        if (existingTask != null) {
          this.logger.debug("reusing existing task for package ${packageName}")
          return existingTask
        }

        this.logger.debug("registering task for package ${packageName}")
        this.withFreshUninstallRequestCode { code ->
          this.UninstallTask(packageName, future, code)
        }
      }

    this.logger.debug("starting uninstaller for ${packageName}")
    val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE)
    intent.setData(Uri.parse("package:${packageName}"));
    intent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
    activity.startActivityForResult(intent, uninstallTask.requestCode)
    return uninstallTask
  }


  private fun findInstallTaskLocked(
    packageName: String,
    packageVersionCode: Int
  ): InstallTask? {
    return this.installRequests.find { t ->
      t.packageName == packageName && t.packageVersionCode == packageVersionCode
    }
  }

  private fun findUninstallTaskLocked(
    packageName: String
  ): UninstallTask? {
    return this.uninstallRequests.find { t ->
      t.packageName == packageName
    }
  }
}
