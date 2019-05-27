package au.org.libraryforall.updater.tests.local

import au.org.libraryforall.updater.apkinstaller.api.APKInstallerType
import au.org.libraryforall.updater.installed.api.InstalledPackagesType
import au.org.libraryforall.updater.inventory.api.InventoryHashIndexedDirectoryType
import au.org.libraryforall.updater.inventory.api.InventoryStringResourcesType
import au.org.libraryforall.updater.inventory.api.InventoryType
import au.org.libraryforall.updater.inventory.vanilla.Inventory
import au.org.libraryforall.updater.tests.InventoryContract
import com.google.common.util.concurrent.ListeningExecutorService
import one.irradia.http.api.HTTPAuthentication
import one.irradia.http.api.HTTPClientType
import java.net.URI

class InventoryTest : InventoryContract() {

  override fun inventory(
    resources: InventoryStringResourcesType,
    executorService: ListeningExecutorService,
    http: HTTPClientType,
    httpAuthentication: (URI) -> HTTPAuthentication?,
    directory: InventoryHashIndexedDirectoryType,
    apkInstaller: APKInstallerType,
    packages: InstalledPackagesType
  ): InventoryType {
    return Inventory.create(
      resources = resources,
      executor = executorService,
      installedPackages = packages,
      http = http,
      httpAuthentication = httpAuthentication,
      directory = directory,
        apkInstaller = apkInstaller
    )
  }

}
