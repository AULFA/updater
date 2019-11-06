package one.lfa.updater.inventory.vanilla.tasks

import au.org.libraryforall.updater.services.api.ServiceDirectoryType
import one.lfa.updater.inventory.api.InventoryProgress

/**
 * The execution context of a task.
 */

interface InventoryTaskExecutionType {

  val services: ServiceDirectoryType

  val isCancelRequested: Boolean

  val onProgress: (InventoryProgress) -> Unit

}