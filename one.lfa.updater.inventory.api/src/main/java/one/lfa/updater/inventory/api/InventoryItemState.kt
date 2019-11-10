package one.lfa.updater.inventory.api

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
   * An operation (such as installation, or uninstallation) failed for the given item.
   */

  data class Failed(
    override val inventoryItem: InventoryRepositoryItemType,
    val result: InventoryItemResult
  ) : InventoryItemState()

  /**
   * An operation is in progress for the item.
   */

  sealed class Operating : InventoryItemState() {

    abstract val major: InventoryProgressValue?
    abstract val minor: InventoryProgressValue
    abstract val status: String

    /**
     * The item is currently installing.
     */

    data class Installing(
      override val inventoryItem: InventoryRepositoryItemType,
      override val major: InventoryProgressValue?,
      override val minor: InventoryProgressValue,
      override val status: String
    ) : Operating()

    /**
     * The item is currently uninstalling.
     */

    data class Uninstalling(
      override val inventoryItem: InventoryRepositoryItemType,
      override val major: InventoryProgressValue?,
      override val minor: InventoryProgressValue,
      override val status: String
    ) : Operating()
  }
}
