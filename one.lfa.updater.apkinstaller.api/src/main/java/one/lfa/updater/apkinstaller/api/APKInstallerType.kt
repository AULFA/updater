package one.lfa.updater.apkinstaller.api

import java.io.File

/**
 * An APK installer interface.
 */

interface APKInstallerType {

  /**
   * Create an installation task for the given package name and version.
   */

  fun createInstallTask(
    activity: Any,
    packageName: String,
    packageVersionCode: Int,
    file: File
  ): APKInstallTaskType

  /**
   * Tell the installer that an APK was removed.
   */

  fun reportAPKRemoved(
    packageName: String)

  /**
   * Tell the installer that an APK was installed.
   */

  fun reportAPKInstalled(
    packageName: String,
    packageVersionCode: Int)

  /**
   * Tell the installer that an installation task reported the given result code.
   */

  fun reportStatus(
    requestCode: Int,
    resultCode: Int)

}
