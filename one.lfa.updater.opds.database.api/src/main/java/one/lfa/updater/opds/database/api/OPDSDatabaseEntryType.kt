package one.lfa.updater.opds.database.api

import one.lfa.updater.opds.api.OPDSManifest
import java.io.File
import java.util.UUID

/**
 * An entry in an OPDS catalog database.
 */

interface OPDSDatabaseEntryType {

  /**
   * The current manifest.
   */

  val manifest: OPDSManifest

  /**
   * Shorthand for the ID in the manifest.
   */

  val id: UUID
    get() = this.manifest.id

  /**
   * The "name" of the version
   */

  val versionName: String
    get() = this.manifest.updated.toString()

  /**
   * The "code" of the version, for ordering versions
   */

  val versionCode: Long

  /**
   * The directory containing catalog files.
   */

  val directory: File

  /**
   * Update the entry with a new manifest revision. The ID of the given revision must match
   * the ID of the current manifest.
   *
   * @throws OPDSDatabaseIdException If the manifest ID does not match
   * @throws OPDSDatabaseException On other errors
   */

  @Throws(OPDSDatabaseException::class)
  fun update(newManifest: OPDSManifest)

}
