package au.org.libraryforall.updater.inventory.vanilla

import au.org.libraryforall.updater.inventory.api.InventoryHashIndexedDirectoryReceivers
import au.org.libraryforall.updater.inventory.api.InventoryHashIndexedDirectoryType.KeyReservationType
import au.org.libraryforall.updater.inventory.api.InventoryHashIndexedDirectoryType.VerificationProgressType
import au.org.libraryforall.updater.inventory.api.InventoryHashIndexedDirectoryType.VerificationResult.VerificationCancelled
import au.org.libraryforall.updater.inventory.api.InventoryHashIndexedDirectoryType.VerificationResult.VerificationFailure
import au.org.libraryforall.updater.inventory.api.InventoryHashIndexedDirectoryType.VerificationResult.VerificationSuccess
import au.org.libraryforall.updater.inventory.api.InventoryStringResourcesType
import au.org.libraryforall.updater.inventory.api.InventoryTaskStep
import one.irradia.http.api.HTTPAuthentication
import one.irradia.http.api.HTTPClientType
import one.irradia.http.api.HTTPResult
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URI

class InventoryTaskDownload(
  private val resources: InventoryStringResourcesType,
  private val http: HTTPClientType,
  private val httpAuthentication: (URI) -> HTTPAuthentication?,
  private val reservation: KeyReservationType,
  private val onVerificationProgress: (VerificationProgressType) -> Unit,
  private val uri: URI) {

  private val logger = LoggerFactory.getLogger(InventoryTaskDownload::class.java)

  fun execute(): InventoryTaskMonad<File> {
    return this.fileNeedsDownloadingTask()
      .flatMap { downloadRequired ->
        if (downloadRequired) {
          this.openConnectionTask().flatMap(this::fileDownloadTask)
        } else {
          InventoryTaskMonad.InventoryTaskSuccess(this.reservation.file)
        }
      }
  }

  private fun fileNeedsDownloadingTask(): InventoryTaskMonad<Boolean> {
    this.logger.debug("check if download needed: {}", this.reservation.file)

    val step = InventoryTaskStep(
      description = this.resources.installDownloadingCheckRequired(this.reservation.file),
      resolution = "",
      exception = null,
      failed = false)

    val receiver =
      InventoryHashIndexedDirectoryReceivers.throttledReceiver(
        approximateCalls = 5,
        progress = onVerificationProgress::invoke)

    return InventoryTaskMonad.startWithStep(step)
      .flatMap { this.runDownloadVerificationCheck(receiver, step) }
  }

  private fun runDownloadVerificationCheck(
    receiver: (VerificationProgressType) -> Unit,
    step: InventoryTaskStep
  ): InventoryTaskMonad.InventoryTaskSuccess<Boolean> {
    return try {
      if (this.reservation.file.isFile) {
        when (val verification = this.reservation.verify(receiver)) {
          is VerificationFailure -> {
            step.failed = false
            step.resolution = this.resources.installDownloadNeededHashFailed(
              expected = this.reservation.hash,
              received = verification.hash)
            InventoryTaskMonad.InventoryTaskSuccess(true)
          }
          is VerificationSuccess -> {
            step.failed = false
            step.resolution = this.resources.installDownloadNeededNot
            InventoryTaskMonad.InventoryTaskSuccess(false)
          }
          VerificationCancelled -> {
            TODO()
          }
        }
      } else {
        step.failed = false
        step.resolution = this.resources.installDownloadNeeded
        InventoryTaskMonad.InventoryTaskSuccess(true)
      }
    } catch (e: Exception) {
      this.logger.error("failed verification: ", e)
      step.failed = true
      step.exception = e
      step.resolution = this.resources.installDownloadNeededExceptional(e)
      InventoryTaskMonad.InventoryTaskSuccess(true)
    }
  }

  private fun fileDownloadTask(inputStream: InputStream): InventoryTaskMonad<File> {
    this.logger.debug("download: {}", this.reservation.file)

    val step = InventoryTaskStep(
      description = this.resources.installDownloadingTo(this.reservation.file),
      resolution = "",
      exception = null,
      failed = false)

    return InventoryTaskMonad.startWithStep(step)
      .flatMap { this.runDownload(inputStream, step) }
  }

  private fun runDownload(
    inputStream: InputStream,
    step: InventoryTaskStep
  ): InventoryTaskMonad<File> {
    return try {
      inputStream.use {
        FileOutputStream(this.reservation.file, false).use { outputStream ->
          val buffer = ByteArray(4096)
          while (true) {
            val r = inputStream.read(buffer)
            if (r == -1) {
              break
            }
            outputStream.write(buffer, 0, r)
          }
        }
      }

      step.resolution = this.resources.installDownloadingSucceeded
      step.failed = false
      InventoryTaskMonad.InventoryTaskSuccess(this.reservation.file)
    } catch (e: Exception) {
      this.logger.error("download failed: ", e)
      step.resolution = this.resources.installDownloadingFailed(e)
      step.exception = e
      step.failed = true
      InventoryTaskMonad.InventoryTaskFailed<File>()
    }
  }

  private fun openConnectionTask(): InventoryTaskMonad<InputStream> {
    this.logger.debug("open connection: {}", this.uri)

    return when (val result =
      this.http.get(
        uri = this.uri,
        authentication = this.httpAuthentication,
        offset = 0L)) {
      is HTTPResult.HTTPOK ->
        InventoryTaskMonad.startWithStep(
          InventoryTaskStep(
            description = this.resources.installOpeningConnectionTo(this.uri),
            resolution = this.resources.installConnectedOK(
              statusCode = result.statusCode,
              message = result.message,
              contentType = result.contentTypeOrDefault,
              contentLength = result.contentLength
            ),
            failed = false))
          .andThen(InventoryTaskMonad.InventoryTaskSuccess(result.result))
      is HTTPResult.HTTPFailed.HTTPError ->
        InventoryTaskMonad.startWithStep(
          InventoryTaskStep(
            description = this.resources.installOpeningConnectionTo(this.uri),
            resolution = this.resources.installConnectionServerError(
              statusCode = result.statusCode,
              message = result.message,
              contentType = result.contentTypeOrDefault,
              contentLength = result.contentLength
            ),
            failed = true))
          .andThen(InventoryTaskMonad.InventoryTaskFailed())
      is HTTPResult.HTTPFailed.HTTPFailure ->
        InventoryTaskMonad.startWithStep(
          InventoryTaskStep(
            description = this.resources.installOpeningConnectionTo(this.uri),
            resolution = this.resources.installConnectionFailed(result.exception),
            exception = result.exception,
            failed = true))
          .andThen(InventoryTaskMonad.InventoryTaskFailed())
    }
  }
}