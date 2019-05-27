package au.org.libraryforall.updater.apkinstaller.api

import java.io.File

interface APKInstallerType {

  fun reportStatus(
    code: Int,
    status: Int)

  fun createInstallTask(
    activity: Any,
    packageName: String,
    packageVersionCode: Int,
    file: File)
    : APKInstallTaskType

}