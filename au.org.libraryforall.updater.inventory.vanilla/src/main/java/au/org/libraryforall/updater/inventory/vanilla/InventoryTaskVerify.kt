package au.org.libraryforall.updater.inventory.vanilla

import au.org.libraryforall.updater.inventory.api.InventoryAPKDirectoryReceivers
import au.org.libraryforall.updater.inventory.api.InventoryAPKDirectoryType.KeyReservationType
import au.org.libraryforall.updater.inventory.api.InventoryAPKDirectoryType.VerificationProgressType
import au.org.libraryforall.updater.inventory.api.InventoryAPKDirectoryType.VerificationResult.VerificationCancelled
import au.org.libraryforall.updater.inventory.api.InventoryAPKDirectoryType.VerificationResult.VerificationFailure
import au.org.libraryforall.updater.inventory.api.InventoryAPKDirectoryType.VerificationResult.VerificationSuccess
import au.org.libraryforall.updater.inventory.api.InventoryStringResourcesType
import au.org.libraryforall.updater.inventory.api.InventoryTaskStep
import org.slf4j.LoggerFactory
import java.io.File

class InventoryTaskVerify(
  private val resources: InventoryStringResourcesType,
  private val reservation: KeyReservationType,
  private val onVerificationProgress: (VerificationProgressType) -> Unit) {

  private val logger = LoggerFactory.getLogger(InventoryTaskVerify::class.java)

  fun execute(): InventoryTaskMonad<File> {
    this.logger.debug("verifying local data: {}", this.reservation.file)

    val step = InventoryTaskStep(
      description = this.resources.installVerifiedFile(this.reservation.file),
      resolution = "",
      exception = null,
      failed = false)

    val receiver =
      InventoryAPKDirectoryReceivers.throttledReceiver(
        approximateCalls = 5,
        progress = onVerificationProgress::invoke)

    return InventoryTaskMonad.startWithStep(step)
      .flatMap { this.runVerification(receiver, step) }
  }

  private fun runVerification(
    receiver: (VerificationProgressType) -> Unit,
    step: InventoryTaskStep
  ): InventoryTaskMonad<File> {
    return try {
      if (reservation.file.isFile) {
        when (val verification = this.reservation.verify(receiver)) {
          is VerificationFailure -> {
            this.logger.debug("verification failed")
            step.failed = true
            step.resolution = this.resources.installVerificationFailed(
              expected = this.reservation.hash,
              received = verification.hash)
            InventoryTaskMonad.InventoryTaskFailed<File>()
          }
          is VerificationSuccess -> {
            this.logger.debug("verification succeeded")
            step.failed = false
            step.resolution = this.resources.installVerificationSucceeded
            InventoryTaskMonad.InventoryTaskSuccess(this.reservation.file)
          }
          VerificationCancelled -> {
            TODO()
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
      InventoryTaskMonad.InventoryTaskFailed<File>()
    }
  }
}