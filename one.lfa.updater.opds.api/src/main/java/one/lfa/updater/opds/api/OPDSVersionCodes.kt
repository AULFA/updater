package one.lfa.updater.opds.api

import org.joda.time.DateTime
import org.joda.time.DateTimeZone

/**
 * Functions to convert date/time values in OPDS manifests to stable version codes.
 */

object OPDSVersionCodes {

  /**
   * Convert the given time to a version code.
   */

  fun timeToVersion(
    time: DateTime
  ): Long {
    val utc = DateTime(time, DateTimeZone.UTC)
    val builder = StringBuilder(32)
    builder.append(String.format("%04d", utc.year))
    builder.append(String.format("%02d", utc.monthOfYear))
    builder.append(String.format("%02d", utc.dayOfMonth))
    builder.append(String.format("%02d", utc.hourOfDay))
    builder.append(String.format("%02d", utc.minuteOfHour))
    builder.append(String.format("%02d", utc.secondOfMinute))
    return builder.toString().toLong()
  }
}
