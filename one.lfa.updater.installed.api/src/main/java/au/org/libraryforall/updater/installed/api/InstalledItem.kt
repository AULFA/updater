package au.org.libraryforall.updater.installed.api

import org.joda.time.Instant

/**
 * An installed item.
 */

data class InstalledItem(

  /**
   * The ID of the item.
   */

  val id: String,

  /**
   * The integer version code of the item.
   */

  val versionCode: Long,

  /**
   * The humanly-readable item version.
   */

  val versionName: String,

  /**
   * The humanly-readable name of the item.
   */

  val name: String,

  /**
   * The time that the item was last updated.
   */

  val lastUpdated: Instant)
