package one.lfa.updater.opds.database.api

import java.util.UUID

/**
 * We expected a particular ID, but a different one was given.
 */

class OPDSDatabaseIdException(
  strings: OPDSDatabaseStringsType,

  /**
   * The expected ID.
   */

  val expected: UUID,

  /**
   * The received ID.
   */

  val received: UUID
) : OPDSDatabaseException(
  message = strings.opdsDatabaseErrorIdMismatch(
    expected = expected,
    received = received
  ))
