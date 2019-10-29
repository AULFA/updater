package au.org.libraryforall.updater.inventory.api

import org.joda.time.Instant

/**
 * A clock interface.
 */

interface InventoryClockType {

  /**
   * @return The current time
   */

  fun now(): Instant
}
