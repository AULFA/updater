package au.org.libraryforall.updater.app.boot

/**
 * The type of events that are published while the application boots.
 */

sealed class BootEvent {

  abstract val message: String

  /**
   * Booting is in progress.
   */

  data class BootInProgress(
    override val message: String
  ) : BootEvent()

  /**
   * Booting has completed.
   */

  data class BootCompleted(
    override val message: String
  ) : BootEvent()

  /**
   * Booting has failed.
   */

  data class BootFailed(
    override val message: String,
    val exception: Throwable
  ) : BootEvent()
}
