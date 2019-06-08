package au.org.libraryforall.updater.inventory.vanilla

import au.org.libraryforall.updater.inventory.api.InventoryAPKDirectoryThrottledVerificationReceiver
import au.org.libraryforall.updater.inventory.api.InventoryAPKDirectoryType.KeyReservationType
import au.org.libraryforall.updater.inventory.api.InventoryAPKDirectoryType.VerificationProgressType
import au.org.libraryforall.updater.inventory.api.InventoryAPKDirectoryType.VerificationResult.VerificationCancelled
import au.org.libraryforall.updater.inventory.api.InventoryAPKDirectoryType.VerificationResult.VerificationFailure
import au.org.libraryforall.updater.inventory.api.InventoryAPKDirectoryType.VerificationResult.VerificationSuccess
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
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A task that:
 *
 * 1. Checks that a local file exists.
 * 2. If the local file exists, checks that the content is correct.
 * 3. If the content is correct, returns immediately.
 * 4. If the content is not correct, downloads a remote file.
 * 5. Checks that the downloaded file is correct.
 */

class InventoryTaskDownload(
  private val resources: InventoryStringResourcesType,
  private val http: HTTPClientType,
  private val httpAuthentication: (URI) -> HTTPAuthentication?,
  private val reservation: KeyReservationType,
  private val onDownloadProgress: (InventoryTaskDownloadProgressType) -> Unit,
  private val onVerificationProgress: (VerificationProgressType) -> Unit,
  private val uri: URI,
  private val cancel: AtomicBoolean) {

  private val logger = LoggerFactory.getLogger(InventoryTaskDownload::class.java)

  fun execute(): InventoryTaskMonad<File> {
    return this.fileNeedsDownloadingTask().flatMap { downloadRequired ->
      if (downloadRequired) {
        this.openConnectionTask().flatMap { pair -> this.fileDownloadTask(pair.first, pair.second) }
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
      InventoryAPKDirectoryThrottledVerificationReceiver(
        this.onVerificationProgress,
        this.cancel)

    return InventoryTaskMonad.startWithStep(step)
      .flatMap { this.runDownloadVerificationCheck(receiver, step) }
  }

  private fun runDownloadVerificationCheck(
    receiver: (VerificationProgressType) -> Unit,
    step: InventoryTaskStep
  ): InventoryTaskMonad<Boolean> {
    this.logger.debug("running pre-download verification check")
    return try {
      if (this.reservation.file.isFile) {
        when (val verification = this.reservation.verify(receiver)) {
          is VerificationFailure -> {
            this.logger.debug("verification failed, download required")
            step.failed = false
            step.resolution = this.resources.installDownloadNeededHashFailed(
              expected = this.reservation.hash,
              received = verification.hash)
            InventoryTaskMonad.InventoryTaskSuccess(true)
          }
          is VerificationSuccess -> {
            this.logger.debug("verification succeeded, no download required")
            step.failed = false
            step.resolution = this.resources.installDownloadNeededNot
            InventoryTaskMonad.InventoryTaskSuccess(false)
          }
          VerificationCancelled -> {
            this.logger.debug("verification cancelled")
            step.failed = false
            step.resolution = this.resources.installVerificationCancelled
            InventoryTaskMonad.InventoryTaskCancelled()
          }
        }
      } else {
        this.logger.debug("file does not exist, download required")
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

  private fun fileDownloadTask(
    expectedBytes: Long?,
    inputStream: InputStream
  ): InventoryTaskMonad<File> {
    this.logger.debug("download: {}", this.reservation.file)

    val step = InventoryTaskStep(
      description = this.resources.installDownloadingTo(this.reservation.file),
      resolution = "",
      exception = null,
      failed = false)

    return InventoryTaskMonad.startWithStep(step)
      .flatMap { this.runDownload(expectedBytes, inputStream, step) }
  }

  private fun runDownload(
    expectedBytes: Long?,
    inputStream: InputStream,
    step: InventoryTaskStep
  ): InventoryTaskMonad<File> {

    val receiver =
      InventoryTaskDownloadThrottledReceiver(expectedBytes, this.onDownloadProgress)

    return try {
      inputStream.use {
        FileOutputStream(this.reservation.file, false).use { outputStream ->
          val buffer = ByteArray(4096)
          var received = 0L
          while (true) {
            if (this.cancel.get()) {
              this.logger.debug("download cancelled")
              step.resolution = this.resources.installDownloadingCancelled
              step.failed = false
              return InventoryTaskMonad.InventoryTaskCancelled()
            }

            receiver.receivedNow(received)

            val r = inputStream.read(buffer)
            if (r == -1) {
              break
            }

            received += r
            outputStream.write(buffer, 0, r)
          }
        }
      }

      this.logger.debug("download succeeded")
      step.resolution = this.resources.installDownloadingSucceeded
      step.failed = false
      InventoryTaskMonad.InventoryTaskSuccess(this.reservation.file)
    } catch (e: Exception) {
      this.logger.error("download failed: ", e)
      step.resolution = this.resources.installDownloadingFailed(e)
      step.exception = e
      step.failed = true
      InventoryTaskMonad.InventoryTaskFailed()
    }
  }

  private fun openConnectionTask(): InventoryTaskMonad<Pair<Long?, InputStream>> {
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
          .andThen(InventoryTaskMonad.InventoryTaskSuccess(
            Pair(this.knownContentLengthOrNull(result.contentLength), result.result)))
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

  private fun knownContentLengthOrNull(contentLength: Long): Long? {
    return if (contentLength == -1L) {
      null
    } else {
      contentLength
    }
  }
}