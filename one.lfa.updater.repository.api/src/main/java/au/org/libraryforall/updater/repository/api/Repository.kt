package au.org.libraryforall.updater.repository.api

import org.joda.time.LocalDateTime
import java.net.URI
import java.util.UUID

/**
 * A repository.
 */

data class Repository(

  /**
   * A unique identity for the repository.
   */

  val id: UUID,

  /**
   * The title of the repository.
   */

  val title: String,

  /**
   * The time that the repository was most recently updated.
   */

  val updated: LocalDateTime,

  /**
   * The list of items in the repository.
   */

  val items: List<RepositoryItem>,

  /**
   * A link to this repository.
   */

  val self: URI) {

  /*
   * A view of the repository items such that only the highest version of each package
   * is shown.
   */

  val itemsNewest = itemsNewest(this.items)

  companion object {
    private fun itemsNewest(items: List<RepositoryItem>) : Map<String, RepositoryItem> {
      val results = mutableMapOf<String, RepositoryItem>()
      for (repositoryPackage in items) {
        val existing = results[repositoryPackage.id]
        if (existing != null) {
          if (repositoryPackage.versionCode > existing.versionCode) {
            results[repositoryPackage.id] = repositoryPackage
          }
        } else {
          results[repositoryPackage.id] = repositoryPackage
        }
      }
      return results.toMap()
    }
  }
}
