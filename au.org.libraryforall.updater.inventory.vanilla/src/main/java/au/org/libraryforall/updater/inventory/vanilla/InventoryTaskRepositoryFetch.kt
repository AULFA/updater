package au.org.libraryforall.updater.inventory.vanilla

import au.org.libraryforall.updater.inventory.api.InventoryStringResourcesType
import au.org.libraryforall.updater.inventory.api.InventoryTaskStep
import au.org.libraryforall.updater.repository.api.Repository
import au.org.libraryforall.updater.repository.xml.api.RepositoryXMLParserProviderType
import au.org.libraryforall.updater.repository.xml.spi.ParseError
import one.irradia.http.api.HTTPAuthentication
import one.irradia.http.api.HTTPClientType
import one.irradia.http.api.HTTPResult
import org.joda.time.Duration
import org.joda.time.Instant
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.net.URI
import java.util.UUID

class InventoryTaskRepositoryFetch(
  private val resources: InventoryStringResourcesType,
  private val http: HTTPClientType,
  private val httpAuthentication: (URI) -> HTTPAuthentication?,
  private val repositoryParsers: RepositoryXMLParserProviderType,
  private val uri: URI,
  private val requiredUUID: UUID?) {

  private val logger = LoggerFactory.getLogger(InventoryTaskRepositoryFetch::class.java)

  private fun fetchHTTP(): InventoryTaskMonad<InputStream> {
    this.logger.debug("fetch: {}", this.uri)

    val step = InventoryTaskStep(
      description = this.resources.inventoryRepositoryAddFetching(this.uri),
      resolution = "",
      exception = null,
      failed = false)

    val timeThen = Instant.now()
    return InventoryTaskMonad.startWithStep(step).flatMap {
      when (val result = this.http.get(this.uri, this.httpAuthentication, 0L)) {
        is HTTPResult.HTTPOK -> {
          val timeNow = Instant.now()
          step.resolution = this.resources.inventoryRepositoryAddFetched(Duration(timeThen, timeNow))
          step.failed = false
          InventoryTaskMonad.InventoryTaskSuccess(result.result)
        }
        is HTTPResult.HTTPFailed.HTTPError -> {
          step.failed = true
          step.resolution = this.resources.inventoryRepositoryAddServerError(
            statusCode = result.statusCode,
            message = result.message,
            contentType = result.contentTypeOrDefault,
            contentLength = result.contentLength)
          InventoryTaskMonad.InventoryTaskFailed()
        }
        is HTTPResult.HTTPFailed.HTTPFailure -> {
          step.failed = true
          step.resolution = this.resources.inventoryRepositoryAddConnectionFailed(result.exception)
          InventoryTaskMonad.InventoryTaskFailed<InputStream>()
        }
      }
    }
  }

  private fun parse(
    inputStream: InputStream
  ): InventoryTaskMonad<Repository> {
    this.logger.debug("parse: {}", this.uri)

    val step = InventoryTaskStep(
      description = this.resources.inventoryRepositoryAddParsing,
      resolution = "",
      exception = null,
      failed = false)

    return InventoryTaskMonad.startWithStep(step).flatMap {
      val errors = mutableListOf<ParseError>()
      val timeThen = Instant.now()
      try {
        this.repositoryParsers.createParser(this.uri, inputStream).use { parser ->
          parser.errors.subscribe { error -> errors.add(error) }
          val repository = parser.parse()
          val timeNow = Instant.now()
          step.resolution = this.resources.inventoryRepositoryAddParsed(Duration(timeThen, timeNow))
          step.failed = false
          InventoryTaskMonad.InventoryTaskSuccess(repository)
        }
      } catch (e: Exception) {
        this.logger.error("parse: {} failed: ", this.uri, e)
        step.failed = true
        step.exception = e
        step.resolution = buildString {
          this.append(this@InventoryTaskRepositoryFetch.resources.inventoryRepositoryAddParseFailed)
          this.append("\n\n")
          this.append("Problem: ")
          this.append(e.message)
          this.append("(")
          this.append(e.javaClass.canonicalName)
          this.append(")")
          this.append("\n\n")

          for (error in errors) {
            this.append(error.line)
            this.append(':')
            this.append(error.column)
            this.append(": ")
            this.append(error.severity)
            this.append(": ")
            this.append(error.message)
            this.append("\n\n")
          }
        }
        InventoryTaskMonad.InventoryTaskFailed<Repository>()
      }
    }
  }

  private fun checkRequiredUUID(repository: Repository): InventoryTaskMonad<Repository> {
    val step = InventoryTaskStep(
      description = this.resources.inventoryRepositoryRequiredUUIDChecking,
      resolution = "",
      exception = null,
      failed = false)

    return when (this.requiredUUID) {
      null -> {
        step.failed = false
        step.resolution = this.resources.inventoryRepositoryRequiredUUIDCheckingOK(this.requiredUUID, repository.id)
        InventoryTaskMonad.InventoryTaskSuccess(repository)
      }
      else -> {
        if (this.requiredUUID != repository.id) {
          step.failed = true
          step.resolution = this.resources.inventoryRepositoryRequiredUUIDCheckingFailed(this.requiredUUID, repository.id)
          InventoryTaskMonad.InventoryTaskFailed()
        } else {
          step.failed = false
          step.resolution = this.resources.inventoryRepositoryRequiredUUIDCheckingOK(this.requiredUUID, repository.id)
          InventoryTaskMonad.InventoryTaskSuccess(repository)
        }
      }
    }
  }

  fun execute(): InventoryTaskMonad<Repository> =
    this.fetchHTTP()
      .flatMap(this::parse)
      .flatMap(this::checkRequiredUUID)

}