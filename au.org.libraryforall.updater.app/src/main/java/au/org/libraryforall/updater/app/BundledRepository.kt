package au.org.libraryforall.updater.app

import java.net.URI
import java.util.UUID

data class BundledRepository(
  val uri: URI,
  val requiredUUID: UUID,
  val title: String)
