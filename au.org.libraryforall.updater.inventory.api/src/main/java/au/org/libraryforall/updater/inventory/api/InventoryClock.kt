package au.org.libraryforall.updater.inventory.api

import org.joda.time.Instant

/**
 * An implementation of the [InventoryClockType] interface using the system clock.
 */

object InventoryClock : InventoryClockType {
  override fun now(): Instant {
    return Instant.now()
  }
}
