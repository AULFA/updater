package one.lfa.updater.inventory.vanilla.tasks

import one.irradia.http.api.HTTPClientType
import one.irradia.http.api.HTTPResult
import one.lfa.updater.inventory.api.InventoryClockType
import one.lfa.updater.inventory.api.InventoryExternalStorageServiceType
import one.lfa.updater.inventory.api.InventoryHTTPAuthenticationType
import one.lfa.updater.inventory.api.InventoryHTTPConfigurationType
import one.lfa.updater.inventory.api.InventoryProgress
import one.lfa.updater.inventory.api.InventoryProgressValue
import one.lfa.updater.inventory.api.InventoryProgressValue.InventoryProgressValueDefinite
import one.lfa.updater.inventory.api.InventoryProgressValue.InventoryProgressValueIndefinite
import one.lfa.updater.inventory.api.InventoryStringResourcesType
import one.lfa.updater.inventory.api.InventoryTaskStep
import one.lfa.updater.inventory.vanilla.Hex
import one.lfa.updater.inventory.vanilla.UnitsPerSecond
import one.lfa.updater.repository.api.Hash
import org.joda.time.Duration
import org.slf4j.LoggerFactory
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URI
import java.security.MessageDigest

/**
 * A task that downloads a file over HTTP, retrying on failure.
 */

object InventoryTaskFileDownload {

  private val logger = LoggerFactory.getLogger(InventoryTaskFileDownload.javaClass)

  private const val readBufferSize = 65536
  private const val writeBufferSize = 65536

  /**
   * Create a task that will, when evaluated, download a file over HTTP.
   */

  fun create(
    request: InventoryTaskFileDownloadRequest
  ): InventoryTask<File> {
    return this.downloadTask(request)
  }

  private fun pauses(
    progressMajor: InventoryProgressValue?,
    retryAttempt: InventoryTaskRetryAttempt,
    execution: InventoryTaskExecutionType
  ): InventoryTask<Unit> {
    val strings =
      execution.services.requireService(InventoryStringResourcesType::class.java)
    val httpConfiguration =
      execution.services.requireService(InventoryHTTPConfigurationType::class.java)

    return InventoryTaskPause.create(
      progressMajor = progressMajor,
      seconds = httpConfiguration.retryDelaySeconds,
      description = strings.downloadingHTTPWaitingBeforeRetrying,
      status = { time ->
        strings.downloadingHTTPRetryingInSeconds(
          time,
          retryAttempt.attemptCurrent,
          retryAttempt.attemptMaximum
        )
      })
  }

  private fun getTask(
    uri: URI,
    attempt: InventoryTaskRetryAttempt,
    offset: Long
  ): InventoryTask<InventoryPossiblySizedInputStream> {
    return InventoryTask { execution -> this.get(uri, offset, attempt, execution) }
  }

  private fun headTask(
    uri: URI,
    attempt: InventoryTaskRetryAttempt
  ): InventoryTask<Long?> {
    return InventoryTask { execution -> this.head(uri, attempt, execution) }
  }

  private fun head(
    uri: URI,
    attempt: InventoryTaskRetryAttempt,
    execution: InventoryTaskExecutionType
  ): InventoryTaskResult<Long?> {
    this.logger.debug(
      "performing HEAD request for {} (attempt {}/{})",
      uri,
      attempt.attemptCurrent,
      attempt.attemptMaximum)

    val strings =
      execution.services.requireService(InventoryStringResourcesType::class.java)
    val httpClient =
      execution.services.requireService(HTTPClientType::class.java)
    val httpAuthentication =
      execution.services.requireService(InventoryHTTPAuthenticationType::class.java)
    val clock =
      execution.services.requireService(InventoryClockType::class.java)

    val step =
      InventoryTaskStep(
        description = strings.downloadingHTTPRequest(
          uri = uri,
          attemptCurrent = attempt.attemptCurrent,
          attemptMax = attempt.attemptMaximum
        ),
        resolution = "",
        exception = null,
        failed = false)

    val timeThen = clock.now()
    return when (val result = httpClient.head(
      uri = uri,
      authentication = httpAuthentication::authenticationFor,
      offset = 0L
    )) {
      is HTTPResult.HTTPOK -> {
        this.logger.debug(
          "HEAD ok: {} ({} {})",
          result.statusCode,
          result.message,
          result.contentLength,
          result.contentType
        )

        val timeNow = clock.now()
        step.resolution = strings.downloadingHTTPOK(Duration(timeThen, timeNow))
        step.failed = false
        InventoryTaskResult.succeeded(result.contentLength, step)
      }

      is HTTPResult.HTTPFailed.HTTPError -> {
        this.logger.error("http error: {}", result.statusCode, result.message)

        step.failed = true
        step.resolution =
          strings.downloadingHTTPServerError(
            statusCode = result.statusCode,
            message = result.message,
            contentType = result.contentTypeOrDefault,
            contentLength = result.contentLength)
        InventoryTaskResult.failed(step)
      }

      is HTTPResult.HTTPFailed.HTTPFailure -> {
        this.logger.error("http failure: ", result.exception)

        step.failed = true
        step.exception = result.exception
        step.resolution = strings.downloadingHTTPConnectionFailed(result.exception)
        InventoryTaskResult.failed(step)
      }
    }
  }

