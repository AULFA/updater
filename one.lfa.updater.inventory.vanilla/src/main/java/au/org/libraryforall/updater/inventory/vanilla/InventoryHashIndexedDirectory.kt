package au.org.libraryforall.updater.inventory.vanilla

import au.org.libraryforall.updater.inventory.api.InventoryClockType
import au.org.libraryforall.updater.inventory.api.InventoryHashIndexedDirectoryType
import au.org.libraryforall.updater.inventory.api.InventoryHashIndexedDirectoryType.KeyReservationType
import au.org.libraryforall.updater.inventory.api.InventoryHashIndexedDirectoryType.ReservationUnavailableException
import au.org.libraryforall.updater.inventory.api.InventoryHashIndexedDirectoryType.VerificationResult
import au.org.libraryforall.updater.inventory.api.InventoryHashIndexedDirectoryType.VerificationResult.VerificationCancelled
import au.org.libraryforall.updater.inventory.api.InventoryHashIndexedDirectoryType.VerificationResult.VerificationFailure
import au.org.libraryforall.updater.inventory.api.InventoryHashIndexedDirectoryType.VerificationResult.VerificationSuccess
import au.org.libraryforall.updater.inventory.api.InventoryProgress
import au.org.libraryforall.updater.inventory.api.InventoryProgressValue
import au.org.libraryforall.updater.inventory.api.InventoryStringResourcesType
import one.lfa.updater.repository.api.Hash
import net.jcip.annotations.ThreadSafe
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
class InventoryHashIndexedDirectory private constructor(
  private val base: File,
  private val clock: InventoryClockType,
  private val strings: InventoryStringResourcesType
) : InventoryHashIndexedDirectoryType {

  private val logger = LoggerFactory.getLogger(InventoryHashIndexedDirectory::class.java)
  private val reservationLock = Object()
  private val reservations = mutableMapOf<Hash, Reservation>()

  init {
    this.logger.debug("mkdir {}", this.base)
    this.base.mkdirs()
  }

  override fun clear(): List<InventoryHashIndexedDirectoryType.Deleted> {
    val baseList: Array<String> = this.base.list() ?: emptyArray()

    val deleted =
      mutableListOf<InventoryHashIndexedDirectoryType.Deleted>()

    for (name in baseList) {
      try {
        val hash = Hash(name.removeSuffix(".apk"))
        val file = synchronized(this.reservationLock) {
          if (!this.reservations.containsKey(hash)) {
            File(this.base, hash.text + ".apk")
          } else {
            null
          }
        }

        if (file != null) {
          val size = file.length()
          this.logger.debug("deleting {} ({} bytes)", file, size)
          file.delete()
          deleted.add(InventoryHashIndexedDirectoryType.Deleted(file, hash, size))
        }
      } catch (e: IllegalArgumentException) {
        this.logger.debug("unusual file in APK directory: {}: ", name, e)
      }
    }

    return deleted.toList()
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
          val reservation = this.Reservation(key, File(this.base, "${key.text}.apk"))
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
    override val file: File
  ) : KeyReservationType {

    override fun verify(
      progressMajor: InventoryProgressValue?,
      shouldCancel: () -> Boolean,
      progress: (InventoryProgress) -> Unit
    ): VerificationResult =
      this@InventoryHashIndexedDirectory.verifyFileWithProgress(
        key = this.hash,
        progressMajor = progressMajor,
        shouldCancel = shouldCancel,
        progress = progress
      )
  }

  private fun verifyFileWithProgress(
    key: Hash,
    progressMajor: InventoryProgressValue?,
    shouldCancel: () -> Boolean = { false },
    progress: (InventoryProgress) -> Unit
  ): VerificationResult {

    val file = File(this.base, "${key.text}.apk")
    this.logger.debug("verify: {}", file)

    val counter = UnitsPerSecond(this.clock)
    var current = 0L
    val expected = file.length()

    return FileInputStream(file).use { stream ->
      val digest = MessageDigest.getInstance("SHA-256")
      val buffer = ByteArray(4096)
      while (true) {
        if (shouldCancel.invoke()) {
          break
        }

        val r = stream.read(buffer)
        if (r == -1) {
          break
        }
        digest.update(buffer, 0, r)

        val progressMinor =
          InventoryProgressValue.InventoryProgressValueDefinite(
            current = current,
            perSecond = counter.now,
            maximum = expected)

        val status =
          this.strings.installVerifyingLocalFileProgress(current, expected, counter.now)

        current += r
        if (counter.update(r.toLong())) {
          progress.invoke(InventoryProgress(progressMajor, progressMinor, status))
        }
      }

      if (shouldCancel.invoke()) {
        return VerificationCancelled
      }

      val result = digest.digest()
      val resultText = Hex.bytesToHex(result).toLowerCase()
      if (resultText == key.text) {
        VerificationSuccess(file)
      } else {
        VerificationFailure(resultText)
      }
    }
  }

  companion object {

    /**
     * Create a new directory.
     */

    fun create(
      base: File,
      strings: InventoryStringResourcesType,
      clock: InventoryClockType
    ): InventoryHashIndexedDirectoryType =
      InventoryHashIndexedDirectory(base, clock, strings)
  }
}
