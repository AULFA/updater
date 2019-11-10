package one.lfa.updater.opds.database.api

import java.util.UUID

/**
 * The type of events produced by OPDS catalog databases.
 */

sealed class OPDSDatabaseEvent {

  /**
   * Events related to database entries.
   */

  sealed class OPDSDatabaseEntryEvent : OPDSDatabaseEvent() {

    /**
     * The ID of the entry
     */

    abstract val id: UUID

    /**
     * A database entry was created or updated.
     */

    data class DatabaseEntryUpdated(
      override val id: UUID
    ): OPDSDatabaseEntryEvent()

    /**
     * A database entry was deleted.
     */

    class DatabaseEntryDeleted(
      override val id: UUID
    ) : OPDSDatabaseEntryEvent()
  }
}
