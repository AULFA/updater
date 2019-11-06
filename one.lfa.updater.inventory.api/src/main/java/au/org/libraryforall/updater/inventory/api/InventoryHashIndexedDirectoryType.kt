package au.org.libraryforall.updater.inventory.api

import one.lfa.updater.repository.api.Hash
import net.jcip.annotations.ThreadSafe
import java.io.File
import java.io.IOException

/**
 * A hash indexed directory with pessimistic locking. Clients acquire reservations on files
 * and no other clients may touch those files until the reservations are returned.
 *
 * Implementations are required to be thread-safe.
 */

@ThreadSafe
interface InventoryHashIndexedDirectoryType {

  /**
   * The result of deleting a file.
   */

  data class Deleted(

    /**
     * The deleted file.
     */

    val file: File,

    /**
     * The hash the file had.
     */

    val hash: Hash,

    /**
     * The size of the deleted file.
     */

    val size: Long)

  /**
   * The result of executing verification of a file.
   */

  sealed class VerificationResult {

    /**
     * Verification succeeded.
     */

    data class VerificationSuccess(
      val file: File)
      : VerificationResult()

    /**
     * Verification failed. The resulting SHA-256 hash is included.
     */

    data class VerificationFailure(
      val hash: String)
      : VerificationResult()

    /**
     * Verification was cancelled.
     */

    object VerificationCancelled
      : VerificationResult()
  }

  /**
   * An exception raised when a reservation for a given file is unavailable.
   */

  class ReservationUnavailableException(message: String) : Exception(message)

  /**
   * The type of reservations.
   */

  interface KeyReservationType {

    /**
     * The hash key for the reservation.
     */

    val hash: Hash

    /**
     * The reserved file.
     */

    val file: File

    /**
     * Run verification for the given file. The given `progress` function receives progress
     * information in the form of the current and maximum byte offset.
     */

    @Throws(IOException::class)
    fun verify(
      progressMajor: InventoryProgressValue? = null,
      shouldCancel: () -> Boolean = { false },
      progress: (InventoryProgress) -> Unit
    ): VerificationResult
  }

  /**
   * Obtain a reservation for the given key.
   */

  @Throws(IOException::class, ReservationUnavailableException::class)
  fun <T> withKey(
    key: Hash,
    receiver: (KeyReservationType) -> T
  ): T

  /**
   * Delete all cached, non-locked files.
   */

  fun clear(): List<Deleted>
}