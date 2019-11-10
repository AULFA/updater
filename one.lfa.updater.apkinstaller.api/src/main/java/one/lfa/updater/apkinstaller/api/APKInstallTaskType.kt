package one.lfa.updater.apkinstaller.api

import com.google.common.util.concurrent.ListenableFuture
import java.io.File

/**
 * An installation task.
 */

interface APKInstallTaskType {

  /**
   * The ID of the package being installed.
   */

  val packageName: String

  /**
   * The version code of the package being installed.
   */

  val packageVersionCode: Int

  /**
   * The source APK file.
   */

  val file: File

  /**
   * A future representing the installation task in progress.
   */

  val future: ListenableFuture<APKInstallerStatus>
}