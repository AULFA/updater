package au.org.libraryforall.updater.tests

import au.org.libraryforall.updater.inventory.api.InventoryAPKDirectoryType
import au.org.libraryforall.updater.inventory.api.InventoryAPKDirectoryType.VerificationResult.*
import au.org.libraryforall.updater.inventory.vanilla.InventoryTaskDownload
import au.org.libraryforall.updater.inventory.vanilla.InventoryTaskMonad
import au.org.libraryforall.updater.inventory.vanilla.InventoryTaskMonad.*
import au.org.libraryforall.updater.repository.api.Hash
import com.google.common.util.concurrent.ListeningExecutorService
import one.irradia.http.api.HTTPAuthentication
import one.irradia.http.api.HTTPClientType
import one.irradia.http.api.HTTPResult
import one.irradia.http.api.HTTPResult.HTTPFailed.*
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.slf4j.Logger
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean

abstract class InventoryTaskDownloadContract {

  private lateinit var strings: InventoryStringResources
  private lateinit var http: HTTPClientType
  private lateinit var logger: Logger

  protected abstract fun logger(): Logger

  @Before
  fun setup() {
    this.logger = this.logger()
    this.http = Mockito.mock(HTTPClientType::class.java)
    this.strings = InventoryStringResources()
  }

  @After
  fun tearDown() {

  }

  /**
   * If the initial verification step is cancelled, no download occurs.
   */

  @Test
  fun testDownloadInitialVerificationCancelled() {
    val file =
      File.createTempFile("inventory-", ".apk").absoluteFile

    val reservation =
      Mockito.mock(InventoryAPKDirectoryType.KeyReservationType::class.java)

    Mockito.`when`(reservation.verify(this.anyNonNull()))
      .thenReturn(VerificationCancelled)
    Mockito.`when`(reservation.file)
      .thenReturn(file)

    val task =
      InventoryTaskDownload(
        resources = this.strings,
        http = this.http,
        httpAuthentication = { null },
        reservation = reservation,
        onDownloadProgress = { },
        onVerificationProgress = { },
        uri = URI("urn:x"),
        cancel = AtomicBoolean(false))

    val result = task.execute()
    this.logger.debug("result: {}", result)

    Assert.assertEquals(this.strings.installVerificationCancelled, result.steps[0].resolution)
  }

  /**
   * If the file already exists locally, no download occurs.
   */

  @Test
  fun testDownloadNotNeeded() {
    val file =
      File.createTempFile("inventory-", ".apk").absoluteFile

    val reservation =
      Mockito.mock(InventoryAPKDirectoryType.KeyReservationType::class.java)

    Mockito.`when`(reservation.verify(this.anyNonNull()))
      .thenReturn(VerificationSuccess(file))
    Mockito.`when`(reservation.file)
      .thenReturn(file)

    val task =
      InventoryTaskDownload(
      resources = this.strings,
      http = this.http,
      httpAuthentication = { null },
      reservation = reservation,
      onDownloadProgress = { },
      onVerificationProgress = { },
      uri = URI("urn:x"),
      cancel = AtomicBoolean(false))

    val result = task.execute()
    this.logger.debug("result: {}", result)

    Assert.assertEquals(this.strings.installDownloadNeededNot, result.steps[0].resolution)
  }

  /**
   * If attempting to connect to the server fails, the download fails.
   */

  @Test
  fun testDownloadNeededButFails() {
    val file =
      File.createTempFile("inventory-", ".apk").absoluteFile

    val authentication : (URI) -> HTTPAuthentication? =
      { null }
    val reservation =
      Mockito.mock(InventoryAPKDirectoryType.KeyReservationType::class.java)

    val exception = IOException()

    Mockito.`when`(reservation.verify(this.anyNonNull()))
      .thenReturn(VerificationFailure(""))
    Mockito.`when`(reservation.file)
      .thenReturn(file)
    Mockito.`when`(reservation.hash)
      .thenReturn(Hash("5891b5b522d5df086d0ff0b110fbd9d21bb4fc7163af34d08286a2e846f6be03"))

    Mockito.`when`(this.http.get(URI("urn:x"), authentication, 0L))
      .thenReturn(HTTPFailure(URI("urn:x"), exception))

    val task =
      InventoryTaskDownload(
        resources = this.strings,
        http = this.http,
        httpAuthentication = authentication,
        reservation = reservation,
        onDownloadProgress = { },
        onVerificationProgress = { },
        uri = URI("urn:x"),
        cancel = AtomicBoolean(false))

    val result = task.execute()
    this.logger.debug("result: {}", result)

    Assert.assertEquals(true, result.steps[1].failed)
    Assert.assertEquals(exception, result.steps[1].exception)
  }

