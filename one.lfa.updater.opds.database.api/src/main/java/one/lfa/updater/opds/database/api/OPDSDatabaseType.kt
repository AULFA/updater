package one.lfa.updater.opds.database.api

import io.reactivex.Observable
import one.lfa.updater.opds.api.OPDSManifest
import java.util.UUID
import javax.annotation.concurrent.ThreadSafe

/**
 * A database of OPDS catalogs.
 *
 * Implementations are required to be safe to access concurrently from multiple threads.
 */

@ThreadSafe
interface OPDSDatabaseType {

  /**
   * An observable value that publishes events on database changes.
   */

  val events: Observable<OPDSDatabaseEvent>

  /**
   * A read-only view of the current set of available catalogs.
   */

  val catalogs: Set<UUID>

  /**
   * Open an existing database entry.
   */

  fun open(id: UUID): OPDSDatabaseEntryType?

  /**
   * Create a new entry, or update an existing entry, with the given manifest.
   */

  @Throws(OPDSDatabaseException::class)
  fun createOrUpdate(manifest: OPDSManifest): OPDSDatabaseEntryType

}
