package one.lfa.updater.opds.database.api

import java.util.UUID

interface OPDSDatabaseStringsType {

  /**
   * We expected a particular ID, but a different one was provided.
   */

  fun opdsDatabaseErrorIdMismatch(
    expected: UUID,
    received: UUID
  ): String

}