  private fun get(
    uri: URI,
    offset: Long,
    attempt: InventoryTaskRetryAttempt,
    execution: InventoryTaskExecutionType
  ): InventoryTaskResult<InventoryPossiblySizedInputStream> {
    this.logger.debug(
      "performing GET request for {} offset {} (attempt {}/{})",
      uri,
      offset,
      attempt.attemptCurrent,
      attempt.attemptMaximum)

    val strings =
      execution.services.requireService(InventoryStringResourcesType::class.java)
    val httpClient =
      execution.services.requireService(HTTPClientType::class.java)
    val httpAuthentication =
      execution.services.requireService(InventoryHTTPAuthenticationType::class.java)
    val clock =
      execution.services.requireService(InventoryClockType::class.java)

    val step =
      InventoryTaskStep(
        description = strings.downloadingHTTPRequest(
          uri = uri,
          attemptCurrent = attempt.attemptCurrent,
          attemptMax = attempt.attemptMaximum
        ),
        resolution = "",
        exception = null,
        failed = false)

    val timeThen = clock.now()
    return when (val result = httpClient.get(
      uri = uri,
      authentication = httpAuthentication::authenticationFor,
      offset = offset
    )) {
      is HTTPResult.HTTPOK -> {
        this.logger.debug(
          "GET ok: {} ({} {})",
          result.statusCode,
          result.message,
          result.contentLength,
          result.contentType
        )

        val timeNow = clock.now()
        step.resolution = strings.downloadingHTTPOK(Duration(timeThen, timeNow))
        step.failed = false
        InventoryTaskResult.succeeded(InventoryPossiblySizedInputStream(
          contentLength = result.contentLength,
          inputStream = result.result
        ), step)
      }

      is HTTPResult.HTTPFailed.HTTPError -> {
        this.logger.error("http error: {}", result.statusCode, result.message)

        step.failed = true
        step.resolution =
          strings.downloadingHTTPServerError(
            statusCode = result.statusCode,
            message = result.message,
            contentType = result.contentTypeOrDefault,
            contentLength = result.contentLength)
        InventoryTaskResult.failed(step)
      }

      is HTTPResult.HTTPFailed.HTTPFailure -> {
        this.logger.error("http failure: ", result.exception)

        step.failed = true
        step.exception = result.exception
        step.resolution = strings.downloadingHTTPConnectionFailed(result.exception)
        InventoryTaskResult.failed(step)
      }
    }
  }

  private fun downloadTask(
    request: InventoryTaskFileDownloadRequest
  ): InventoryTask<File> {
    return InventoryTaskRetry.retrying(
      retries = request.retries,
      pauses = { execution, retry ->
        this.pauses(request.progressMajor, retry, execution)
      },
      one = { attempt ->
        this.fetchOneAttemptTask(request, attempt)
      })
  }

  private fun openInputFileTask(
    inputFile: File
  ): InventoryTask<InventoryPossiblySizedInputStream> {
    return InventoryTask { execution ->
      this.openInputFile(inputFile, execution)
    }
  }

