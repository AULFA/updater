package au.org.libraryforall.updater.app

import android.content.Context
import com.google.common.util.concurrent.MoreExecutors
import one.lfa.updater.inventory.api.InventoryType
import one.lfa.updater.repository.api.Repository
import org.joda.time.LocalDateTime
import org.slf4j.LoggerFactory

/**
 * A task that adds any bundled repositories.
 */

class BundledRepositoriesTask(
  private val context: Context,
  private val inventory: InventoryType) {

  private val logger = LoggerFactory.getLogger(BundledRepositoriesTask::class.java)

  /**
   * Execute the task.
   */

  fun execute() {
    try {
      val repositories = BundledRepositories.fromXMLResources(this.context)
      for (bundledRepository in repositories) {
        try {
          this.logger.debug("loading bundled repository: {}", bundledRepository.uri)

          val repository =
            Repository(
              bundledRepository.requiredUUID,
              bundledRepository.title,
              LocalDateTime(),
              listOf(),
              bundledRepository.uri)

          val existing =
            this.inventory.inventoryRepositorySelect(bundledRepository.requiredUUID)

          if (existing == null) {
            this.inventory.inventoryRepositoryPut(repository)
              .addListener(
                Runnable {
                  this.inventory.inventoryRepositorySelect(bundledRepository.requiredUUID)
                    ?.update()
                },
                MoreExecutors.directExecutor())
          } else {
            this.logger.debug("bundled repository {} already exists", bundledRepository.requiredUUID)
            existing.update()
          }
        } catch (e: Exception) {
          this.logger.error("error loading bundled repository {}: ", bundledRepository.uri, e)
        }
      }
    } catch (e: Exception) {
      this.logger.error("error loading bundled repositories: ", e)
    }
  }
}