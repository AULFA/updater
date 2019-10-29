package au.org.libraryforall.updater.inventory.api

/**
 * The state of a particular inventory item.
 */

sealed class InventoryItemState {

  abstract val inventoryItem: InventoryRepositoryItemType

  /**
   * The inventory item is not installed.
   */

  data class NotInstalled(
    override val inventoryItem: InventoryRepositoryItemType
  ) : InventoryItemState()

  /**
   * The given version of the item is installed.
   */

  data class Installed(
    override val inventoryItem: InventoryRepositoryItemType,
    val installedVersionCode: Long,
    val installedVersionName: String
  ) : InventoryItemState()

  /**
   * Installation failed for the given item.
   */

  data class InstallFailed(
    override val inventoryItem: InventoryRepositoryItemType,
    val result: InventoryItemInstallResult
  ) : InventoryItemState()

  /**
   * The item is currently installing.
   */

  data class Installing(
    override val inventoryItem: InventoryRepositoryItemType,
    val major: InventoryProgressValue?,
    val minor: InventoryProgressValue,
    val status: String
  ) : InventoryItemState()

}
