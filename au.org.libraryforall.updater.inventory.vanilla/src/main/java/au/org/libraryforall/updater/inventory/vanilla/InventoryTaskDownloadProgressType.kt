package au.org.libraryforall.updater.inventory.vanilla

/**
 * A receiver of download progress.
 */

interface InventoryTaskDownloadProgressType {

  /**
   * The expected total number of bytes.
   */

  val expectedBytesTotal : Long?

  /**
   * The total number of bytes received so far.
   */

  val receivedBytesTotal : Long

  /**
   * An estimate of the number of bytes being received per second.
   */

  val receivedBytesPerSecond : Long
}
