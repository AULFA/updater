package au.org.libraryforall.updater.inventory.vanilla

import au.org.libraryforall.updater.inventory.api.InventoryHashIndexedDirectoryType
import au.org.libraryforall.updater.inventory.api.InventoryHashIndexedDirectoryType.KeyReservationType
import au.org.libraryforall.updater.inventory.api.InventoryHashIndexedDirectoryType.ReservationUnavailableException
import au.org.libraryforall.updater.inventory.api.InventoryHashIndexedDirectoryType.VerificationProgressType
import au.org.libraryforall.updater.inventory.api.InventoryHashIndexedDirectoryType.VerificationResult
import au.org.libraryforall.updater.repository.api.Hash
import net.jcip.annotations.ThreadSafe
import org.apache.commons.codec.binary.Hex
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/**
 * A hash indexed directory with pessimistic locking. Clients acquire reservations on files
 * and no other clients may touch those files until the reservations are returned. The
 * implementation is thread-safe.
 */

@ThreadSafe
class InventoryHashIndexedDirectory private constructor(private val base: File)
  : InventoryHashIndexedDirectoryType {

  private val logger = LoggerFactory.getLogger(InventoryHashIndexedDirectory::class.java)
  private val reservationLock = Object()
  private val reservations = mutableMapOf<Hash, Reservation>()

  init {
    this.logger.debug("mkdir {}", this.base)
    this.base.mkdirs()
  }

  override fun <T> withKey(
    key: Hash,
    receiver: (KeyReservationType) -> T)
    : T {

    this.logger.debug("withKey: {}", key.text)

    val reservation =
      synchronized(this.reservationLock) {
        val existing = this.reservations[key]
        if (existing != null) {
          throw ReservationUnavailableException("Reservation unavailable for ${key.text}")
        } else {
          val reservation = Reservation(key, File(base, "${key.text}.apk"))
          this.reservations[key] = reservation
          reservation
        }
      }

    return try {
      receiver.invoke(reservation)
    } finally {
      synchronized(this.reservationLock) {
        this.reservations.remove(key)
      }
    }
  }

  inner class Reservation(
    override val hash: Hash,
    override val file: File) : KeyReservationType {

    override fun verify(progress: (VerificationProgressType) -> Unit): VerificationResult =
      verifyFileWithProgress(this.hash, progress)
  }

  private class Verification(
    var currentBytesValue: Long = 0,
    var maximumBytesValue: Long,

    @Volatile
    var wantCancel: Boolean) : VerificationProgressType {

    override val currentBytes: Long
      get() = currentBytesValue

    override val maximumBytes: Long
      get() = maximumBytesValue

    override fun cancel() {
      this.wantCancel = true
    }
  }

  private fun verifyFileWithProgress(
    key: Hash,
    progress: (VerificationProgressType) -> Unit
  ): VerificationResult {

    val file = File(this.base, "${key.text}.apk")
    this.logger.debug("verify: {}", file)

    val verificationProgress =
      Verification(
        currentBytesValue = 0L,
        maximumBytesValue = file.length(),
        wantCancel = false)

    return FileInputStream(file).use { stream ->
      val digest = MessageDigest.getInstance("SHA-256")
      val buffer = ByteArray(4096)
      while (true) {
        val r = stream.read(buffer)
        if (r == -1) {
          break
        }
        digest.update(buffer, 0, r)
        verificationProgress.currentBytesValue += r

        progress.invoke(verificationProgress)
        if (verificationProgress.wantCancel) {
          return VerificationResult.VerificationCancelled
        }
      }
      val result = digest.digest()
      val resultText = Hex.encodeHexString(result, true)
      if (resultText == key.text) {
        VerificationResult.VerificationSuccess(file)
      } else {
        VerificationResult.VerificationFailure(resultText)
      }
    }
  }

  companion object {

    /**
     * Create a new directory.
     */

    fun create(base: File): InventoryHashIndexedDirectoryType =
      InventoryHashIndexedDirectory(base)
  }
}
