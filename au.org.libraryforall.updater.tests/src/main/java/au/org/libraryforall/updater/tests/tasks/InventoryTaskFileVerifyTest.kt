package au.org.libraryforall.updater.tests.tasks

import au.org.libraryforall.updater.inventory.api.InventoryClockType
import au.org.libraryforall.updater.inventory.api.InventoryProgress
import au.org.libraryforall.updater.inventory.api.InventoryProgressValue
import au.org.libraryforall.updater.inventory.api.InventoryStringResourcesType
import au.org.libraryforall.updater.inventory.vanilla.tasks.InventoryTaskExecutionType
import au.org.libraryforall.updater.inventory.vanilla.tasks.InventoryTaskFileVerify
import au.org.libraryforall.updater.inventory.vanilla.tasks.InventoryTaskFileVerify.Verification.FileHashDidNotMatch
import au.org.libraryforall.updater.inventory.vanilla.tasks.InventoryTaskFileVerify.Verification.FileHashMatched
import au.org.libraryforall.updater.inventory.vanilla.tasks.InventoryTaskResult
import au.org.libraryforall.updater.repository.api.Hash
import au.org.libraryforall.updater.services.api.ServiceDirectoryType
import au.org.libraryforall.updater.tests.InventoryStringResources
import au.org.libraryforall.updater.tests.MockClock
import au.org.libraryforall.updater.tests.MutableServiceDirectory
import au.org.libraryforall.updater.tests.TestDirectories
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean

class InventoryTaskFileVerifyTest {

  private lateinit var clock: MockClock
  private lateinit var executionContext: InventoryTaskExecutionType
  private lateinit var cancelled: AtomicBoolean
  private lateinit var progress: MutableList<InventoryProgress>
  private lateinit var serviceDirectory: MutableServiceDirectory
  private lateinit var tempDir: File

  @Before
  fun testSetup() {
    this.tempDir = TestDirectories.temporaryDirectory()
    this.progress = mutableListOf()
    this.clock = MockClock()

    this.serviceDirectory = MutableServiceDirectory()
    this.serviceDirectory.registerService(
      serviceClass = InventoryStringResourcesType::class.java,
      service = InventoryStringResources())
    this.serviceDirectory.registerService(
      serviceClass = InventoryClockType::class.java,
      service = this.clock)

    this.cancelled = AtomicBoolean(false)

    this.executionContext =
      object : InventoryTaskExecutionType {
        override val services: ServiceDirectoryType
          get() = this@InventoryTaskFileVerifyTest.serviceDirectory
        override val isCancelRequested: Boolean
          get() = this@InventoryTaskFileVerifyTest.cancelled.get()
        override val onProgress: (InventoryProgress) -> Unit
          get() = { progress -> this@InventoryTaskFileVerifyTest.progress.add(progress) }
      }
  }

  /**
   * Verifying a file works.
   */

  @Test
  fun testVerifyOK() {
    val outputFile = File(this.tempDir, "0.apk")
    outputFile.writeText("Hello.")

    val result =
      InventoryTaskFileVerify.create(
        file = outputFile,
        hash = Hash("2d8bd7d9bb5f85ba643f0110d50cb506a1fe439e769a22503193ea6046bb87f7")
      ).evaluate(this.executionContext)

    val resultT =
      result as InventoryTaskResult.InventoryTaskSucceeded

    Assert.assertEquals(
      FileHashMatched(
        hashExpected = Hash("2d8bd7d9bb5f85ba643f0110d50cb506a1fe439e769a22503193ea6046bb87f7"),
        hashReceived = Hash("2d8bd7d9bb5f85ba643f0110d50cb506a1fe439e769a22503193ea6046bb87f7")
      ),
      resultT.result)

    run {
      val inventoryProgress = this.progress.removeAt(0)
      Assert.assertEquals(
        null,
        inventoryProgress.major)
      Assert.assertEquals(
        InventoryProgressValue.InventoryProgressValueDefinite(6L, 6L, 6L),
        inventoryProgress.minor)
      Assert.assertEquals(
        "installVerifyingLocalFileProgress 6 6 6",
        inventoryProgress.status)
    }

    Assert.assertEquals(0, this.progress.size)
  }

  /**
   * Verifying a file works.
   */

  @Test
  fun testVerifyOKFailing() {
    val outputFile = File(this.tempDir, "0.apk")
    outputFile.writeText("Hello.")

    val result =
      InventoryTaskFileVerify.createFailing(
        file = outputFile,
        hash = Hash("2d8bd7d9bb5f85ba643f0110d50cb506a1fe439e769a22503193ea6046bb87f7")
      ).evaluate(this.executionContext)

    val resultT =
      result as InventoryTaskResult.InventoryTaskSucceeded

    Assert.assertEquals(
      FileHashMatched(
        hashExpected = Hash("2d8bd7d9bb5f85ba643f0110d50cb506a1fe439e769a22503193ea6046bb87f7"),
        hashReceived = Hash("2d8bd7d9bb5f85ba643f0110d50cb506a1fe439e769a22503193ea6046bb87f7")
      ),
      resultT.result)

    run {
      val inventoryProgress = this.progress.removeAt(0)
      Assert.assertEquals(
        null,
        inventoryProgress.major)
      Assert.assertEquals(
        InventoryProgressValue.InventoryProgressValueDefinite(6L, 6L, 6L),
        inventoryProgress.minor)
      Assert.assertEquals(
        "installVerifyingLocalFileProgress 6 6 6",
        inventoryProgress.status)
    }

    Assert.assertEquals(0, this.progress.size)
  }

