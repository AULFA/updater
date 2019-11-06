package au.org.libraryforall.updater.tests

import one.lfa.updater.inventory.api.InventoryClockType
import org.joda.time.Instant

class MockClock : InventoryClockType {

  @Volatile
  var now: Instant = Instant.EPOCH

  override fun now(): Instant {
    return this.now
  }
}