package one.lfa.updater.inventory.vanilla.tasks

import one.lfa.updater.inventory.api.InventoryTaskStep

/**
 * The result of executing an inventory task.
 */

sealed class InventoryTaskResult<T> {

  abstract val steps: List<InventoryTaskStep>

  /**
   * The task succeeded and yielded a result.
   */

  data class InventoryTaskSucceeded<T>(
    val result: T,
    override val steps: List<InventoryTaskStep>
  ) : InventoryTaskResult<T>()

  /**
   * The task failed.
   */

  data class InventoryTaskFailed<T>(
    override val steps: List<InventoryTaskStep>
  ) : InventoryTaskResult<T>()

  /**
   * The task was cancelled.
   */

  data class InventoryTaskCancelled<T>(
    override val steps: List<InventoryTaskStep>
  ) : InventoryTaskResult<T>()

  companion object {

    /**
     * Construct a cancelled task with a single step.
     */

    fun <T> cancelled(step: InventoryTaskStep): InventoryTaskCancelled<T> =
      InventoryTaskCancelled(listOf(step))

    /**
     * Construct a failed task with a single step.
     */

    fun <T> failed(step: InventoryTaskStep): InventoryTaskFailed<T> =
      InventoryTaskFailed(listOf(step))

    /**
     * Construct a succeeded task with a single step and value.
     */

    fun <T> succeeded(result: T, step: InventoryTaskStep): InventoryTaskSucceeded<T> =
      InventoryTaskSucceeded(result, listOf(step))
  }
}