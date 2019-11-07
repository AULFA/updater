package one.lfa.updater.inventory.api

interface InventoryHTTPConfigurationType {

  val retryDelaySeconds: Long

  val retryCount: Int

}
