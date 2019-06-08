package au.org.libraryforall.updater.inventory.api

import au.org.libraryforall.updater.inventory.api.InventoryAPKDirectoryType.VerificationProgressType
import org.joda.time.Instant
import org.joda.time.Seconds
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A throttled verification progress receiver that delegates one update per second
 * to a given receiver function.
 */

class InventoryAPKDirectoryThrottledVerificationReceiver(
  private val delegate: (VerificationProgressType) -> Unit,
  private val cancel: AtomicBoolean)
  : (VerificationProgressType) -> Unit {

  @Volatile
  private var receivedBPS = 0L

  @Volatile
  private var receivedLast = 0L

  @Volatile
  private var timeLast = Instant.now()

  @Volatile
  private var timeCurrent = Instant.now()

  override fun invoke(progress: VerificationProgressType) {
    if (this.cancel.get()) {
      progress.cancel()
      return
    }

    this.timeCurrent = Instant.now()
    if (Seconds.secondsBetween(this.timeLast, this.timeCurrent).seconds >= 1) {
      this.receivedBPS = Math.max(0L, progress.currentBytes - this.receivedLast)
      this.receivedLast = progress.currentBytes
      this.timeLast = this.timeCurrent
      this.delegate.invoke(progress)
    }
  }
}
