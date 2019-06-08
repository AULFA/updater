package au.org.libraryforall.updater.inventory.vanilla

import au.org.libraryforall.updater.inventory.api.InventoryAPKDirectoryThrottledVerificationReceiver
import au.org.libraryforall.updater.inventory.api.InventoryAPKDirectoryType.KeyReservationType
import au.org.libraryforall.updater.inventory.api.InventoryAPKDirectoryType.VerificationProgressType
import au.org.libraryforall.updater.inventory.api.InventoryAPKDirectoryType.VerificationResult.VerificationCancelled
import au.org.libraryforall.updater.inventory.api.InventoryAPKDirectoryType.VerificationResult.VerificationFailure
import au.org.libraryforall.updater.inventory.api.InventoryAPKDirectoryType.VerificationResult.VerificationSuccess
import au.org.libraryforall.updater.inventory.api.InventoryStringResourcesType
import au.org.libraryforall.updater.inventory.api.InventoryTaskStep
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A task that checks if a local file is correct.
 */

class InventoryTaskVerify(
  private val resources: InventoryStringResourcesType,
  private val reservation: KeyReservationType,
  private val onVerificationProgress: (VerificationProgressType) -> Unit,
  private val cancel: AtomicBoolean) {

  private val logger = LoggerFactory.getLogger(InventoryTaskVerify::class.java)

  fun execute(): InventoryTaskMonad<File> {
    this.logger.debug("verifying local data: {}", this.reservation.file)

    val step = InventoryTaskStep(
      description = this.resources.installVerifiedFile(this.reservation.file),
      resolution = "",
      exception = null,
      failed = false)

    val receiver =
      InventoryAPKDirectoryThrottledVerificationReceiver(
        this.onVerificationProgress,
        this.cancel)

    return InventoryTaskMonad.startWithStep(step).flatMap { this.runVerification(receiver, step) }
  }

  private fun runVerification(
    receiver: (VerificationProgressType) -> Unit,
    step: InventoryTaskStep
  ): InventoryTaskMonad<File> {
    return try {
      if (this.reservation.file.isFile) {
        when (val verification = this.reservation.verify(receiver)) {
          is VerificationFailure -> {
            this.logger.debug("verification failed")
            step.failed = true
            step.resolution = this.resources.installVerificationFailed(
              expected = this.reservation.hash,
              received = verification.hash)
            InventoryTaskMonad.InventoryTaskFailed()
          }
          is VerificationSuccess -> {
            this.logger.debug("verification succeeded")
            step.failed = false
            step.resolution = this.resources.installVerificationSucceeded
            InventoryTaskMonad.InventoryTaskSuccess(this.reservation.file)
          }
          VerificationCancelled -> {
            this.logger.debug("verification cancelled")
            step.failed = false
            step.resolution = this.resources.installVerificationCancelled
            InventoryTaskMonad.InventoryTaskCancelled()
          }
        }
      } else {
        this.logger.debug("verification failed: missing file")
        step.failed = true
        step.resolution = this.resources.installVerificationFailedMissing(this.reservation.file)
        InventoryTaskMonad.InventoryTaskFailed()
      }
    } catch (e: Exception) {
      this.logger.error("verification failed: ", e)
      step.failed = true
      step.exception = e
      step.resolution = this.resources.installVerificationFailedException(e)
      InventoryTaskMonad.InventoryTaskFailed()
    }
  }
}