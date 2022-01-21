package au.org.libraryforall.updater.main

import one.lfa.updater.inventory.api.InventoryHTTPConfigurationType

object InventoryHTTPConfiguration : InventoryHTTPConfigurationType {

  override val retryCount: Int
    get() = 5

  override val retryDelaySeconds: Long
    get() = 5L

}
