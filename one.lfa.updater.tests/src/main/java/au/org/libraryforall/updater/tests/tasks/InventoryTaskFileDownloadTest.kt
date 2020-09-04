package au.org.libraryforall.updater.tests.tasks

import one.lfa.updater.services.api.ServiceDirectoryType
import au.org.libraryforall.updater.tests.InventoryStringResources
import au.org.libraryforall.updater.tests.MockClock
import au.org.libraryforall.updater.tests.MockHTTP
import au.org.libraryforall.updater.tests.TestDirectories
import one.irradia.http.api.HTTPAuthentication
import one.irradia.http.api.HTTPClientType
import one.irradia.http.api.HTTPResult
import one.lfa.updater.inventory.api.InventoryClockType
import one.lfa.updater.inventory.api.InventoryHTTPAuthenticationType
import one.lfa.updater.inventory.api.InventoryHTTPConfigurationType
import one.lfa.updater.inventory.api.InventoryProgress
import one.lfa.updater.inventory.api.InventoryProgressValue
import one.lfa.updater.inventory.api.InventoryStringResourcesType
import one.lfa.updater.inventory.vanilla.tasks.InventoryTaskExecutionType
import one.lfa.updater.inventory.vanilla.tasks.InventoryTaskFileDownload
import one.lfa.updater.inventory.vanilla.tasks.InventoryTaskFileDownloadRequest
import one.lfa.updater.inventory.vanilla.tasks.InventoryTaskResult
import one.lfa.updater.repository.api.Hash
import one.lfa.updater.services.api.ServiceDirectory
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean

class InventoryTaskFileDownloadTest {

  private lateinit var httpConfiguration: InventoryHTTPConfigurationType
  private lateinit var clock: MockClock
  private lateinit var httpAuth: InventoryHTTPAuthenticationType
  private lateinit var mockHttp: MockHTTP
  private lateinit var executionContext: InventoryTaskExecutionType
  private lateinit var cancelled: AtomicBoolean
  private lateinit var progress: MutableList<InventoryProgress>
  private lateinit var serviceDirectory: ServiceDirectoryType
  private lateinit var tempDir: File
  private lateinit var fileHash: Hash

  @Before
  fun testSetup() {
    this.tempDir = TestDirectories.temporaryDirectory()
    this.progress = mutableListOf()
    this.mockHttp = MockHTTP()
    this.clock = MockClock()
    this.httpAuth = object : InventoryHTTPAuthenticationType {
      override fun authenticationFor(uri: URI): HTTPAuthentication? {
        return null
      }
    }
    this.httpConfiguration = object : InventoryHTTPConfigurationType {
      override val retryCount: Int
        get() = 1
      override val retryDelaySeconds: Long
        get() = 1L
    }

    val services = ServiceDirectory.builder()
    services.addService(
      interfaceType = InventoryStringResourcesType::class.java,
      service = InventoryStringResources())
    services.addService(
      interfaceType = HTTPClientType::class.java,
      service = this.mockHttp)
    services.addService(
      interfaceType = InventoryHTTPAuthenticationType::class.java,
      service = this.httpAuth)
    services.addService(
      interfaceType = InventoryClockType::class.java,
      service = this.clock)
    services.addService(
      interfaceType = InventoryHTTPConfigurationType::class.java,
      service = this.httpConfiguration)
    this.serviceDirectory = services.build()

    this.cancelled = AtomicBoolean(false)

    this.executionContext =
      object : InventoryTaskExecutionType {
        override val services: ServiceDirectoryType
          get() = this@InventoryTaskFileDownloadTest.serviceDirectory
        override val isCancelRequested: Boolean
          get() = this@InventoryTaskFileDownloadTest.cancelled.get()
        override val onProgress: (InventoryProgress) -> Unit
          get() = { progress -> this@InventoryTaskFileDownloadTest.progress.add(progress) }
      }

    this.fileHash = Hash("2d8bd7d9bb5f85ba643f0110d50cb506a1fe439e769a22503193ea6046bb87f7")
  }

  /**
   * Downloading a simple file with no errors works.
   */

