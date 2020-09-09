package one.lfa.updater.taskrecorder

/**
 * A step in a task.
 */

data class TaskStep(
  val description: String,
  var resolution: String = "",
  var exception: Throwable? = null,
  var errorCode: String = "",
  var failed: Boolean = false
)
