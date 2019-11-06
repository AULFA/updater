package one.lfa.updater.inventory.vanilla.tasks

import one.lfa.updater.inventory.api.InventoryStringResourcesType
import one.lfa.updater.inventory.api.InventoryTaskStep

/**
 * A task that, when evaluated, downloads an OPDS catalog and verifies that all of the
 * files are correct.
 */

object InventoryTaskOPDSFetchInstall {

  fun create(): InventoryTask<Unit> {
    return InventoryTask { execution ->

      val strings =
        execution.services.requireService(InventoryStringResourcesType::class.java)

      val step =
        InventoryTaskStep(
          description = "installing OPDS package",
          resolution = "",
          exception = null,
          failed = true
        )

      InventoryTaskResult.failed<Unit>(step)
    }
  }

}
