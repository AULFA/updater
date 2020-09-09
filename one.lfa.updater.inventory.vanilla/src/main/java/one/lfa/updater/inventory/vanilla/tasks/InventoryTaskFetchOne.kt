package one.lfa.updater.inventory.vanilla.tasks

import one.irradia.http.api.HTTPClientType
import one.irradia.http.api.HTTPResult
import one.lfa.updater.inventory.api.InventoryExternalStorageServiceType
import one.lfa.updater.inventory.api.InventoryHTTPAuthenticationType
import one.lfa.updater.inventory.api.InventoryStringResourcesType
import one.lfa.updater.inventory.api.InventoryTaskStep
import org.joda.time.Duration
import org.joda.time.Instant
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.net.URI

object InventoryTaskFetchOne {

  private val logger =
    LoggerFactory.getLogger(InventoryTaskFetchOne::class.java)

  fun fetch(uri: URI): InventoryTask<InventoryPossiblySizedInputStream> {
    return when (uri.scheme) {
      "file" ->
        this.fetchFileTask(uri)
      InventoryExternalStorageServiceType.uriScheme ->
        this.fetchExternalTask(uri)
      else ->
        this.fetchHTTPTask(uri)
    }
  }

  private fun fetchHTTPTask(uri: URI): InventoryTask<InventoryPossiblySizedInputStream> {
    return InventoryTask { execution ->
      this.logger.debug("fetch: {}", uri)

      val strings =
        execution.services.requireService(InventoryStringResourcesType::class.java)
      val httpClient =
        execution.services.requireService(HTTPClientType::class.java)
      val httpAuthentication =
        execution.services.requireService(InventoryHTTPAuthenticationType::class.java)

      val step = InventoryTaskStep(
        description = strings.repositoryAddFetching(uri),
        resolution = "",
        exception = null,
        failed = false)

      val timeThen = Instant.now()
      return@InventoryTask try {
        when (val result = httpClient.get(uri, httpAuthentication::authenticationFor, 0L)) {
          is HTTPResult.HTTPOK -> {
            val timeNow = Instant.now()
            step.resolution = strings.downloadingHTTPOK(Duration(timeThen, timeNow))
            step.failed = false
            InventoryTaskResult.succeeded(
              InventoryPossiblySizedInputStream(result.contentLength, result.result),
              step
            )
          }

          is HTTPResult.HTTPFailed.HTTPError -> {
            step.failed = true
            step.resolution =
              strings.downloadingHTTPServerError(
                statusCode = result.statusCode,
                message = result.message,
                contentType = result.contentTypeOrDefault,
                contentLength = result.contentLength)
            InventoryTaskResult.failed<InventoryPossiblySizedInputStream>(step)
          }

          is HTTPResult.HTTPFailed.HTTPFailure -> {
            step.failed = true
            step.resolution = strings.downloadingHTTPConnectionFailed(result.exception)
            InventoryTaskResult.failed(step)
          }
        }
      } catch (e : Exception) {
        this.logger.error("fetch file: {}: ", uri, e)
        step.failed = true
        step.resolution = strings.repositoryAddConnectionFailed(e)
        InventoryTaskResult.failed<InventoryPossiblySizedInputStream>(step)
      }
    }
  }

  private fun fetchExternalTask(uri: URI): InventoryTask<InventoryPossiblySizedInputStream> {
    return InventoryTaskExternalStorage.resolveExternalInputFileTask(uri)
  }

  private fun fetchFileTask(uri: URI): InventoryTask<InventoryPossiblySizedInputStream> {
    return InventoryTask { execution ->
      this.logger.debug("fetch file: {}", uri)

      val strings =
        execution.services.requireService(InventoryStringResourcesType::class.java)
      val step =
        InventoryTaskStep(
          description = strings.fileOpening,
          resolution = "",
          exception = null,
          failed = false
        )

      try {
        val file = File(uri)
        val stream = file.inputStream() as InputStream
        InventoryTaskResult.succeeded(
          result = InventoryPossiblySizedInputStream(file.length(), stream),
          step = step
        )
      } catch (e: Exception) {
        this.logger.error("fetch file: {}: ", uri, e)
        step.failed = true
        step.resolution = strings.fileDoesNotExist
        step.exception = e
        InventoryTaskResult.failed<InventoryPossiblySizedInputStream>(step)
      }
    }
  }
}