  /**
   * If the server returns an error, the download fails.
   */

  @Test
  fun testDownloadServerErrorFails() {
    val file =
      File.createTempFile("inventory-", ".apk").absoluteFile

    val authentication : (URI) -> HTTPAuthentication? =
      { null }
    val reservation =
      Mockito.mock(InventoryAPKDirectoryType.KeyReservationType::class.java)

    Mockito.`when`(reservation.verify(this.anyNonNull()))
      .thenReturn(VerificationFailure(""))
    Mockito.`when`(reservation.file)
      .thenReturn(file)
    Mockito.`when`(reservation.hash)
      .thenReturn(Hash("5891b5b522d5df086d0ff0b110fbd9d21bb4fc7163af34d08286a2e846f6be03"))

    Mockito.`when`(this.http.get(URI("urn:x"), authentication, 0L))
      .thenReturn(HTTPError(
        uri = URI("urn:x"),
        contentLength = 0L,
        headers = mapOf(),
        message = "FAILED!",
        statusCode = 404,
        result = ByteArrayInputStream(ByteArray(0))
      ))

    val task =
      InventoryTaskDownload(
        resources = this.strings,
        http = this.http,
        httpAuthentication = authentication,
        reservation = reservation,
        onDownloadProgress = { },
        onVerificationProgress = { },
        uri = URI("urn:x"),
        cancel = AtomicBoolean(false))

    val result = task.execute()
    this.logger.debug("result: {}", result)

    Assert.assertEquals(true, result.steps[1].failed)
    Assert.assertEquals(
      this.strings.installConnectionServerError(404, "FAILED!", "text/plain", 0L),
      result.steps[1].resolution)
  }

  /**
   * If the download is cancelled, the task is cancelled.
   */

  @Test
  fun testDownloadCancelled() {
    val file =
      File.createTempFile("inventory-", ".apk").absoluteFile

    val authentication : (URI) -> HTTPAuthentication? =
      { null }
    val reservation =
      Mockito.mock(InventoryAPKDirectoryType.KeyReservationType::class.java)

    Mockito.`when`(reservation.verify(this.anyNonNull()))
      .thenReturn(VerificationFailure("167112362adb3b2041c11f7337437872f9d821e57e8c3edd68d87a1d0babd0f5"))
    Mockito.`when`(reservation.file)
      .thenReturn(file)
    Mockito.`when`(reservation.hash)
      .thenReturn(Hash("5891b5b522d5df086d0ff0b110fbd9d21bb4fc7163af34d08286a2e846f6be03"))

    Mockito.`when`(this.http.get(URI("urn:x"), authentication, 0L))
      .thenReturn(HTTPResult.HTTPOK(
        uri = URI("urn:x"),
        contentLength = 0L,
        headers = mapOf(),
        message = "OK",
        statusCode = 200,
        result = ByteArrayInputStream(ByteArray(0))
      ))

    val task =
      InventoryTaskDownload(
        resources = this.strings,
        http = this.http,
        httpAuthentication = authentication,
        reservation = reservation,
        onDownloadProgress = { },
        onVerificationProgress = { },
        uri = URI("urn:x"),
        cancel = AtomicBoolean(true))

    val result = task.execute()
    this.logger.debug("result: {}", result)

    Assert.assertEquals(
      this.strings.installDownloadingCancelled,
      result.steps[2].resolution)
  }

  private fun <T> anyNonNull(): T =
    Mockito.argThat { x -> x != null }
}

