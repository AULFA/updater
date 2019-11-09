package one.lfa.updater.opds.database.api

import java.util.UUID

/**
 * The type of events produced by OPDS catalog databases.
 */

sealed class OPDSDatabaseEvent {

  /**
   * A database entry was updated.
   */

  data class DatabaseEntryUpdated(
    val id: UUID
  ): OPDSDatabaseEvent()

}
