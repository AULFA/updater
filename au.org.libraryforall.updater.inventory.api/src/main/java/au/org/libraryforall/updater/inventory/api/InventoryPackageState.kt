package au.org.libraryforall.updater.inventory.api

sealed class InventoryPackageState {

  abstract val inventoryPackage : InventoryRepositoryPackageType

  data class NotInstalled(
    override val inventoryPackage: InventoryRepositoryPackageType)
    : InventoryPackageState()

  data class Installed(
    override val inventoryPackage: InventoryRepositoryPackageType)
    : InventoryPackageState()

  data class InstallFailed(
    override val inventoryPackage: InventoryRepositoryPackageType,
    val result: InventoryInstallResult)
    : InventoryPackageState()

  sealed class InstallingStatus {
    abstract val status: String

    data class InstallingStatusIndefinite(
      override val status: String)
      : InstallingStatus()

    data class InstallingStatusDefinite(
      val currentBytes: Long,
      val maximumBytes: Long,
      override val status: String)
      : InstallingStatus() {

      val percent: Double
        get() = (this.currentBytes.toDouble() / Math.max(1.0, this.maximumBytes.toDouble())) * 100.0
    }
  }

  data class Installing(
    override val inventoryPackage: InventoryRepositoryPackageType,
    val state: InstallingStatus)
    : InventoryPackageState()

}
