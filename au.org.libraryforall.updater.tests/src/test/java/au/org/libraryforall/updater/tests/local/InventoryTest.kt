package au.org.libraryforall.updater.tests.local

import au.org.libraryforall.updater.apkinstaller.api.APKInstallerType
import au.org.libraryforall.updater.installed.api.InstalledItemsType
import au.org.libraryforall.updater.inventory.api.InventoryAPKDirectoryType
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryDatabaseType
import au.org.libraryforall.updater.inventory.api.InventoryStringResourcesType
import au.org.libraryforall.updater.inventory.api.InventoryType
import au.org.libraryforall.updater.inventory.vanilla.Inventory
import au.org.libraryforall.updater.repository.xml.api.RepositoryXMLParserProviderType
import au.org.libraryforall.updater.tests.InventoryContract
import com.google.common.util.concurrent.ListeningExecutorService
import one.irradia.http.api.HTTPAuthentication
import one.irradia.http.api.HTTPClientType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI

class InventoryTest : InventoryContract() {

  override fun logger(): Logger =
    LoggerFactory.getLogger(InventoryTest::class.java)

  override fun inventory(
    resources: InventoryStringResourcesType,
    executorService: ListeningExecutorService,
    http: HTTPClientType,
    httpAuthentication: (URI) -> HTTPAuthentication?,
    database: InventoryRepositoryDatabaseType,
    apkDirectory: InventoryAPKDirectoryType,
    apkInstaller: APKInstallerType,
    repositoryParsers: RepositoryXMLParserProviderType,
    packages: InstalledItemsType
  ): InventoryType {
    return Inventory.open(
      resources = resources,
      executor = executorService,
      installedPackages = packages,
      http = http,
      httpAuthentication = httpAuthentication,
      inventoryDatabase = database,
      apkDirectory = apkDirectory,
      apkInstaller = apkInstaller,
      repositoryParsers = repositoryParsers
    )
  }

}
