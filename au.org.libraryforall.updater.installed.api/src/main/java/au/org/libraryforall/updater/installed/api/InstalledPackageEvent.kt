package au.org.libraryforall.updater.installed.api

/**
 * The type of installed package events.
 */

sealed class InstalledPackageEvent {

  /**
   * The set of installed packages has changed.
   */

  sealed class InstalledPackagesChanged : InstalledPackageEvent() {

    abstract val installedPackage: InstalledPackage

    data class InstalledPackageAdded(
      override val installedPackage: InstalledPackage)
      : InstalledPackagesChanged()

    data class InstalledPackageRemoved(
      override val installedPackage: InstalledPackage)
      : InstalledPackagesChanged()

    data class InstalledPackageUpdated(
      override val installedPackage: InstalledPackage)
      : InstalledPackagesChanged()

  }

}