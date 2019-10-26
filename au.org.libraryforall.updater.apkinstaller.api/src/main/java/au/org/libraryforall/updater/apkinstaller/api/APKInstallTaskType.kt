package au.org.libraryforall.updater.apkinstaller.api

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

  val future: ListenableFuture<Status>

  /**
   * The status of the task.
   */

  sealed class Status {

    /**
     * Installation failed with the given error code.
     */

    data class Failed(
      val errorCode: Int
    ) : Status()

    /**
     * Installation was cancelled.
     */

    object Cancelled
      : Status()

    /**
     * Installation succeeded.
     */

    object Succeeded
      : Status()
  }
}