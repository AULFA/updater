package one.lfa.updater.inventory.api

/**
 * The progress value of an inventory task.
 *
 * Progress is specified in terms of generic "units". These "units" may correspond to bytes
 * in download tasks, files in indexing tasks, and so on.
 */

sealed class InventoryProgressValue {

  /**
   * The current number of units processed.
   */

  abstract val current: Long

  /**
   * An estimate of the number of units being processed per second.
   */

  abstract val perSecond: Long

  /**
   * The progress value is indefinite. We know how many units we've processed, and how many we're
   * processing per second, but we don't know how many units there are left to go.
   */

  data class InventoryProgressValueIndefinite(
    override val current: Long,
    override val perSecond: Long
  ) : InventoryProgressValue()

  /**
   * The progress value is definite. We know how many units we've processed, and how many we're
   * processing per second, and how many units there are left to go.
   */

  data class InventoryProgressValueDefinite(
    override val current: Long,
    override val perSecond: Long,
    val maximum: Long
  ) : InventoryProgressValue() {

    /**
     * The progressed expressed as a percentage.
     */

    val percent: Double
      get() = (this.current.toDouble() / this.maximum.toDouble()) * 100.0
  }

}
