package au.org.libraryforall.updater.inventory.api

import au.org.libraryforall.updater.installed.api.InstalledPackagesType
import net.jcip.annotations.ThreadSafe

@ThreadSafe
interface InventoryProviderType {

  fun get(installedPackages: InstalledPackagesType): InventoryType

}