  @Test
  fun testDownloadOk() {
    this.mockHttp.addResponse(
      URI.create("http://www.example.com/0.apk"),
      HTTPResult.HTTPOK(
        URI.create("http://www.example.com/0.apk"),
        7L,
        mapOf(),
        "OK",
        200,
        this.resource("hello.txt")
      ))
    this.mockHttp.addResponse(
      URI.create("http://www.example.com/0.apk"),
      HTTPResult.HTTPOK(
        URI.create("http://www.example.com/0.apk"),
        7L,
        mapOf(),
        "OK",
        200,
        this.resource("hello.txt")
      ))

    val outputFile =
      File(this.tempDir, "0.apk")

    val request =
      InventoryTaskFileDownloadRequest(
        progressMajor = null,
        uri = URI.create("http://www.example.com/0.apk"),
        retries = 1,
        outputFile = outputFile,
        expectedHash = fileHash)

    val result =
      InventoryTaskFileDownload.create(request)
        .evaluate(this.executionContext)

    val resultT =
      result as InventoryTaskResult.InventoryTaskSucceeded

    Assert.assertEquals(outputFile, resultT.result)
    Assert.assertEquals("Hello.", outputFile.readText())

    run {
      val inventoryProgress = this.progress.removeAt(0)
      Assert.assertEquals(
        null,
        inventoryProgress.major)
      Assert.assertEquals(
        InventoryProgressValue.InventoryProgressValueDefinite(6L, 6L, 7L),
        inventoryProgress.minor)
      Assert.assertEquals(
        "downloadingHTTPProgress",
        inventoryProgress.status)
    }

    Assert.assertEquals(0, this.progress.size)
  }

  /**
   * If a server returns an error, retrying works.
   */

  @Test
  fun testDownloadRetry0() {
    this.mockHttp.addResponse(
      URI.create("http://www.example.com/0.apk"),
      HTTPResult.HTTPOK(
        URI.create("http://www.example.com/0.apk"),
        7L,
        mapOf(),
        "OK",
        200,
        this.resource("hello.txt")
      ))
    this.mockHttp.addResponse(
      URI.create("http://www.example.com/0.apk"),
      HTTPResult.HTTPFailed.HTTPError(
        URI.create("http://www.example.com/0.apk"),
        0L,
        mapOf(),
        "OK",
        500,
        ByteArrayInputStream(ByteArray(0))
      ))

    this.mockHttp.addResponse(
      URI.create("http://www.example.com/0.apk"),
      HTTPResult.HTTPOK(
        URI.create("http://www.example.com/0.apk"),
        7L,
        mapOf(),
        "OK",
        200,
        this.resource("hello.txt")
      ))
    this.mockHttp.addResponse(
      URI.create("http://www.example.com/0.apk"),
      HTTPResult.HTTPFailed.HTTPError(
        URI.create("http://www.example.com/0.apk"),
        0L,
        mapOf(),
        "OK",
        500,
        ByteArrayInputStream(ByteArray(0))
      ))

    this.mockHttp.addResponse(
      URI.create("http://www.example.com/0.apk"),
      HTTPResult.HTTPOK(
        URI.create("http://www.example.com/0.apk"),
        7L,
        mapOf(),
        "OK",
        200,
        this.resource("hello.txt")
      ))
    this.mockHttp.addResponse(
      URI.create("http://www.example.com/0.apk"),
      HTTPResult.HTTPOK(
        URI.create("http://www.example.com/0.apk"),
        7L,
        mapOf(),
        "OK",
        200,
        this.resource("hello.txt")
      ))

    val outputFile =
      File(this.tempDir, "0.apk")

    val request =
      InventoryTaskFileDownloadRequest(
        progressMajor = null,
        uri = URI.create("http://www.example.com/0.apk"),
        retries = 3,
        outputFile = outputFile,
        expectedHash = fileHash)

    val result =
      InventoryTaskFileDownload.create(request)
        .evaluate(this.executionContext)

    val resultT =
      result as InventoryTaskResult.InventoryTaskSucceeded

    Assert.assertEquals(outputFile, resultT.result)
    Assert.assertEquals("Hello.", outputFile.readText())

    run {
      val inventoryProgress = this.progress.removeAt(0)
      Assert.assertEquals(
        null,
        inventoryProgress.major)
      Assert.assertEquals(
        InventoryProgressValue.InventoryProgressValueDefinite(0, 1L, 1L),
        inventoryProgress.minor)
      Assert.assertEquals(
        "downloadingHTTPRetryingInSeconds 1 1 3",
        inventoryProgress.status)
    }

    run {
      val inventoryProgress = this.progress.removeAt(0)
      Assert.assertEquals(
        null,
        inventoryProgress.major)
      Assert.assertEquals(
        InventoryProgressValue.InventoryProgressValueDefinite(0, 1L, 1L),
        inventoryProgress.minor)
      Assert.assertEquals(
        "downloadingHTTPRetryingInSeconds 1 2 3",
        inventoryProgress.status)
    }

    run {
      val inventoryProgress = this.progress.removeAt(0)
      Assert.assertEquals(
        null,
        inventoryProgress.major)
      Assert.assertEquals(
        InventoryProgressValue.InventoryProgressValueDefinite(6L, 6L, 7L),
        inventoryProgress.minor)
      Assert.assertEquals(
        "downloadingHTTPProgress",
        inventoryProgress.status)
    }

    Assert.assertEquals(0, this.progress.size)
  }

