package au.org.libraryforall.updater.app

import org.joda.time.LocalDateTime
import java.net.URI
import java.util.UUID

data class BundledRepository(
  val uri: URI,
  val requiredUUID: UUID,
  val updated: LocalDateTime,
  val title: String
)
