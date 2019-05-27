package au.org.libraryforall.updater.tests

import au.org.libraryforall.updater.apkinstaller.api.APKInstallerType
import au.org.libraryforall.updater.installed.api.InstalledPackage
import au.org.libraryforall.updater.installed.api.InstalledPackageEvent
import au.org.libraryforall.updater.installed.api.InstalledPackagesType
import au.org.libraryforall.updater.inventory.api.InventoryHashIndexedDirectoryType
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryType
import au.org.libraryforall.updater.inventory.api.InventoryStringResourcesType
import au.org.libraryforall.updater.inventory.api.InventoryType
import au.org.libraryforall.updater.inventory.vanilla.InventoryHashIndexedDirectory
import au.org.libraryforall.updater.repository.api.Hash
import au.org.libraryforall.updater.repository.api.Repository
import au.org.libraryforall.updater.repository.api.RepositoryPackage
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import one.irradia.http.api.HTTPAuthentication
import one.irradia.http.api.HTTPClientType
import org.joda.time.LocalDateTime
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.io.File
import java.net.URI
import java.util.UUID
import java.util.concurrent.Executors

abstract class InventoryContract {

  private lateinit var directory: File
  private var executor: ListeningExecutorService? = null

  protected abstract fun inventory(
    resources: InventoryStringResourcesType,
    executorService: ListeningExecutorService,
    http: HTTPClientType,
    httpAuthentication: (URI) -> HTTPAuthentication?,
    directory: InventoryHashIndexedDirectoryType,
    apkInstaller: APKInstallerType,
    packages: InstalledPackagesType): InventoryType

  class EmptyInstalledPackages : InstalledPackagesType {

    val eventSubject =
      PublishSubject.create<InstalledPackageEvent>()

    override fun packages(): Map<String, InstalledPackage> =
      mapOf()

    override fun poll() {

    }

    override val events: Observable<InstalledPackageEvent>
      get() = this.eventSubject
  }

  @Before
  fun setup() {
    this.directory = File.createTempFile("inventory-hash-indexed", "dir")
    this.directory.delete()
    this.directory.mkdirs()
    this.executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4))
  }

  @After
  fun tearDown() {
    this.executor?.shutdown()
  }

  @Test
  fun testEmpty() {
    val apkInstaller =
      Mockito.mock(APKInstallerType::class.java)

    val inventory =
      this.inventory(
        resources = InventoryStringResources(),
        executorService = this.executor!!,
        http = MockHTTP(),
        httpAuthentication = { null },
        directory = InventoryHashIndexedDirectory.create(this.directory),
        apkInstaller = apkInstaller,
        packages = EmptyInstalledPackages())

    Assert.assertEquals(
      "No repositories",
      listOf<InventoryRepositoryType>(),
      inventory.inventoryRepositories())

    Assert.assertNull(
      "No repository",
      inventory.inventoryRepositorySelect(UUID.randomUUID()))
  }

  @Test
  fun testRepositoryAdded() {
    val apkInstaller =
      Mockito.mock(APKInstallerType::class.java)

    val inventory =
      this.inventory(
        resources = InventoryStringResources(),
        executorService = this.executor!!,
        http = MockHTTP(),
        httpAuthentication = { null },
        directory = InventoryHashIndexedDirectory.create(this.directory),
        apkInstaller = apkInstaller,
        packages = EmptyInstalledPackages())

    val package0 =
      RepositoryPackage(
        id = "one.lfa.app0",
        versionCode = 23,
        versionName = "1.0.2",
        name = "App 0",
        source = URI.create("http://www.example.com/0"),
        sha256 = Hash("01ba4719c80b6fe911b091a7c05124b64eeece964e09c058ef8f9805daca546b"))

    val repository =
      Repository(
        id = UUID.randomUUID(),
        title = "LFA",
        updated = LocalDateTime(),
        packages = listOf(package0),
        source = URI.create("http://example.com"))

    val inventoryRepository =
      inventory.inventoryRepositoryPut(repository)

    Assert.assertEquals(
      "One package",
      1,
      inventoryRepository.packages.size)
  }

}

