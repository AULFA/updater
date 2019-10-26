package au.org.libraryforall.updater.inventory.api

sealed class InventoryItemState {

  abstract val inventoryItem : InventoryRepositoryItemType

  data class NotInstalled(
    override val inventoryItem: InventoryRepositoryItemType
  ) : InventoryItemState()

  data class Installed(
    override val inventoryItem: InventoryRepositoryItemType,
    val installedVersionCode: Long,
    val installedVersionName: String
  ) : InventoryItemState()

  data class InstallFailed(
    override val inventoryItem: InventoryRepositoryItemType,
    val result: InventoryItemInstallResult
  ) : InventoryItemState()

  sealed class InstallingStatus {
    abstract val status: String

    data class InstallingStatusIndefinite(
      override val status: String
    ) : InstallingStatus()

    data class InstallingStatusDefinite(
      val currentBytes: Long,
      val maximumBytes: Long,
      override val status: String
    ) : InstallingStatus() {

      val percent: Double
        get() = (this.currentBytes.toDouble() / Math.max(1.0, this.maximumBytes.toDouble())) * 100.0
    }
  }

  data class Installing(
    override val inventoryItem: InventoryRepositoryItemType,
    val state: InstallingStatus
  ) : InventoryItemState()

}
