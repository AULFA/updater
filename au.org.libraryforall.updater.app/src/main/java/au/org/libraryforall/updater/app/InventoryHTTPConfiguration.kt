package au.org.libraryforall.updater.app

import au.org.libraryforall.updater.inventory.api.InventoryHTTPConfigurationType

object InventoryHTTPConfiguration : InventoryHTTPConfigurationType {

  override val retryDelaySeconds: Long
    get() = 5L

}