  /**
   * If a server returns an error, retrying works.
   */

  @Test
  fun testDownloadRetry1() {
    this.mockHttp.addResponse(
      URI.create("http://www.example.com/0.apk"),
      HTTPResult.HTTPOK(
        URI.create("http://www.example.com/0.apk"),
        7L,
        mapOf(),
        "OK",
        200,
        this.resource("hello.txt")
      ))
    this.mockHttp.addResponse(
      URI.create("http://www.example.com/0.apk"),
      HTTPResult.HTTPFailed.HTTPFailure(
        URI.create("http://www.example.com/0.apk"),
        IOException()
      ))

    this.mockHttp.addResponse(
      URI.create("http://www.example.com/0.apk"),
      HTTPResult.HTTPOK(
        URI.create("http://www.example.com/0.apk"),
        7L,
        mapOf(),
        "OK",
        200,
        this.resource("hello.txt")
      ))
    this.mockHttp.addResponse(
      URI.create("http://www.example.com/0.apk"),
      HTTPResult.HTTPFailed.HTTPFailure(
        URI.create("http://www.example.com/0.apk"),
        IOException()
      ))

    this.mockHttp.addResponse(
      URI.create("http://www.example.com/0.apk"),
      HTTPResult.HTTPOK(
        URI.create("http://www.example.com/0.apk"),
        7L,
        mapOf(),
        "OK",
        200,
        this.resource("hello.txt")
      ))
    this.mockHttp.addResponse(
      URI.create("http://www.example.com/0.apk"),
      HTTPResult.HTTPOK(
        URI.create("http://www.example.com/0.apk"),
        7L,
        mapOf(),
        "OK",
        200,
        this.resource("hello.txt")
      ))

    val outputFile =
      File(this.tempDir, "0.apk")

    val request =
      InventoryTaskFileDownloadRequest(
        progressMajor = null,
        uri = URI.create("http://www.example.com/0.apk"),
        retries = 3,
        outputFile = outputFile,
        expectedHash = fileHash)

    val result =
      InventoryTaskFileDownload.create(request)
        .evaluate(this.executionContext)

    val resultT =
      result as InventoryTaskResult.InventoryTaskSucceeded

    Assert.assertEquals(outputFile, resultT.result)
    Assert.assertEquals("Hello.", outputFile.readText())

    run {
      val inventoryProgress = this.progress.removeAt(0)
      Assert.assertEquals(
        null,
        inventoryProgress.major)
      Assert.assertEquals(
        InventoryProgressValue.InventoryProgressValueDefinite(0, 1L, 1L),
        inventoryProgress.minor)
      Assert.assertEquals(
        "downloadingHTTPRetryingInSeconds 1 1 3",
        inventoryProgress.status)
    }

    run {
      val inventoryProgress = this.progress.removeAt(0)
      Assert.assertEquals(
        null,
        inventoryProgress.major)
      Assert.assertEquals(
        InventoryProgressValue.InventoryProgressValueDefinite(0, 1L, 1L),
        inventoryProgress.minor)
      Assert.assertEquals(
        "downloadingHTTPRetryingInSeconds 1 2 3",
        inventoryProgress.status)
    }

    run {
      val inventoryProgress = this.progress.removeAt(0)
      Assert.assertEquals(
        null,
        inventoryProgress.major)
      Assert.assertEquals(
        InventoryProgressValue.InventoryProgressValueDefinite(6L, 6L, 7L),
        inventoryProgress.minor)
      Assert.assertEquals(
        "downloadingHTTPProgress",
        inventoryProgress.status)
    }

    Assert.assertEquals(0, this.progress.size)
  }

