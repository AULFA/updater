package au.org.libraryforall.updater.installed.api

/**
 * The type of installed package events.
 */

sealed class InstalledPackageEvent {

  /**
   * The set of installed packages has changed.
   */

  object InstalledPackagesChanged : InstalledPackageEvent()

}