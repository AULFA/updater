package au.org.libraryforall.updater.app

import android.content.Context
import com.google.common.util.concurrent.MoreExecutors
import one.lfa.updater.inventory.api.InventoryType
import one.lfa.updater.repository.api.Repository
import org.slf4j.LoggerFactory

/**
 * A task that adds any bundled repositories.
 */

object BundledRepositoriesTask {

  private val logger =
    LoggerFactory.getLogger(BundledRepositoriesTask::class.java)

  /**
   * Execute the task.
   */

  fun execute(
    context: Context,
    inventory: InventoryType
  ) {
    try {
      this.logger.debug("loading bundled repositories")
      val repositories = BundledRepositories.fromXMLResources(context)
      this.logger.debug("loaded {} bundled repositories", repositories.size)

      for (bundledRepository in repositories) {
        try {
          this.logger.debug("loading bundled repository: {}", bundledRepository.uri)

          val repository =
            Repository(
              bundledRepository.requiredUUID,
              bundledRepository.title,
              bundledRepository.updated,
              listOf(),
              bundledRepository.uri)

          val existing =
            inventory.inventoryRepositorySelect(bundledRepository.requiredUUID)

          var shouldPut = false
          if (existing != null) {
            if (existing.updated.isBefore(bundledRepository.updated)) {
              this.logger.debug(
                "[{}] bundled repos is newer than current repos, replacing!",
                bundledRepository.requiredUUID)
              shouldPut = true
            } else {
              this.logger.debug(
                "[{}]: repository exists and is newer than bundled",
                bundledRepository.requiredUUID)
            }
          } else {
            this.logger.debug(
              "[{}] bundled repos is nonexistent, adding!",
              bundledRepository.requiredUUID)
            shouldPut = true
          }

          if (shouldPut) {
            inventory.inventoryRepositoryPut(repository)
              .addListener(
                Runnable {
                  inventory.inventoryRepositorySelect(bundledRepository.requiredUUID)
                    ?.update()
                },
                MoreExecutors.directExecutor())
          } else {
            inventory.inventoryRepositorySelect(bundledRepository.requiredUUID)
              ?.update()
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