  private fun openInputFile(
    inputFile: File,
    execution: InventoryTaskExecutionType
  ): InventoryTaskResult<InventoryPossiblySizedInputStream> {
    this.logger.debug("opening {}", inputFile)

    val strings =
      execution.services.requireService(InventoryStringResourcesType::class.java)

    val step =
      InventoryTaskStep(
        description = strings.fileOpening,
        resolution = "",
        exception = null,
        failed = false)

    return try {
      InventoryTaskResult.succeeded(
        result = InventoryPossiblySizedInputStream(
          contentLength = null,
          inputStream = FileInputStream(inputFile)),
        step = step
      )
    } catch (e: Exception) {
      step.resolution = strings.fileOpeningFailed(e)
      step.exception = e
      step.failed = true
      InventoryTaskResult.failed(step)
    }
  }

  private fun openOutputFileTask(
    outputFile: File
  ): InventoryTask<FileOutputStream> {
    return InventoryTask { execution ->
      this.openOutputFile(outputFile, execution)
    }
  }

  private fun openOutputFile(
    outputFile: File,
    execution: InventoryTaskExecutionType
  ): InventoryTaskResult<FileOutputStream> {
    this.logger.debug("opening {}", outputFile)

    val strings =
      execution.services.requireService(InventoryStringResourcesType::class.java)

    val step =
      InventoryTaskStep(
        description = strings.downloadingHTTPOpeningFile,
        resolution = "",
        exception = null,
        failed = false)

    return try {
      InventoryTaskResult.succeeded(FileOutputStream(outputFile, true), step)
    } catch (e: Exception) {
      step.resolution = strings.downloadingHTTPOpeningFileFailed(e)
      step.exception = e
      step.failed = true
      InventoryTaskResult.failed(step)
    }
  }

  private fun fetchOneAttemptTask(
    request: InventoryTaskFileDownloadRequest,
    attempt: InventoryTaskRetryAttempt
  ): InventoryTask<File> {
    return when (request.uri.scheme) {
      "file" ->
        this.copyOneAttemptTask(request, attempt)
      InventoryExternalStorageServiceType.uriScheme ->
        this.externalCopyOneAttemptTask(request, attempt)
      else ->
        this.downloadOneAttemptTask(request, attempt)
    }
  }

  private fun externalCopyOneAttemptTask(
    request: InventoryTaskFileDownloadRequest,
    attempt: InventoryTaskRetryAttempt
  ): InventoryTask<File> {
    return this.openOutputFileTask(request.outputFile).flatMap { outputStream ->
      val currentSize = request.outputFile.length()
      this.logger.debug("file:      {}", request.outputFile)
      this.logger.debug("file size: {}", currentSize)
      InventoryTaskExternalStorage.resolveExternalInputFileTask(request.uri).flatMap { inputStream ->
        this.transferTask(
          progress = request.progressMajor,
          attempt = attempt,
          expectedSize = null,
          outputFile = request.outputFile,
          inputStream = inputStream,
          outputStream = outputStream,
          expectedHash = request.expectedHash
        )
      }
    }
  }

  private fun copyOneAttemptTask(
    request: InventoryTaskFileDownloadRequest,
    attempt: InventoryTaskRetryAttempt
  ): InventoryTask<File> {
    return this.openOutputFileTask(request.outputFile).flatMap { outputStream ->
      val currentSize = request.outputFile.length()
      this.logger.debug("file:      {}", request.outputFile)
      this.logger.debug("file size: {}", currentSize)
      this.openInputFileTask(File(request.uri)).flatMap { inputStream ->
        this.transferTask(
          progress = request.progressMajor,
          attempt = attempt,
          expectedSize = null,
          outputFile = request.outputFile,
          inputStream = inputStream,
          outputStream = outputStream,
          expectedHash = request.expectedHash
        )
      }
    }
  }

  private fun downloadOneAttemptTask(
    request: InventoryTaskFileDownloadRequest,
    attempt: InventoryTaskRetryAttempt
  ): InventoryTask<File> {

    return this.headTask(request.uri, attempt).flatMap { expectedSize ->
      var currentSize = request.outputFile.length()
      this.logger.debug("file:      {}", request.outputFile)
      this.logger.debug("file size: {}", currentSize)
      this.logger.debug("uri:       {}", request.uri)
      this.logger.debug("uri size:  {}", expectedSize)

      if (expectedSize != null) {
        if (currentSize == expectedSize) {
          return@flatMap this.skipTask(request)
        }
        if (currentSize > expectedSize) {
          this.logger.debug("deleting local content")
          request.outputFile.delete()
        }
      }

      currentSize = request.outputFile.length()
      this.openOutputFileTask(request.outputFile).flatMap { outputStream ->
        this.getTask(request.uri, attempt, currentSize).flatMap { httpInputStream ->
          this.transferTask(
            progress = request.progressMajor,
            attempt = attempt,
            expectedSize = expectedSize,
            outputFile = request.outputFile,
            inputStream = httpInputStream,
            outputStream = outputStream,
            expectedHash = request.expectedHash
          )
        }
      }
    }
  }

