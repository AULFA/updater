package au.org.libraryforall.updater.tests

import android.app.Activity
import au.org.libraryforall.updater.apkinstaller.api.APKInstallTaskType
import au.org.libraryforall.updater.apkinstaller.api.APKInstallerType
import au.org.libraryforall.updater.installed.api.InstalledPackage
import au.org.libraryforall.updater.installed.api.InstalledPackageEvent
import au.org.libraryforall.updater.installed.api.InstalledPackagesType
import au.org.libraryforall.updater.inventory.api.InventoryHashIndexedDirectoryType
import au.org.libraryforall.updater.inventory.api.InventoryPackageState
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryType
import au.org.libraryforall.updater.inventory.api.InventoryStringResourcesType
import au.org.libraryforall.updater.inventory.api.InventoryType
import au.org.libraryforall.updater.inventory.vanilla.InventoryHashIndexedDirectory
import au.org.libraryforall.updater.repository.api.Hash
import au.org.libraryforall.updater.repository.api.Repository
import au.org.libraryforall.updater.repository.api.RepositoryPackage
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.SettableFuture
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import one.irradia.http.api.HTTPAuthentication
import one.irradia.http.api.HTTPClientType
import one.irradia.http.api.HTTPResult
import one.irradia.http.api.HTTPResult.HTTPFailed.*
import org.joda.time.LocalDateTime
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.slf4j.Logger
import java.io.ByteArrayInputStream
import java.io.File
import java.lang.Exception
import java.net.URI
import java.util.UUID
import java.util.concurrent.Executors

abstract class InventoryContract {

  private lateinit var directory: File
  private lateinit var logger: Logger
  private var executor: ListeningExecutorService? = null

  protected abstract fun logger(): Logger

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
    this.logger = this.logger()
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

  @Test(timeout = 10_000L)
  fun testInstallPackage() {
    val activity =
      Mockito.mock(Activity::class.java)
    val apkInstaller =
      Mockito.mock(APKInstallerType::class.java)
    val http =
      Mockito.mock(HTTPClientType::class.java)

    val httpAuthentication: (URI) -> HTTPAuthentication? = { null }

    val inventory =
      this.inventory(
        resources = InventoryStringResources(),
        executorService = this.executor!!,
        http = http,
        httpAuthentication = httpAuthentication,
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
        sha256 = Hash("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"))

    val repository =
      Repository(
        id = UUID.randomUUID(),
        title = "LFA",
        updated = LocalDateTime(),
        packages = listOf(package0),
        source = URI.create("http://example.com"))

    val inventoryRepository =
      inventory.inventoryRepositoryPut(repository)
    val inventoryPackage =
      inventoryRepository.packages[0]

    Mockito
      .`when`(http.get(
        uri = URI.create("http://www.example.com/0"),
        authentication = httpAuthentication,
        offset = 0L))
      .thenReturn(HTTPResult.HTTPOK(
        URI.create("http://www.example.com/0"),
        contentLength = 5,
        headers = mapOf(),
        message = "OK",
        statusCode = 200,
        result = ByteArrayInputStream("hello".toByteArray())))

    val installTask =
      Mockito.mock(APKInstallTaskType::class.java)

    val installFuture = SettableFuture.create<Int>()
    installFuture.set(-1)

    Mockito.`when`(installTask.future)
      .thenReturn(installFuture)

    Mockito
      .`when`(apkInstaller.createInstallTask(
        activity = activity,
        packageName = package0.id,
        packageVersionCode = package0.versionCode,
        file = File(directory, "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824.apk")))
      .thenReturn(installTask)

    val result =
      inventoryPackage.install(activity)
        .get()

    Assert.assertTrue(
      "All steps succeeded",
      result.steps.all { step ->
        this.logger.debug("step: {} {}", step.description, step.failed)
        !step.failed
      })

    Assert.assertTrue(
      "Package is installed",
      inventoryPackage.state is InventoryPackageState.Installed)
  }

  @Test(timeout = 10_000L)
  fun testInstallPackageServerRejects() {
    val activity =
      Mockito.mock(Activity::class.java)
    val apkInstaller =
      Mockito.mock(APKInstallerType::class.java)
    val http =
      Mockito.mock(HTTPClientType::class.java)

    val httpAuthentication: (URI) -> HTTPAuthentication? = { null }

    val inventory =
      this.inventory(
        resources = InventoryStringResources(),
        executorService = this.executor!!,
        http = http,
        httpAuthentication = httpAuthentication,
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
        sha256 = Hash("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"))

    val repository =
      Repository(
        id = UUID.randomUUID(),
        title = "LFA",
        updated = LocalDateTime(),
        packages = listOf(package0),
        source = URI.create("http://example.com"))

    val inventoryRepository =
      inventory.inventoryRepositoryPut(repository)
    val inventoryPackage =
      inventoryRepository.packages[0]

    Mockito
      .`when`(http.get(
        uri = URI.create("http://www.example.com/0"),
        authentication = httpAuthentication,
        offset = 0L))
      .thenReturn(HTTPError(
        URI.create("http://www.example.com/0"),
        contentLength = 5,
        headers = mapOf(),
        message = "OK",
        statusCode = 404,
        result = ByteArrayInputStream("hello".toByteArray())))

    val result =
      inventoryPackage.install(activity)
        .get()

    Assert.assertTrue(
      "A step failed",
      result.steps.any { step ->
        this.logger.debug("step: {} {}", step.description, step.failed)
        step.failed
      })

    Assert.assertTrue(
      "Package install failed",
      inventoryPackage.state is InventoryPackageState.InstallFailed)
  }

