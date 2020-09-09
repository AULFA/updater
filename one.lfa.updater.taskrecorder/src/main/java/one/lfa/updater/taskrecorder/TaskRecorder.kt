package one.lfa.updater.taskrecorder

import com.google.common.base.Preconditions

/**
 * A task step recorder.
 */

class TaskRecorder private constructor() : TaskRecorderType {

  companion object {

    /**
     * Create a new task recorder.
     */

    fun create(): TaskRecorderType =
      TaskRecorder()
  }

  private val steps = mutableListOf<TaskStep>()
  private val attributes = mutableMapOf<String, String>()

  override fun addAttribute(
    name: String,
    value: String
  ) {
    TaskAttributes.putRetry(this.attributes, name, value)
  }

  override fun addAttributes(attributes: Map<String, String>) {
    for ((key, value) in attributes) {
      this.addAttribute(key, value)
    }
  }

  override fun beginNewStep(message: String): TaskStep {
    val step = TaskStep(description = message)
    this.steps.add(step)
    return step
  }

  override fun currentStepSucceeded(message: String): TaskStep {
    Preconditions.checkState(this.steps.isNotEmpty(), "A step must be active")

    val step = this.steps.last()
    step.resolution = message
    return step
  }

  override fun currentStepFailed(
    message: String,
    errorCode: String,
    exception: Throwable?
  ): TaskStep {
    Preconditions.checkState(this.steps.isNotEmpty(), "A step must be active")

    val step = this.steps.last()
    step.resolution = message
    step.errorCode = errorCode
    step.exception = exception
    return step
  }

  override fun currentStepFailedAppending(
    message: String,
    errorCode: String,
    exception: Throwable
  ): TaskStep {
    Preconditions.checkState(this.steps.isNotEmpty(), "A step must be active")

    val step = this.steps.last()
    when (val ex = step.exception) {
      null -> {
        step.exception = exception
      }
      else -> {
        if (ex != exception) {
          ex.addSuppressed(exception)
        }
      }
    }

    step.failed = true
    step.errorCode = errorCode
    step.resolution = message
    return step
  }

  override fun addAll(steps: List<TaskStep>) {
    this.steps.addAll(steps)
  }

  override fun currentStep(): TaskStep? =
    this.steps.lastOrNull()

  override fun <A> finishSuccess(result: A): TaskResult.Success<A> =
    TaskResult.Success(result, this.steps, this.attributes.toMap())

  override fun <A> finishFailure(): TaskResult.Failure<A> =
    TaskResult.Failure(this.steps, this.attributes.toMap())
}