  private fun skipTask(request: InventoryTaskFileDownloadRequest): InventoryTask<File> {
    return InventoryTask { execution ->
      val strings =
        execution.services.requireService(InventoryStringResourcesType::class.java)
      InventoryTaskResult.succeeded(request.outputFile, InventoryTaskStep(strings.downloadingHTTPSkipping))
    }
  }

  private fun transferTask(
    progress: InventoryProgressValue?,
    attempt: InventoryTaskRetryAttempt,
    expectedSize: Long?,
    outputFile: File,
    inputStream: InventoryPossiblySizedInputStream,
    outputStream: OutputStream,
    expectedHash: Hash
  ): InventoryTask<File> =
    InventoryTask { execution ->
      this.transfer(
        execution = execution,
        progress = progress,
        attempt = attempt,
        expectedSize = expectedSize,
        outputFile = outputFile,
        inputStream = inputStream,
        outputStream = outputStream,
        expectedHash = expectedHash
      )
    }

  private fun transfer(
    execution: InventoryTaskExecutionType,
    progress: InventoryProgressValue?,
    attempt: InventoryTaskRetryAttempt,
    expectedSize: Long?,
    outputFile: File,
    inputStream: InventoryPossiblySizedInputStream,
    outputStream: OutputStream,
    expectedHash: Hash
  ): InventoryTaskResult<File> {

    this.logger.debug(
      "transferring (attempt {}/{})",
      attempt.attemptCurrent,
      attempt.attemptMaximum)

    val currentlyHave = outputFile.length()
    val expectedTotal = expectedSize ?: -1L
    val availableRemote = inputStream.contentLength ?: -1L

    this.logger.debug("local size:     {}", currentlyHave)
    this.logger.debug("expected size:  {}", expectedTotal)
    this.logger.debug("available size: {}", availableRemote)
    this.logger.debug("delta:          {}", expectedTotal - (currentlyHave + availableRemote))

    val strings =
      execution.services.requireService(InventoryStringResourcesType::class.java)
    val clock =
      execution.services.requireService(InventoryClockType::class.java)

    val step =
      InventoryTaskStep(
        description = strings.downloadingHTTPWritingFile,
        resolution = "",
        exception = null,
        failed = false
      )

    return BufferedOutputStream(outputStream, this.writeBufferSize).use { output ->
      try {
        val counter = UnitsPerSecond(clock)
        var current = currentlyHave
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(this.readBufferSize)
        while (true) {
          if (execution.isCancelRequested) {
            return InventoryTaskResult.cancelled(step)
          }

          val r = inputStream.inputStream.read(buffer)
          if (r == -1) {
            break
          }

          digest.update(buffer, 0, r)
          output.write(buffer, 0, r)
          current += r

          if (counter.update(r.toLong())) {
            val minorProgress = if (expectedSize != null) {
              InventoryProgressValueDefinite(
                current = current,
                perSecond = counter.now,
                maximum = expectedTotal)
            } else {
              InventoryProgressValueIndefinite(
                current = current,
                perSecond = counter.now)
            }

            execution.onProgress(
              InventoryProgress(
                major = progress,
                minor = minorProgress,
                status = strings.downloadingHTTPProgress(progress, minorProgress)))
          }
        }

        val result = digest.digest()
        val resultText = Hex.bytesToHex(result).toLowerCase()
        this.logger.debug("verification: expected {} received {}", expectedHash.text, resultText)

        if (expectedHash.text == resultText) {
          step.resolution = strings.downloadingHTTPSucceeded
          InventoryTaskResult.succeeded(outputFile, step)
        } else {
          throw java.lang.Exception("Hash doesn't match, download failed.");
        }
      } catch (e: java.lang.Exception) {
        this.logger.error("transfer error: ", e)
        step.resolution = strings.downloadingHTTPConnectionFailed(e)
        step.exception = e
        InventoryTaskResult.failed(step)
      } finally {
        output.flush()
      }
    }
  }
}