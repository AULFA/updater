package au.org.libraryforall.updater.inventory.vanilla.tasks

import au.org.libraryforall.updater.inventory.api.InventoryProgress
import au.org.libraryforall.updater.services.api.ServiceDirectoryType

/**
 * The execution context of a task.
 */

interface InventoryTaskExecutionType {

  val services: ServiceDirectoryType

  val isCancelRequested: Boolean

  val onProgress: (InventoryProgress) -> Unit

}