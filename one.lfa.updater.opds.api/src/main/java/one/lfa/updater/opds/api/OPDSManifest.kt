package one.lfa.updater.opds.api

import org.joda.time.DateTime
import org.joda.time.LocalDateTime
import java.net.URI
import java.util.UUID

/**
 * A manifest of all the files that need to be downloaded in order to
 * reproduce a given OPDS feed. Manifests contain links to content, and
 * full hash information to ensure that content has been downloaded
 * correctly.
 */

data class OPDSManifest(

  /**
   * The base attribute specifies the base address of the content; all
   * files specified in the manifest should be resolved relative to this
   * base address.
   */

  val baseURI: URI?,

  /**
   * The file that represents the root of the feed.
   */

  val rootFile: URI,

  /**
   * The time that the manifest was generated.
   */

  val updated: DateTime,

  /**
   * The file that represents the search index for the feed, if one was provided.
   */

  val searchIndex: URI?,

  /**
   * The id attribute specifies the globally-unique ID of the feed
   * described by the manifest.
   */

  val id: UUID,

  /**
   * The list of files that must be downloaded.
   */

  val files: List<OPDSFile>,

  /**
   * The title of the OPDS feed
   */

  val title: String
)
