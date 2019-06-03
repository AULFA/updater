package au.org.libraryforall.updater.installed.api

import io.reactivex.Observable

/**
 * A view of the installed packages.
 */

interface InstalledPackagesType {

  /**
   * Retrieve a map of the currently installed packages, organized by package name
   */

  fun packages(): Map<String, InstalledPackage>

  /**
   * An observable that publishes package events.
   */

  val events: Observable<InstalledPackageEvent>

}
