package au.org.libraryforall.updater.inventory.vanilla.tasks

import au.org.libraryforall.updater.inventory.api.InventoryClockType
import au.org.libraryforall.updater.inventory.api.InventoryHTTPAuthenticationType
import au.org.libraryforall.updater.inventory.api.InventoryHTTPConfigurationType
import au.org.libraryforall.updater.inventory.api.InventoryProgress
import au.org.libraryforall.updater.inventory.api.InventoryProgressValue
import au.org.libraryforall.updater.inventory.api.InventoryProgressValue.InventoryProgressValueDefinite
import au.org.libraryforall.updater.inventory.api.InventoryProgressValue.InventoryProgressValueIndefinite
import au.org.libraryforall.updater.inventory.api.InventoryStringResourcesType
import au.org.libraryforall.updater.inventory.api.InventoryTaskStep
import au.org.libraryforall.updater.inventory.vanilla.UnitsPerSecond
import one.irradia.http.api.HTTPClientType
import one.irradia.http.api.HTTPResult
import org.joda.time.Duration
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URI

/**
 * A task that downloads a file over HTTP, retrying on failure.
 */

object InventoryTaskFileDownload {

  private val logger = LoggerFactory.getLogger(InventoryTaskFileDownload.javaClass)

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
      status = { time -> strings.downloadingHTTPRetryingInSeconds(
        time,
        retryAttempt.attemptCurrent,
        retryAttempt.attemptMaximum
      ) })
  }

  private data class HTTPInputStream(
    val contentLength: Long?,
    val inputStream: InputStream)

  private fun getTask(
    uri: URI,
    attempt: InventoryTaskRetryAttempt,
    offset: Long
  ): InventoryTask<HTTPInputStream> {
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
  ): InventoryTaskResult<HTTPInputStream> {
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
        InventoryTaskResult.succeeded(HTTPInputStream(
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
        this.downloadOneAttemptTask(request, attempt)
      })
  }

  private fun openFileTask(
    outputFile: File
  ): InventoryTask<FileOutputStream> {
    return InventoryTask { execution ->
      this.openFile(outputFile, execution)
    }
  }

  private fun openFile(
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
      this.openFileTask(request.outputFile).flatMap { outputStream ->
        this.getTask(request.uri, attempt, currentSize).flatMap { httpInputStream ->
          this.transferTask(
            progress = request.progressMajor,
            attempt = attempt,
            expectedSize = expectedSize,
            outputFile = request.outputFile,
            inputStream = httpInputStream,
            outputStream = outputStream
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
    inputStream: HTTPInputStream,
    outputStream: FileOutputStream
  ): InventoryTask<File> =
    InventoryTask { execution ->
      this.transfer(
        execution = execution,
        progress = progress,
        attempt = attempt,
        expectedSize = expectedSize,
        outputFile = outputFile,
        inputStream = inputStream,
        outputStream = outputStream
      )
    }

  private fun transfer(
    execution: InventoryTaskExecutionType,
    progress: InventoryProgressValue?,
    attempt: InventoryTaskRetryAttempt,
    expectedSize: Long?,
    outputFile: File,
    inputStream: HTTPInputStream,
    outputStream: FileOutputStream
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
        failed = false)

    return try {
      val counter = UnitsPerSecond(clock)
      var current = currentlyHave

      val buffer = ByteArray(4096)
      while (true) {
        if (execution.isCancelRequested) {
          return InventoryTaskResult.cancelled(step)
        }

        val r = inputStream.inputStream.read(buffer)
        if (r == -1) {
          break
        }

        outputStream.write(buffer, 0, r)
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

      step.resolution = strings.downloadingHTTPSucceeded
      InventoryTaskResult.succeeded(outputFile, step)
    } catch (e: java.lang.Exception) {
      this.logger.error("transfer error: ", e)
      throw e
    } finally {
      outputStream.flush()
      outputStream.close()
    }
  }
}