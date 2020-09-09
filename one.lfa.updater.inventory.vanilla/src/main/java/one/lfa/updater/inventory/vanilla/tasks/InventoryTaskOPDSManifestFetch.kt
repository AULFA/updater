package one.lfa.updater.inventory.vanilla.tasks

import one.lfa.updater.inventory.api.InventoryStringOPDSResourcesType
import one.lfa.updater.inventory.api.InventoryStringResourcesType
import one.lfa.updater.inventory.api.InventoryTaskStep
import one.lfa.updater.opds.api.OPDSManifest
import one.lfa.updater.opds.xml.api.OPDSXMLParserProviderType
import one.lfa.updater.xml.spi.ParseError
import org.joda.time.Duration
import org.joda.time.Instant
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.net.URI

/**
 * A task that, when evaluated, downloads an OPDS manifest and parses it.
 */

object InventoryTaskOPDSManifestFetch {

  private val logger = LoggerFactory.getLogger(InventoryTaskOPDSManifestFetch::class.java)

  fun create(uri: URI): InventoryTask<OPDSManifest> {
    return InventoryTaskFetchOne.fetch(uri)
      .flatMap { stream -> parseTask(uri, stream.inputStream) }
  }

  private fun parseTask(
    uri: URI,
    inputStream: InputStream
  ): InventoryTask<OPDSManifest> {
    return InventoryTask { execution ->
      this.logger.debug("parsing manifest {}", uri)

      val strings : InventoryStringOPDSResourcesType =
        execution.services.requireService(InventoryStringResourcesType::class.java)
      val parsers =
        execution.services.requireService(OPDSXMLParserProviderType::class.java)

      val step =
        InventoryTaskStep(
          description = strings.opdsManifestParsing,
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
          step.resolution = strings.opdsManifestParsed(Duration(timeThen, timeNow))
          step.failed = false
          InventoryTaskResult.succeeded(repository, step)
        }
      } catch (e: Exception) {
        this.logger.error("parse: {} failed: ", uri, e)
        step.failed = true
        step.exception = e
        step.resolution = buildString {
          this.append(strings.opdsManifestParseFailed)
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
        InventoryTaskResult.failed<OPDSManifest>(step)
      }
    }
  }
}
