package au.org.libraryforall.updater.app

import one.lfa.updater.inventory.api.InventoryHTTPConfigurationType

object InventoryHTTPConfiguration : InventoryHTTPConfigurationType {

  override val retryCount: Int
    get() = 100

  override val retryDelaySeconds: Long
    get() = 5L

}
