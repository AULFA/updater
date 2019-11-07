package one.lfa.updater.inventory.vanilla.tasks

import one.lfa.updater.inventory.api.InventoryTaskStep
import one.lfa.updater.inventory.vanilla.tasks.InventoryTaskResult.InventoryTaskCancelled
import one.lfa.updater.inventory.vanilla.tasks.InventoryTaskResult.InventoryTaskFailed
import one.lfa.updater.inventory.vanilla.tasks.InventoryTaskResult.InventoryTaskSucceeded

/**
 * An inventory task monad.
 *
 * This monad essentially implements a combined error and writer monad, with unconstrained
 * side effects.
 */

data class InventoryTask<A>(
  val f: (InventoryTaskExecutionType) -> InventoryTaskResult<A>) {

  companion object {

    /**
     * Evaluate the task.
     */

    fun <A> evaluate(
      m: InventoryTask<A>,
      x: InventoryTaskExecutionType): InventoryTaskResult<A> {
      return m.f.invoke(x)
    }

    /**
     * A computation that applies `f` to the result of evaluating `m`.
     */

    fun <A, B> map(
      m: InventoryTask<A>,
      f: (A) -> B
    ): InventoryTask<B> {
      return InventoryTask { execution ->
        when (val evaluated = evaluate(m, execution)) {
          is InventoryTaskSucceeded ->
            InventoryTaskSucceeded(f.invoke(evaluated.result), evaluated.steps)
          is InventoryTaskFailed ->
            InventoryTaskFailed<B>(evaluated.steps)
          is InventoryTaskCancelled ->
            InventoryTaskCancelled(evaluated.steps)
        }
      }
    }

    /**
     * The monadic `bind` function. Called `flatMap` to match Java and Kotlin conventions.
     */

    fun <A, B> flatMap(
      m: InventoryTask<A>,
      f: (A) -> InventoryTask<B>
    ): InventoryTask<B> {
      return InventoryTask { execution ->
        when (val result0 = evaluate(m, execution)) {
          is InventoryTaskSucceeded -> {
            val nextTask = f.invoke(result0.result)
            when (val result1 = nextTask.f.invoke(execution)) {
              is InventoryTaskSucceeded ->
                InventoryTaskSucceeded(result1.result, result0.steps.plus(result1.steps))
              is InventoryTaskFailed ->
                InventoryTaskFailed<B>(result0.steps.plus(result1.steps))
              is InventoryTaskCancelled -> {
                InventoryTaskCancelled(result0.steps.plus(result1.steps))
              }
            }
          }
          is InventoryTaskFailed ->
            InventoryTaskFailed(result0.steps)
          is InventoryTaskCancelled ->
            InventoryTaskCancelled(result0.steps)
        }
      }
    }

    /**
     * Create a task that, when evaluated, evaluates all of the given actions and discards
     * the results.
     */

    fun <A> sequenceUnit(
      actions: List<InventoryTask<A>>
    ): InventoryTask<Unit> {
      return InventoryTask { execution ->
        val steps = mutableListOf<InventoryTaskStep>()
        actionLoop@ for (action in actions) {
          when (val result = action.evaluate(execution)) {
            is InventoryTaskSucceeded -> {
              steps.addAll(result.steps)
              continue@actionLoop
            }
            is InventoryTaskCancelled -> {
              steps.addAll(result.steps)
              return@InventoryTask InventoryTaskCancelled<Unit>(steps)
            }
            is InventoryTaskFailed -> {
              steps.addAll(result.steps)
              return@InventoryTask InventoryTaskFailed<Unit>(steps)
            }
          }
        }
        InventoryTaskSucceeded(Unit, steps.toList())
      }
    }

    /**
     * Create a task that, when evaluated, evaluates all of the given actions and discards
     * the results.
     */

    fun <A, B> forAllUnit(
      items: List<A>,
      action: (A) -> InventoryTask<B>
    ): InventoryTask<Unit> {
      return InventoryTask { execution ->
        sequenceUnit(items.map { item -> action.invoke(item) })
          .evaluate(execution)
      }
    }
  }

  /**
   * Functor `map` for tasks.
   */

  fun <B> map(f: (A) -> B): InventoryTask<B> =
    map(this, f)

  /**
   * Monadic `bind` for tasks.
   */

  fun <B> flatMap(f: (A) -> InventoryTask<B>): InventoryTask<B> =
    flatMap(this, f)

  /**
   * Evaluate the task.
   */

  fun evaluate(x: InventoryTaskExecutionType): InventoryTaskResult<A> =
    evaluate(this, x)
}
