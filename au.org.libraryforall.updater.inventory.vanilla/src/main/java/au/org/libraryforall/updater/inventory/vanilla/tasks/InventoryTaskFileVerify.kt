package au.org.libraryforall.updater.inventory.vanilla.tasks

import au.org.libraryforall.updater.inventory.api.InventoryClockType
import au.org.libraryforall.updater.inventory.api.InventoryProgress
import au.org.libraryforall.updater.inventory.api.InventoryProgressValue
import au.org.libraryforall.updater.inventory.api.InventoryStringResourcesType
import au.org.libraryforall.updater.inventory.api.InventoryTaskStep
import au.org.libraryforall.updater.inventory.vanilla.Hex
import au.org.libraryforall.updater.inventory.vanilla.UnitsPerSecond
import au.org.libraryforall.updater.repository.api.Hash
import com.google.common.base.Preconditions
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/**
 * A task that verifies that a file has a given hash.
 */

object InventoryTaskFileVerify {

  private val logger = LoggerFactory.getLogger(InventoryTaskFileVerify::class.java)

  /**
   * The result of verifying a file.
   */

  sealed class Verification {

    /**
     * The file hashes matched.
     */

    data class FileHashMatched(
      val hashExpected: Hash,
      val hashReceived: Hash
    ) : Verification() {
      init {
        Preconditions.checkState(
          this.hashExpected.text == this.hashReceived.text,
          "${this.hashExpected.text} == ${this.hashReceived.text}"
        )
      }
    }

    /**
     * The file hashes did not match.
     */

    data class FileHashDidNotMatch(
      val hashExpected: Hash,
      val hashReceived: Hash
    ) : Verification() {
      init {
        Preconditions.checkState(
          this.hashExpected.text != this.hashReceived.text,
          "${this.hashExpected.text} != ${this.hashReceived.text}"
        )
      }
    }
  }

  /**
   * Create a task that will, when evaluated, check that a file has a given hash.
   */

  fun create(
    progressMajor: InventoryProgressValue? = null,
    file: File,
    hash: Hash,
    deleteOnFailure: Boolean = false
  ): InventoryTask<Verification> {
    return InventoryTask { execution ->
      this.verify(execution, progressMajor, file, hash, deleteOnFailure)
    }
  }

  /**
   * Create a task that will, when evaluated, check that a file has a given hash, and will
   * fail if the verification does not match (rather than returning a successful result with
   * a [FileHashDidNotMatch] value).
   */

  fun createFailing(
    progressMajor: InventoryProgressValue? = null,
    file: File,
    hash: Hash,
    deleteOnFailure: Boolean = false
  ): InventoryTask<Verification> {
    return this.create(progressMajor, file, hash, deleteOnFailure)
      .flatMap(this::verifyFailTask)
  }

  private fun verifyFailTask(verification: Verification): InventoryTask<Verification> {
    return InventoryTask { execution ->
      this.verifyFail(execution, verification)
    }
  }

  private fun verifyFail(
    execution: InventoryTaskExecutionType,
    verification: Verification
  ): InventoryTaskResult<Verification> {
    val strings =
      execution.services.requireService(InventoryStringResourcesType::class.java)

    val step =
      InventoryTaskStep(
        description = strings.verifyCheckSuccess,
        resolution = "",
        exception = null,
        failed = false)

    return when (verification) {
      is Verification.FileHashMatched ->
        InventoryTaskResult.succeeded(verification as Verification, step)
      is Verification.FileHashDidNotMatch -> {
        step.failed = true
        step.resolution = strings.verificationFailed(
          expectedHash = verification.hashExpected,
          receivedHash = verification.hashReceived.text)
        InventoryTaskResult.failed(step)
      }
    }
  }

  private fun verify(
    execution: InventoryTaskExecutionType,
    progressMajor: InventoryProgressValue?,
    file: File,
    hash: Hash,
    deleteOnFailure: Boolean = false
  ): InventoryTaskResult<Verification> {
    this.logger.debug("verify: {} {} (delete: {})", file, hash.text, deleteOnFailure)

    val strings =
      execution.services.requireService(InventoryStringResourcesType::class.java)
    val clock =
      execution.services.requireService(InventoryClockType::class.java)

    val step =
      InventoryTaskStep(
        description = strings.verifyingLocalFile,
        resolution = "",
        exception = null,
        failed = false)

    val counter = UnitsPerSecond(clock)
    var current = 0L
    val expected = file.length()

    return FileInputStream(file).use { stream ->
      val digest = MessageDigest.getInstance("SHA-256")
      val buffer = ByteArray(4096)
      while (true) {
        val r = stream.read(buffer)
        if (r == -1) {
          break
        }
        digest.update(buffer, 0, r)

        current += r
        counter.update(r.toLong())

        val progressMinor =
          InventoryProgressValue.InventoryProgressValueDefinite(
            current = current,
            perSecond = counter.now,
            maximum = expected)

        val status =
          strings.installVerifyingLocalFileProgress(current, expected, counter.now)

        execution.onProgress.invoke(InventoryProgress(progressMajor, progressMinor, status))
        if (execution.isCancelRequested) {
          step.resolution = strings.verificationCancelled
          return InventoryTaskResult.cancelled(step)
        }
      }

      val result = digest.digest()
      val resultText = Hex.bytesToHex(result).toLowerCase()
      this.logger.debug("verification: expected {} received {}", hash.text, resultText)

      if (resultText == hash.text) {
        step.resolution = strings.verificationSucceeded
        InventoryTaskResult.succeeded(
          Verification.FileHashMatched(
            hashExpected = hash,
            hashReceived = Hash(resultText)
          ),
          step)
      } else {
        file.delete()
        step.resolution = strings.verificationFailed(hash, resultText)
        InventoryTaskResult.succeeded(
          Verification.FileHashDidNotMatch(
            hashExpected = hash,
            hashReceived = Hash(resultText)
          ),
          step)
      }
    }
  }
}