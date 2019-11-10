package one.lfa.updater.apkinstaller.api

/**
 * The status of the task.
 */

sealed class APKInstallerStatus {

  /**
   * The task failed with the given error code.
   */

  data class Failed(
    val errorCode: Int
  ) : APKInstallerStatus()

  /**
   * The task was cancelled.
   */

  object Cancelled
    : APKInstallerStatus()

  /**
   * The task succeeded.
   */

  object Succeeded
    : APKInstallerStatus()
}