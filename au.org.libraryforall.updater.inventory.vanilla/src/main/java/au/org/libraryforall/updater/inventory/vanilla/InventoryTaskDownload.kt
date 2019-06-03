package au.org.libraryforall.updater.inventory.vanilla

import au.org.libraryforall.updater.inventory.api.InventoryAPKDirectoryReceivers
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
import org.joda.time.Instant
import org.joda.time.Seconds

class InventoryTaskDownload(
  private val resources: InventoryStringResourcesType,
  private val http: HTTPClientType,
  private val httpAuthentication: (URI) -> HTTPAuthentication?,
  private val reservation: KeyReservationType,
  private val onDownloadProgress: (DownloadProgressType) -> Unit,
  private val onVerificationProgress: (VerificationProgressType) -> Unit,
  private val uri: URI) {

  interface DownloadProgressType {

    val expectedBytesTotal : Long?

    val receivedBytesTotal : Long

    val receivedBytesPerSecond : Long
  }

  private val logger = LoggerFactory.getLogger(InventoryTaskDownload::class.java)

  fun execute(): InventoryTaskMonad<File> {
    return this.fileNeedsDownloadingTask()
      .flatMap { downloadRequired ->
        if (downloadRequired) {
          this.openConnectionTask().flatMap { pair ->
            this.fileDownloadTask(pair.first, pair.second)
          }
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

    val verificationReceiver =
      InventoryAPKDirectoryReceivers.throttledReceiver(
        approximateCalls = 5,
        progress = onVerificationProgress::invoke)

    return InventoryTaskMonad.startWithStep(step)
      .flatMap { this.runDownloadVerificationCheck(verificationReceiver, step) }
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

  private class ThrottledDownloadReceiver(
    override val expectedBytesTotal: Long?,
    val progress: (DownloadProgressType) -> Unit) : DownloadProgressType {

    override val receivedBytesTotal: Long
      get() = this.receivedLast

    override val receivedBytesPerSecond: Long
      get() = this.receivedBPS

    @Volatile
    private var receivedBPS = 0L

    @Volatile
    private var receivedLast = 0L

    @Volatile
    private var timeLast = Instant.now()

    @Volatile
    private var timeCurrent = Instant.now()

    fun receivedNow(receivedNow: Long) {
      this.timeCurrent = Instant.now()
      if (Seconds.secondsBetween(this.timeLast, this.timeCurrent).seconds >= 1) {
        this.receivedBPS = Math.max(0L, receivedNow - this.receivedLast)
        this.receivedLast = receivedNow
        this.timeLast = this.timeCurrent
        this.progress.invoke(this)
      }
    }
  }

  private fun runDownload(
    expectedBytes: Long?,
    inputStream: InputStream,
    step: InventoryTaskStep
  ): InventoryTaskMonad<File> {

    val receiver =
      ThrottledDownloadReceiver(expectedBytes, this.onDownloadProgress)

    return try {
      inputStream.use {
        FileOutputStream(this.reservation.file, false).use { outputStream ->
          val buffer = ByteArray(4096)
          var received = 0L
          while (true) {
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
            Pair(knownContentLengthOrNull(result.contentLength), result.result)))
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