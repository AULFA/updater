package au.org.libraryforall.updater.repository.api

import org.joda.time.LocalDateTime
import java.net.URI
import java.util.UUID

data class Repository(
  val id: UUID,
  val title: String,
  val updated: LocalDateTime,
  val packages: List<RepositoryPackage>,
  val source: URI) {

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
