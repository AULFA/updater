package one.lfa.updater.installed.api

/**
 * The type of installed item events.
 */

sealed class InstalledItemEvent {

  /**
   * The set of installed items has changed.
   */

  sealed class InstalledItemsChanged : InstalledItemEvent() {

    abstract val installedItem: InstalledItem

    /**
     * A new item was added.
     */

    data class InstalledItemAdded(
      override val installedItem: InstalledItem
    ) : InstalledItemsChanged()

    /**
     * An existing item was removed.
     */

    data class InstalledItemRemoved(
      override val installedItem: InstalledItem
    ) : InstalledItemsChanged()

    /**
     * An existing item was updated.
     */

    data class InstalledItemUpdated(
      override val installedItem: InstalledItem
    ) : InstalledItemsChanged()

  }

}