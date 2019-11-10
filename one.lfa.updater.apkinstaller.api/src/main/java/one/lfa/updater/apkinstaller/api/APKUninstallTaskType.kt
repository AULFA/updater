package one.lfa.updater.apkinstaller.api

import com.google.common.util.concurrent.ListenableFuture

/**
 * An uninstallation task.
 */

interface APKUninstallTaskType {

  /**
   * The ID of the package being uninstalled.
   */

  val packageName: String

  /**
   * A future representing the uninstallation task in progress.
   */

  val future: ListenableFuture<APKInstallerStatus>
}