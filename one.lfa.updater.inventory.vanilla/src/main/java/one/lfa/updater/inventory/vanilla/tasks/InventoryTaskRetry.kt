package one.lfa.updater.inventory.vanilla.tasks

import one.lfa.updater.inventory.api.InventoryTaskStep

/**
 * A task that can retry tasks.
 */

object InventoryTaskRetry {

  /**
   * Retry a task a given number of times. The task will be retried until it either
   * succeeds (or is cancelled), or the number of tries is exhausted. The given `one` function
   * is passed a tuple `(attempt, attemptMaximum)`.
   */

  fun <A> retrying(
    retries: Int,
    pauses: (InventoryTaskExecutionType, InventoryTaskRetryAttempt) -> InventoryTask<Unit>,
    one: (InventoryTaskRetryAttempt) -> InventoryTask<A>
  ): InventoryTask<A> {

    return InventoryTask { execution ->
      runRetrying(
        execution = execution,
        retries = retries,
        pauses = pauses,
        one = one
      )
    }
  }

  private fun <A> runRetrying(
    execution: InventoryTaskExecutionType,
    retries: Int,
    pauses: (InventoryTaskExecutionType, InventoryTaskRetryAttempt) -> InventoryTask<Unit>,
    one: (InventoryTaskRetryAttempt) -> InventoryTask<A>
  ): InventoryTaskResult<A> {
    val steps = mutableListOf<InventoryTaskStep>()
    val retryActual = Math.max(retries, 1)
    retryLoop@ for (attempt in 1..retryActual) {
      if (execution.isCancelRequested) {
        return InventoryTaskResult.InventoryTaskCancelled(listOf())
      }

      val retryAttempt = InventoryTaskRetryAttempt(attempt, retryActual)
      val task = one.invoke(retryAttempt)
      val result = task.evaluate(execution)
      if (result is InventoryTaskResult.InventoryTaskFailed) {
        steps.addAll(result.steps)
      } else {
        return result
      }

      val pauseResult =
        pauses.invoke(execution, retryAttempt)
          .evaluate(execution)

      steps.addAll(pauseResult.steps)
    }

    return InventoryTaskResult.InventoryTaskFailed(steps)
  }

}