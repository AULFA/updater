package one.lfa.updater.inventory.vanilla.tasks

import one.lfa.updater.inventory.api.InventoryStringResourcesType
import one.lfa.updater.inventory.api.InventoryTaskStep
import one.lfa.updater.repository.api.Repository
import one.lfa.updater.repository.xml.api.RepositoryXMLParserProviderType
import one.lfa.updater.xml.spi.ParseError
import org.joda.time.Duration
import org.joda.time.Instant
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.net.URI
import java.util.UUID

/**
 * A task that will fetch and parse a repository.
 */

object InventoryTaskRepositoryFetch {

  private val logger =
    LoggerFactory.getLogger(InventoryTaskRepositoryFetch.javaClass)

  private fun parseTask(
    uri: URI,
    inputStream: InputStream
  ): InventoryTask<Repository> {
    return InventoryTask { execution ->
      this.logger.debug("parsing repository {}", uri)

      val strings =
        execution.services.requireService(InventoryStringResourcesType::class.java)
      val parsers =
        execution.services.requireService(RepositoryXMLParserProviderType::class.java)

      val step =
        InventoryTaskStep(
          description = strings.repositoryAddParsing,
          resolution = "",
          exception = null,
          failed = false)

      val errors = mutableListOf<ParseError>()
      val timeThen = Instant.now()
      try {
        parsers.createParser(uri, inputStream).use { parser ->
          parser.errors.subscribe { error -> errors.add(error) }
          val repository = parser.parse()
          val timeNow = Instant.now()
          step.resolution = strings.repositoryAddParsed(Duration(timeThen, timeNow))
          step.failed = false
          InventoryTaskResult.succeeded(repository, step)
        }
      } catch (e: Exception) {
        this.logger.error("parse: {} failed: ", uri, e)
        step.failed = true
        step.exception = e
        step.resolution = buildString {
          this.append(strings.repositoryAddParseFailed)
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
        InventoryTaskResult.failed<Repository>(step)
      }
    }
  }

  private fun checkRequiredUUIDTask(
    repository: Repository,
    requiredUUID: UUID?
  ): InventoryTask<Repository> {
    return InventoryTask { execution ->
      this.logger.debug("checking repository has required uuid: {}", requiredUUID)

      val strings =
        execution.services.requireService(InventoryStringResourcesType::class.java)

      val step = InventoryTaskStep(
        description = strings.repositoryRequiredUUIDChecking,
        resolution = "",
        exception = null,
        failed = false)

      when (requiredUUID) {
        null -> {
          step.failed = false
          step.resolution =
            strings.repositoryRequiredUUIDCheckingOK(requiredUUID, repository.id)
          InventoryTaskResult.succeeded(repository, step)
        }
        else -> {
          if (requiredUUID != repository.id) {
            step.failed = true
            step.resolution =
              strings.repositoryRequiredUUIDCheckingFailed(requiredUUID, repository.id)
            InventoryTaskResult.failed<Repository>(step)
          } else {
            step.failed = false
            step.resolution =
              strings.repositoryRequiredUUIDCheckingOK(requiredUUID, repository.id)
            InventoryTaskResult.succeeded(repository, step)
          }
        }
      }
    }
  }

  /**
   * Create a task that will, when evaluated, download a repository from the given URI, parse it,
   * and then check that it has the given UUID (if one is provided).
   */

  fun create(
    uri: URI,
    requiredUUID: UUID?
  ): InventoryTask<Repository> {
    return InventoryTaskFetchOne.fetch(uri)
      .flatMap { stream -> this.parseTask(uri, stream.inputStream) }
      .flatMap { repository -> this.checkRequiredUUIDTask(repository, requiredUUID) }
  }
}