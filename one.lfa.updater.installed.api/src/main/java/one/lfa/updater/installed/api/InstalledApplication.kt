package one.lfa.updater.installed.api

import org.joda.time.Instant

/**
 * An installed application.
 */

data class InstalledApplication(

  /**
   * The ID of the application.
   */

  val id: String,

  /**
   * The integer version code of the application.
   */

  val versionCode: Long,

  /**
   * The humanly-readable application version.
   */

  val versionName: String,

  /**
   * The humanly-readable name of the application.
   */

  val name: String,

  /**
   * The time that the application was last updated.
   */

  val lastUpdated: Instant)
