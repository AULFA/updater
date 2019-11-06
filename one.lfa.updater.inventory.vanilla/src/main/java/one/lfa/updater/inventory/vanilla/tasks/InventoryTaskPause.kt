package one.lfa.updater.inventory.vanilla.tasks

import one.lfa.updater.inventory.api.InventoryProgress
import one.lfa.updater.inventory.api.InventoryProgressValue
import one.lfa.updater.inventory.api.InventoryTaskStep

/**
 * A task that will wait for a given number of seconds.
 */

object InventoryTaskPause {

  /**
   * Create a new task that will wait for the given number of seconds. Progress updates will
   * be published periodically, with descriptions taken from the given function.
   */

  fun create(
    progressMajor: InventoryProgressValue? = null,
    seconds: Long,
    description: String,
    status: (Long) -> String
  ): InventoryTask<Unit> {

    val step =
      InventoryTaskStep(
        description = description,
        resolution = "",
        exception = null,
        failed = false)

    return InventoryTask { execution ->
      pause(seconds, execution, progressMajor, status, step)
    }
  }

  private fun pause(
    seconds: Long,
    execution: InventoryTaskExecutionType,
    progressMajor: InventoryProgressValue?,
    status: (Long) -> String,
    step: InventoryTaskStep
  ): InventoryTaskResult<Unit> {
    for (time in 0 until seconds) {
      if (execution.isCancelRequested) {
        break
      }

      val progress =
        InventoryProgress(
          major = progressMajor,
          minor = InventoryProgressValue.InventoryProgressValueDefinite(
            current = time,
            perSecond = 1,
            maximum = seconds
          ),
          status = status.invoke(seconds - time)
        )
      execution.onProgress.invoke(progress)

      try {
        Thread.sleep(1_000L)
      } catch (e: InterruptedException) {
        Thread.currentThread().interrupt()
      }
    }

    return if (execution.isCancelRequested) {
      InventoryTaskResult.cancelled(step)
    } else {
      InventoryTaskResult.succeeded(Unit, step)
    }
  }
}
