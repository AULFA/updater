package one.lfa.updater.installed.api

/**
 * The type of installed application events.
 */

sealed class InstalledApplicationEvent {

  /**
   * The set of installed applications has changed.
   */

  sealed class InstalledApplicationsChanged : InstalledApplicationEvent() {

    abstract val installedApplication: InstalledApplication

    /**
     * A new application was added.
     */

    data class InstalledApplicationAdded(
      override val installedApplication: InstalledApplication
    ) : InstalledApplicationsChanged()

    /**
     * An existing application was removed.
     */

    data class InstalledApplicationRemoved(
      override val installedApplication: InstalledApplication
    ) : InstalledApplicationsChanged()

    /**
     * An existing application was updated.
     */

    data class InstalledApplicationUpdated(
      override val installedApplication: InstalledApplication
    ) : InstalledApplicationsChanged()

  }

}