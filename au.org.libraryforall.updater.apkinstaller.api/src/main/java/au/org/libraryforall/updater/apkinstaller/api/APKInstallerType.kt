package au.org.libraryforall.updater.apkinstaller.api

import java.io.File

interface APKInstallerType {

  fun createInstallTask(
    activity: Any,
    packageName: String,
    packageVersionCode: Int,
    file: File)
    : APKInstallTaskType

  fun reportAPKInstalled(
    packageName: String,
    packageVersionCode: Int)

  fun reportAPKRemoved(
    packageName: String,
    packageVersionCode: Int)

}