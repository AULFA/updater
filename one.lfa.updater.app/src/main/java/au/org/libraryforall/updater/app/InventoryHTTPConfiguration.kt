package au.org.libraryforall.updater.app

import one.lfa.updater.inventory.api.InventoryHTTPConfigurationType

object InventoryHTTPConfiguration : InventoryHTTPConfigurationType {

  override val retryDelaySeconds: Long
    get() = 5L

}
