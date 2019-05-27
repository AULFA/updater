package au.org.libraryforall.updater.inventory.vanilla

import au.org.libraryforall.updater.inventory.api.InventoryHashIndexedDirectoryReceivers
import au.org.libraryforall.updater.inventory.api.InventoryHashIndexedDirectoryType.KeyReservationType
import au.org.libraryforall.updater.inventory.api.InventoryHashIndexedDirectoryType.VerificationProgressType
import au.org.libraryforall.updater.inventory.api.InventoryHashIndexedDirectoryType.VerificationResult.VerificationCancelled
import au.org.libraryforall.updater.inventory.api.InventoryHashIndexedDirectoryType.VerificationResult.VerificationFailure
import au.org.libraryforall.updater.inventory.api.InventoryHashIndexedDirectoryType.VerificationResult.VerificationSuccess
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
      InventoryHashIndexedDirectoryReceivers.throttledReceiver(
        approximateCalls = 5,
        progress = onVerificationProgress::invoke)

    return InventoryTaskMonad.startWithStep(step).flatMap {
      try {
        if (reservation.file.isFile) {
          when (val verification = this.reservation.verify(receiver)) {
            is VerificationFailure -> {
              step.failed = true
              step.resolution = this.resources.installVerificationFailed(
                expected = this.reservation.hash,
                received = verification.hash)
              InventoryTaskMonad.InventoryTaskFailed<File>()
            }
            is VerificationSuccess -> {
              step.failed = false
              step.resolution = this.resources.installVerificationSucceeded
              InventoryTaskMonad.InventoryTaskSuccess(this.reservation.file)
            }
            VerificationCancelled -> {
              TODO()
            }
          }
        } else {
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
}