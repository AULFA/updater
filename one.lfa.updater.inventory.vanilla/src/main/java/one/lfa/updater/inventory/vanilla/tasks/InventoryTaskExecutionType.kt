package one.lfa.updater.inventory.vanilla.tasks

import one.lfa.updater.inventory.api.InventoryProgress
import one.lfa.updater.services.api.ServiceDirectoryType

/**
 * The execution context of a task.
 */

interface InventoryTaskExecutionType {

  val services: ServiceDirectoryType

  val isCancelRequested: Boolean

  val onProgress: (InventoryProgress) -> Unit

}