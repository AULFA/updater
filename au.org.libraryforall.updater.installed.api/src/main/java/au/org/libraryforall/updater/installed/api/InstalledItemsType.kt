package au.org.libraryforall.updater.installed.api

import io.reactivex.Observable

/**
 * A view of the installed items.
 */

interface InstalledItemsType {

  /**
   * Retrieve a map of the currently installed items, organized by item name
   */

  fun items(): Map<String, InstalledItem>

  /**
   * An observable that publishes item events.
   */

  val events: Observable<InstalledItemEvent>

}
