package au.org.libraryforall.updater.inventory.api

import au.org.libraryforall.updater.repository.api.Hash
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
interface InventoryAPKDirectoryType {

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
     * information in the form of the current and maximum byte offset, and can optionally cancel
     * verification.
     */

    @Throws(IOException::class)
    fun verify(progress: (VerificationProgressType) -> Unit): VerificationResult
  }

  /**
   * The type of verification process receivers.
   */

  interface VerificationProgressType {

    /**
     * The current number of bytes verified.
     */

    val currentBytes: Long

    /**
     * The maximum number of bytes that will be verified.
     */

    val maximumBytes: Long

    /**
     * Cancel verification.
     */

    fun cancel()

  }

  /**
   * Obtain a reservation for the given key.
   */

  @Throws(IOException::class, ReservationUnavailableException::class)
  fun <T> withKey(
    key: Hash,
    receiver: (KeyReservationType) -> T
  ): T
}