  /**
   * If a server returns an error, retrying works.
   */

  @Test
  fun testDownloadRetry2() {
    this.mockHttp.addResponse(
      URI.create("http://www.example.com/0.apk"),
      HTTPResult.HTTPFailed.HTTPFailure(
        URI.create("http://www.example.com/0.apk"),
        IOException()
      ))

    this.mockHttp.addResponse(
      URI.create("http://www.example.com/0.apk"),
      HTTPResult.HTTPOK(
        URI.create("http://www.example.com/0.apk"),
        7L,
        mapOf(),
        "OK",
        200,
        this.resource("hello.txt")
      ))

    this.mockHttp.addResponse(
      URI.create("http://www.example.com/0.apk"),
      HTTPResult.HTTPOK(
        URI.create("http://www.example.com/0.apk"),
        7L,
        mapOf(),
        "OK",
        200,
        this.resource("hello.txt")
      ))

    val outputFile =
      File(this.tempDir, "0.apk")

    val request =
      InventoryTaskFileDownloadRequest(
        progressMajor = null,
        uri = URI.create("http://www.example.com/0.apk"),
        retries = 3,
        outputFile = outputFile,
        expectedHash = fileHash)

    val result =
      InventoryTaskFileDownload.create(request)
        .evaluate(this.executionContext)

    val resultT =
      result as InventoryTaskResult.InventoryTaskSucceeded

    Assert.assertEquals(outputFile, resultT.result)
    Assert.assertEquals("Hello.", outputFile.readText())

    run {
      val inventoryProgress = this.progress.removeAt(0)
      Assert.assertEquals(
        null,
        inventoryProgress.major)
      Assert.assertEquals(
        InventoryProgressValue.InventoryProgressValueDefinite(0, 1L, 1L),
        inventoryProgress.minor)
      Assert.assertEquals(
        "downloadingHTTPRetryingInSeconds 1 1 3",
        inventoryProgress.status)
    }

    run {
      val inventoryProgress = this.progress.removeAt(0)
      Assert.assertEquals(
        null,
        inventoryProgress.major)
      Assert.assertEquals(
        InventoryProgressValue.InventoryProgressValueDefinite(6L, 6L, 7L),
        inventoryProgress.minor)
      Assert.assertEquals(
        "downloadingHTTPProgress",
        inventoryProgress.status)
    }

    Assert.assertEquals(0, this.progress.size)
  }

  /**
   * Cancelling the download works.
   */

  @Test
  fun testDownloadCancel() {
    this.mockHttp.addResponse(
      URI.create("http://www.example.com/0.apk"),
      HTTPResult.HTTPOK(
        URI.create("http://www.example.com/0.apk"),
        7L,
        mapOf(),
        "OK",
        200,
        this.resource("hello.txt")
      ))
    this.mockHttp.addResponse(
      URI.create("http://www.example.com/0.apk"),
      HTTPResult.HTTPOK(
        URI.create("http://www.example.com/0.apk"),
        7L,
        mapOf(),
        "OK",
        200,
        this.resource("hello.txt")
      ))

    val outputFile =
      File(this.tempDir, "0.apk")

    val request =
      InventoryTaskFileDownloadRequest(
        progressMajor = null,
        uri = URI.create("http://www.example.com/0.apk"),
        retries = 1,
        outputFile = outputFile,
        expectedHash = fileHash)

    this.cancelled.set(true)

    val result =
      InventoryTaskFileDownload.create(request)
        .evaluate(this.executionContext)

    val resultT =
      result as InventoryTaskResult.InventoryTaskCancelled

    Assert.assertEquals(0, this.progress.size)
  }

  private fun resource(name: String): InputStream {
    return InventoryTaskFileDownloadTest::class.java.getResourceAsStream(
      "/au/org/libraryforall/updater/tests/${name}")!!
  }
}