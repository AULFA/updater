package au.org.libraryforall.updater.inventory.vanilla

import org.joda.time.Instant
import org.joda.time.Seconds

/**
 * A throttled download progress receiver that reports progress updates to a delegate once per
 * second.
 */

class InventoryTaskDownloadThrottledReceiver(
  override val expectedBytesTotal: Long?,
  private val delegate: (InventoryTaskDownloadProgressType) -> Unit) : InventoryTaskDownloadProgressType {

  override val receivedBytesTotal: Long
    get() = this.receivedLast

  override val receivedBytesPerSecond: Long
    get() = this.receivedBPS

  @Volatile
  private var receivedBPS = 0L

  @Volatile
  private var receivedLast = 0L

  @Volatile
  private var timeLast = Instant.now()

  @Volatile
  private var timeCurrent = Instant.now()

  fun receivedNow(receivedNow: Long) {
    this.timeCurrent = Instant.now()
    if (Seconds.secondsBetween(this.timeLast, this.timeCurrent).seconds >= 1) {
      this.receivedBPS = Math.max(0L, receivedNow - this.receivedLast)
      this.receivedLast = receivedNow
      this.timeLast = this.timeCurrent
      this.delegate.invoke(this)
    }
  }
}