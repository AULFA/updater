package au.org.libraryforall.updater.installed.api

import org.joda.time.Instant

/**
 * An installed package.
 */

data class InstalledPackage(

  /**
   * The ID of the package.
   */

  val id: String,

  /**
   * The integer version code of the package.
   */

  val versionCode: Int,

  /**
   * The humanly-readable package version.
   */

  val versionName: String,

  /**
   * The humanly-readable name of the package.
   */

  val name: String,

  /**
   * The time that the app was last updated.
   */

  val lastUpdated: Instant)
