package one.lfa.updater.opds.api

import java.net.URI

/**
 * A single downloadable file.
 */

data class OPDSFile(

  /**
   * The unique (within the manifest) name of the file.
   */

  val file: URI,

  /**
   * The hash of the file.
   */

  val hash: String,

  /**
   * The algorithm used to calculate the hash of the file (such as "SHA-256").
   */

  val hashAlgorithm: String
)