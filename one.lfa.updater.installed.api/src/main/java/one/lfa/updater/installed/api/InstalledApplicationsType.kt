package one.lfa.updater.installed.api

import io.reactivex.Observable

/**
 * A view of the installed Android applications.
 */

interface InstalledApplicationsType {

  /**
   * Retrieve a map of the currently installed applications, organized by package ID
   */

  fun items(): Map<String, InstalledApplication>

  /**
   * An observable that publishes events events.
   */

  val events: Observable<InstalledApplicationEvent>

  /**
   * A convenience function that gives a true/false answer to the question "Is an application installed?"
   */

  fun isInstalled(id: String): Boolean =
    this.items().containsKey(id)
}
