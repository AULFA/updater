package one.lfa.updater.taskrecorder

import com.google.common.base.Preconditions
import one.lfa.updater.taskrecorder.TaskAttributes.mergeAttributes

/**
 * The result of executing a task.
 */

sealed class TaskResult<A> {

  abstract val steps: List<TaskStep>

  /**
   * A task succeeded.
   */

  data class Success<A>(
    val result: A,
    override val steps: List<TaskStep>,
    val attributes: Map<String, String>
  ) : TaskResult<A>() {
    init {
      Preconditions.checkArgument(
        this.steps.isNotEmpty(),
        "Must have logged at least one step")
    }

    val message: String
      get() = this.steps.last().resolution
  }

  /**
   * A task failed.
   */

  data class Failure<A>(
    override val steps: List<TaskStep>,
    val attributes: Map<String, String>
  ) : TaskResult<A>() {
    init {
      Preconditions.checkArgument(
        this.steps.isNotEmpty(),
        "Must have logged at least one step")
    }

    val exception: Throwable?
      get() = this.steps.last().exception
    val message: String
      get() = this.steps.last().resolution

    val lastErrorCode: String
      get() = run {
        return steps.last { step -> step.failed }.errorCode
      }
  }

  /**
   * @return The resolution of step `step`
   */

  fun resolutionOf(step: Int): String =
    this.steps[step].resolution

  /**
   * Functor map for task results.
   */

  fun <B> map(f: (A) -> B): TaskResult<B> {
    return when (this) {
      is Success ->
        Success(
          result = f(this.result),
          steps = this.steps,
          attributes = this.attributes
        )
      is Failure ->
        Failure(
          steps = this.steps,
          attributes = this.attributes
        )
    }
  }

  /**
   * Monadic bind for task results.
   */

  fun <B> flatMap(f: (A) -> TaskResult<B>): TaskResult<B> {
    return when (this) {
      is Success -> {
        when (val next = f.invoke(this.result)) {
          is Success ->
            Success(
              result = next.result,
              steps = this.steps.plus(next.steps),
              attributes = mergeAttributes(this.attributes, next.attributes)
            )
          is Failure ->
            Failure(
              steps = this.steps.plus(next.steps),
              attributes = mergeAttributes(this.attributes, next.attributes)
            )
        }
      }
      is Failure ->
        Failure(
          steps = this.steps,
          attributes = this.attributes
        )
    }
  }

  companion object {

    /**
     * Create a task result that indicates that a task immediately failed with the
     * given error.
     */

    fun <A> fail(
      description: String,
      resolution: String,
      errorCode: String
    ): TaskResult<A> {
      return Failure(
        attributes = mapOf(),
        steps = listOf(
          TaskStep(
            description = description,
            resolution = resolution,
            errorCode = errorCode,
            exception = null
          )
        )
      )
    }
  }
}
