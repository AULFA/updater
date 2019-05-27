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
   * Poll the implementation to see if any packages have changed (such as new packages having
   * been added, or packages having been removed).
   */

  fun poll()

  /**
   * An observable that publishes package events.
   */

  val events: Observable<InstalledPackageEvent>

}
