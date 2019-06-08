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
   * The list of packages in the repository.
   */

  val packages: List<RepositoryPackage>,

  /**
   * A link to this repository.
   */

  val self: URI) {

  /*
   * A view of the repository packages such that only the highest version of each package
   * is shown.
   */

  val packagesNewest = packagesNewest(this.packages)

  companion object {
    private fun packagesNewest(packages: List<RepositoryPackage>) : Map<String, RepositoryPackage> {
      val results = mutableMapOf<String, RepositoryPackage>()
      for (repositoryPackage in packages) {
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
