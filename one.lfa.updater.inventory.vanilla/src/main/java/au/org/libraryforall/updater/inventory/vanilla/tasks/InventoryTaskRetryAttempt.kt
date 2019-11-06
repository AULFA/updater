package au.org.libraryforall.updater.inventory.vanilla.tasks

/**
 * A numbered retry attempt for a task.
 */

data class InventoryTaskRetryAttempt(

  /**
   * The current attempt number.
   */

  val attemptCurrent: Int,

  /**
   * The maximum number of downloadRetries.
   */

  val attemptMaximum: Int)