package au.org.libraryforall.updater.tests

import au.org.libraryforall.updater.inventory.api.InventoryAPKDirectoryType
import au.org.libraryforall.updater.inventory.vanilla.InventoryAPKDirectory
import au.org.libraryforall.updater.inventory.vanilla.InventoryTaskDownload
import au.org.libraryforall.updater.inventory.vanilla.InventoryTaskMonad
import au.org.libraryforall.updater.repository.api.Hash
import fi.iki.elonen.NanoHTTPD
import one.irradia.http.api.HTTPClientType
import one.irradia.http.vanilla.HTTPClientsOkHTTP
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.slf4j.Logger
import java.io.File
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean

abstract class InventoryTaskDownloadIntegrationContract {

  private lateinit var apkDirectory: InventoryAPKDirectoryType
  private lateinit var baseDirectory: File
  private lateinit var nano: NanoHTTPD
  private lateinit var strings: InventoryStringResources
  private lateinit var http: HTTPClientType
  private lateinit var logger: Logger

  protected abstract fun logger(): Logger

  @Before
  fun setup() {
    this.logger = this.logger()
    this.strings = InventoryStringResources()
    this.http = HTTPClientsOkHTTP().createClient()
    this.baseDirectory = File.createTempFile("inventory-apk-", "dir")
    this.baseDirectory.delete()
    this.baseDirectory.mkdirs()
    this.apkDirectory = InventoryAPKDirectory.create(this.baseDirectory)
  }

  @After
  fun tearDown() {
    this.nano.stop()
  }

  @Test
  fun testDownloadIntegration() {
    this.nano = object : NanoHTTPD(10_000) {
      override fun serve(
        uri: String?,
        method: Method?,
        headers: MutableMap<String, String>?,
        parms: MutableMap<String, String>?,
        files: MutableMap<String, String>?
      ): Response {
        return newFixedLengthResponse("hello")
      }
    }
    this.nano.start()

    val result =
      this.apkDirectory.withKey(
        Hash("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"))
      { reservation ->
        val task = InventoryTaskDownload(
          resources = this.strings,
          http = this.http,
          httpAuthentication = { null },
          reservation = reservation,
          onVerificationProgress = { },
          onDownloadProgress = { },
          uri = URI.create("http://localhost:10000/file.txt"),
          cancel = AtomicBoolean(false))

        val result = task.execute()
        Assert.assertEquals("hello", reservation.file.readText())
        result
      }

    Assert.assertEquals(
      InventoryTaskMonad.InventoryTaskSuccess::class.java,
      result.javaClass)
  }
}

