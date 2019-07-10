package au.org.libraryforall.updater.tests

import au.org.libraryforall.updater.inventory.api.InventoryAPKDirectoryThrottledVerificationReceiver
import au.org.libraryforall.updater.inventory.api.InventoryAPKDirectoryType
import au.org.libraryforall.updater.inventory.api.InventoryAPKDirectoryType.ReservationUnavailableException
import au.org.libraryforall.updater.inventory.api.InventoryAPKDirectoryType.VerificationProgressType
import au.org.libraryforall.updater.inventory.api.InventoryAPKDirectoryType.VerificationResult
import au.org.libraryforall.updater.repository.api.Hash
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.slf4j.Logger
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

abstract class InventoryAPKDirectoryContract {

  private var executor: ExecutorService? = null
  private lateinit var directory: File
  private lateinit var logger: Logger

  protected abstract fun hashIndexedDirectory(
    directory: File): InventoryAPKDirectoryType

  protected abstract fun logger(): Logger

  @JvmField
  @Rule
  val expectedException : ExpectedException = ExpectedException.none()

  @Before
  fun setup() {
    this.logger = this.logger()
    this.directory = File.createTempFile("inventory-hash-indexed", "dir")
    this.directory.delete()
    this.directory.mkdirs()
    this.executor = Executors.newFixedThreadPool(4)
  }

  @After
  fun tearDown() {
    this.executor?.shutdown()
  }

  @Test
  fun testEmpty() {
    val index = this.hashIndexedDirectory(this.directory)
    val hash = Hash("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")

    index.withKey(hash) { reservation ->
      Assert.assertTrue("File does not exist", !reservation.file.exists())
    }
  }

  @Test
  fun testDoubleReservation() {
    val index = this.hashIndexedDirectory(this.directory)
    val hash = Hash("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")

    val exec = this.executor!!
    val latch = CountDownLatch(1)

    exec.execute {
      index.withKey(hash) { reservation ->
        latch.countDown()
        Thread.sleep(5000L)
      }
    }

    latch.await()
    this.expectedException.expect(ReservationUnavailableException::class.java)
    index.withKey(hash) { reservation ->
      Assert.fail()
    }
  }

  @Test
  fun testVerifyMissing() {
    val index = this.hashIndexedDirectory(this.directory)
    val hash = Hash("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")

    this.expectedException.expect(FileNotFoundException::class.java)
    index.withKey(hash) { reservation ->
      val result = reservation.verify { progress ->
        this.logger.debug("{}/{}", progress.currentBytes, progress.maximumBytes)
      }
      Assert.fail()
    }
  }

  @Test
  fun testVerifyOK() {
    val index = this.hashIndexedDirectory(this.directory)
    val hash = Hash("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824")

    index.withKey(hash) { reservation ->
      FileOutputStream(reservation.file).use {stream ->
        stream.write("hello".toByteArray())
        stream.flush()

        val result = reservation.verify { progress ->
          this.logger.debug("{}/{}", progress.currentBytes, progress.maximumBytes)
        }
        Assert.assertEquals(result, VerificationResult.VerificationSuccess(reservation.file))
      }
    }
  }

  @Test
  fun testVerifyCancelled() {
    val index = this.hashIndexedDirectory(this.directory)
    val hash = Hash("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824")

    index.withKey(hash) { reservation ->
      FileOutputStream(reservation.file).use {stream ->
        stream.write("hello".toByteArray())
        stream.flush()

        val result = reservation.verify { progress ->
          this.logger.debug("{}/{}", progress.currentBytes, progress.maximumBytes)
          progress.cancel()
        }
        Assert.assertEquals(result, VerificationResult.VerificationCancelled)
      }
    }
  }

  @Test
  fun testVerifyFailed() {
    val index = this.hashIndexedDirectory(this.directory)
    val hash = Hash("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824")

    index.withKey(hash) { reservation ->
      FileOutputStream(reservation.file).use {stream ->
        stream.write("hello2".toByteArray())
        stream.flush()

        val result = reservation.verify { progress ->
          this.logger.debug("{}/{}", progress.currentBytes, progress.maximumBytes)
        }
        Assert.assertEquals(
          result,
          VerificationResult.VerificationFailure("87298cc2f31fba73181ea2a9e6ef10dce21ed95e98bdac9c4e1504ea16f486e4"))
      }
    }
  }

  @Test
  fun testClear() {
    val index = this.hashIndexedDirectory(this.directory)

    val hashes =
      listOf<Hash>(
        Hash("5a936ee19a0cf3c70d8cb0006111b7a52f45ec01703e0af8cdc8c6d81ac5850c"),
        Hash("91e9240f415223982edc345532630710e94a7f52cd5f48f5ee1afc555078f0ab"),
        Hash("87298cc2f31fba73181ea2a9e6ef10dce21ed95e98bdac9c4e1504ea16f486e4"))

    val data =
      listOf<String>(
        "hello0",
        "hello1",
        "hello2")

    for (dataIndex in 0 until 3) {
      val hash = hashes[dataIndex]
      val data = data[dataIndex]

      index.withKey(hash) { reservation ->
        FileOutputStream(reservation.file).use { stream ->
          stream.write(data.toByteArray())
          stream.flush()
        }
      }
    }

    val deletions = index.clear()
    Assert.assertEquals(3, deletions.size)
    Assert.assertEquals(hashes.toSet(), deletions.map { deletion -> deletion.hash }.toSet())
  }

  @Test(timeout = 20_000L)
  fun testThrottledReceiver10() {
    this.runThrottledReceiver(10, AtomicBoolean(false))
  }

  @Test(timeout = 20_000L)
  fun testThrottledReceiver100() {
    this.runThrottledReceiver(100, AtomicBoolean(false))
  }

  @Test(timeout = 20_000L)
  fun testThrottledReceiverCancel() {
    this.runThrottledReceiver(100, AtomicBoolean(true))
  }

  private fun runThrottledReceiver(approximateCalls: Int, cancel: AtomicBoolean) {
    var calls = 0
    val receiver =
      InventoryAPKDirectoryThrottledVerificationReceiver({ progress ->
        this.logger.debug("({}) {}/{}", approximateCalls, progress.currentBytes, progress.maximumBytes)
        ++calls
      }, cancel)

    var cancelled = false
    for (i in 0L..1000L) {
      if (cancelled) {
        break
      }

      Thread.sleep(10L)
      receiver.invoke(object: VerificationProgressType {
        override val currentBytes: Long = i
        override val maximumBytes: Long = 1000L
        override fun cancel() {
          cancelled = true
        }
      })
    }

    if (cancelled) {
      Assert.assertTrue(
        "Must have received 0 calls (${calls})",
        calls == 0)
      return
    }

    Assert.assertTrue(
      "Must have received > 0 calls (${calls})",
      calls > 0)
    Assert.assertTrue(
      "Must have received < ${approximateCalls} calls (${calls})",
      calls <= approximateCalls)
  }
}