  /**
   * Verifying a file fails as expected.
   */

  @Test
  fun testVerifyFails() {
    val outputFile = File(this.tempDir, "0.apk")
    outputFile.writeText("Hello ex.")

    val result =
      InventoryTaskFileVerify.create(
        file = outputFile,
        hash = Hash("2d8bd7d9bb5f85ba643f0110d50cb506a1fe439e769a22503193ea6046bb87f7")
      ).evaluate(this.executionContext)

    val resultT =
      result as InventoryTaskResult.InventoryTaskSucceeded

    Assert.assertEquals(
      FileHashDidNotMatch(
        hashExpected = Hash("2d8bd7d9bb5f85ba643f0110d50cb506a1fe439e769a22503193ea6046bb87f7"),
        hashReceived = Hash("35514dabff674e92dcb50380047b415d5a0bcaa037a9f436ea510eba1a61aba5")
      ),
      resultT.result)

    run {
      val inventoryProgress = this.progress.removeAt(0)
      Assert.assertEquals(
        null,
        inventoryProgress.major)
      Assert.assertEquals(
        InventoryProgressValue.InventoryProgressValueDefinite(9L, 9L, 9L),
        inventoryProgress.minor)
      Assert.assertEquals(
        "installVerifyingLocalFileProgress 9 9 9",
        inventoryProgress.status)
    }

    Assert.assertEquals(0, this.progress.size)
  }

  /**
   * Verifying a file fails as expected.
   */

  @Test
  fun testVerifyFailsFailure() {
    val outputFile = File(this.tempDir, "0.apk")
    outputFile.writeText("Hello ex.")

    val result =
      InventoryTaskFileVerify.createFailing(
        file = outputFile,
        hash = Hash("2d8bd7d9bb5f85ba643f0110d50cb506a1fe439e769a22503193ea6046bb87f7")
      ).evaluate(this.executionContext)

    val resultT =
      result as InventoryTaskResult.InventoryTaskFailed

    run {
      val inventoryProgress = this.progress.removeAt(0)
      Assert.assertEquals(
        null,
        inventoryProgress.major)
      Assert.assertEquals(
        InventoryProgressValue.InventoryProgressValueDefinite(9L, 9L, 9L),
        inventoryProgress.minor)
      Assert.assertEquals(
        "installVerifyingLocalFileProgress 9 9 9",
        inventoryProgress.status)
    }

    Assert.assertEquals(0, this.progress.size)
  }

  /**
   * Verifying a file can be cancelled.
   */

  @Test
  fun testVerifyCancelled() {
    val outputFile = File(this.tempDir, "0.apk")
    outputFile.writeText("Hello ex.")

    this.cancelled.set(true)

    val result =
      InventoryTaskFileVerify.create(
        file = outputFile,
        hash = Hash("2d8bd7d9bb5f85ba643f0110d50cb506a1fe439e769a22503193ea6046bb87f7")
      ).evaluate(this.executionContext)

    val resultT =
      result as InventoryTaskResult.InventoryTaskCancelled

    run {
      val inventoryProgress = this.progress.removeAt(0)
      Assert.assertEquals(
        null,
        inventoryProgress.major)
      Assert.assertEquals(
        InventoryProgressValue.InventoryProgressValueDefinite(9L, 9L, 9L),
        inventoryProgress.minor)
      Assert.assertEquals(
        "installVerifyingLocalFileProgress 9 9 9",
        inventoryProgress.status)
    }

    Assert.assertEquals(0, this.progress.size)
  }

  /**
   * Verifying a file can be cancelled.
   */

  @Test
  fun testVerifyCancelledFailing() {
    val outputFile = File(this.tempDir, "0.apk")
    outputFile.writeText("Hello ex.")

    this.cancelled.set(true)

    val result =
      InventoryTaskFileVerify.createFailing(
        file = outputFile,
        hash = Hash("2d8bd7d9bb5f85ba643f0110d50cb506a1fe439e769a22503193ea6046bb87f7")
      ).evaluate(this.executionContext)

    val resultT =
      result as InventoryTaskResult.InventoryTaskCancelled

    run {
      val inventoryProgress = this.progress.removeAt(0)
      Assert.assertEquals(
        null,
        inventoryProgress.major)
      Assert.assertEquals(
        InventoryProgressValue.InventoryProgressValueDefinite(9L, 9L, 9L),
        inventoryProgress.minor)
      Assert.assertEquals(
        "installVerifyingLocalFileProgress 9 9 9",
        inventoryProgress.status)
    }

    Assert.assertEquals(0, this.progress.size)
  }

  private fun resource(name: String): InputStream {
    return InventoryTaskFileVerifyTest::class.java.getResourceAsStream(
      "/au/org/libraryforall/updater/tests/${name}")!!
  }
}