  @Test(timeout = 10_000L)
  fun testInstallPackageServerFails() {
    val activity =
      Mockito.mock(Activity::class.java)
    val apkInstaller =
      Mockito.mock(APKInstallerType::class.java)
    val http =
      Mockito.mock(HTTPClientType::class.java)

    val httpAuthentication: (URI) -> HTTPAuthentication? = { null }

    val inventory =
      this.inventory(
        resources = InventoryStringResources(),
        executorService = this.executor!!,
        http = http,
        httpAuthentication = httpAuthentication,
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
        sha256 = Hash("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"))

    val repository =
      Repository(
        id = UUID.randomUUID(),
        title = "LFA",
        updated = LocalDateTime(),
        packages = listOf(package0),
        source = URI.create("http://example.com"))

    val inventoryRepository =
      inventory.inventoryRepositoryPut(repository)
    val inventoryPackage =
      inventoryRepository.packages[0]

    Mockito
      .`when`(http.get(
        uri = URI.create("http://www.example.com/0"),
        authentication = httpAuthentication,
        offset = 0L))
      .thenReturn(HTTPFailure(
        URI.create("http://www.example.com/0"),
        exception = Exception()))

    val result =
      inventoryPackage.install(activity)
        .get()

    Assert.assertTrue(
      "A step failed",
      result.steps.any { step ->
        this.logger.debug("step: {} {}", step.description, step.failed)
        step.failed
      })

    Assert.assertTrue(
      "Package install failed",
      inventoryPackage.state is InventoryPackageState.InstallFailed)
  }

  @Test(timeout = 10_000L)
  fun testInstallPackageAPKFails() {
    val activity =
      Mockito.mock(Activity::class.java)
    val apkInstaller =
      Mockito.mock(APKInstallerType::class.java)
    val http =
      Mockito.mock(HTTPClientType::class.java)

    val httpAuthentication: (URI) -> HTTPAuthentication? = { null }

    val inventory =
      this.inventory(
        resources = InventoryStringResources(),
        executorService = this.executor!!,
        http = http,
        httpAuthentication = httpAuthentication,
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
        sha256 = Hash("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"))

    val repository =
      Repository(
        id = UUID.randomUUID(),
        title = "LFA",
        updated = LocalDateTime(),
        packages = listOf(package0),
        source = URI.create("http://example.com"))

    val inventoryRepository =
      inventory.inventoryRepositoryPut(repository)
    val inventoryPackage =
      inventoryRepository.packages[0]

    Mockito
      .`when`(http.get(
        uri = URI.create("http://www.example.com/0"),
        authentication = httpAuthentication,
        offset = 0L))
      .thenReturn(HTTPResult.HTTPOK(
        URI.create("http://www.example.com/0"),
        contentLength = 5,
        headers = mapOf(),
        message = "OK",
        statusCode = 200,
        result = ByteArrayInputStream("hello".toByteArray())))

    val installTask =
      Mockito.mock(APKInstallTaskType::class.java)

    val installFuture = SettableFuture.create<Int>()
    installFuture.set(999)

    Mockito.`when`(installTask.future)
      .thenReturn(installFuture)

    Mockito
      .`when`(apkInstaller.createInstallTask(
        activity = activity,
        packageName = package0.id,
        packageVersionCode = package0.versionCode,
        file = File(directory, "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824.apk")))
      .thenReturn(installTask)

    val result =
      inventoryPackage.install(activity)
        .get()

    Assert.assertTrue(
      "A step failed",
      result.steps.any { step ->
        this.logger.debug("step: {} {}", step.description, step.failed)
        step.failed
      })

    Assert.assertTrue(
      "Package install failed",
      inventoryPackage.state is InventoryPackageState.InstallFailed)
  }

  @Test(timeout = 10_000L)
  fun testInstallPackageAPKFailsExceptionally() {
    val activity =
      Mockito.mock(Activity::class.java)
    val apkInstaller =
      Mockito.mock(APKInstallerType::class.java)
    val http =
      Mockito.mock(HTTPClientType::class.java)

    val httpAuthentication: (URI) -> HTTPAuthentication? = { null }

    val inventory =
      this.inventory(
        resources = InventoryStringResources(),
        executorService = this.executor!!,
        http = http,
        httpAuthentication = httpAuthentication,
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
        sha256 = Hash("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"))

    val repository =
      Repository(
        id = UUID.randomUUID(),
        title = "LFA",
        updated = LocalDateTime(),
        packages = listOf(package0),
        source = URI.create("http://example.com"))

    val inventoryRepository =
      inventory.inventoryRepositoryPut(repository)
    val inventoryPackage =
      inventoryRepository.packages[0]

    Mockito
      .`when`(http.get(
        uri = URI.create("http://www.example.com/0"),
        authentication = httpAuthentication,
        offset = 0L))
      .thenReturn(HTTPResult.HTTPOK(
        URI.create("http://www.example.com/0"),
        contentLength = 5,
        headers = mapOf(),
        message = "OK",
        statusCode = 200,
        result = ByteArrayInputStream("hello".toByteArray())))

    val installTask =
      Mockito.mock(APKInstallTaskType::class.java)

    val installFuture = SettableFuture.create<Int>()
    installFuture.setException(Exception())

    Mockito.`when`(installTask.future)
      .thenReturn(installFuture)

    Mockito
      .`when`(apkInstaller.createInstallTask(
        activity = activity,
        packageName = package0.id,
        packageVersionCode = package0.versionCode,
        file = File(directory, "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824.apk")))
      .thenReturn(installTask)

    val result =
      inventoryPackage.install(activity)
        .get()

    Assert.assertTrue(
      "A step failed",
      result.steps.any { step ->
        this.logger.debug("step: {} {}", step.description, step.failed)
        step.failed
      })

    Assert.assertTrue(
      "Package install failed",
      inventoryPackage.state is InventoryPackageState.InstallFailed)
  }
}

