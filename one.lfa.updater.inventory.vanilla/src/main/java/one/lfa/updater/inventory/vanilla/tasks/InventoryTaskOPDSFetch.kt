package one.lfa.updater.inventory.vanilla.tasks

import one.lfa.updater.inventory.api.InventoryHTTPConfigurationType
import one.lfa.updater.inventory.api.InventoryProgressValue
import one.lfa.updater.inventory.api.InventoryStringOPDSResourcesType
import one.lfa.updater.inventory.api.InventoryStringResourcesType
import one.lfa.updater.inventory.api.InventoryTaskStep
import one.lfa.updater.inventory.vanilla.InventoryOPDSOperation.CreateDirectory
import one.lfa.updater.inventory.vanilla.InventoryOPDSOperation.DeleteLocalFile
import one.lfa.updater.inventory.vanilla.InventoryOPDSOperation.DownloadFile
import one.lfa.updater.inventory.vanilla.InventoryOPDSOperation.SerializeManifest
import one.lfa.updater.inventory.vanilla.InventoryOPDSPlanning
import one.lfa.updater.inventory.vanilla.tasks.InventoryTaskFileVerify.Verification.FileHashDidNotMatch
import one.lfa.updater.inventory.vanilla.tasks.InventoryTaskFileVerify.Verification.FileHashMatched
import one.lfa.updater.opds.api.OPDSManifest
import one.lfa.updater.opds.database.api.OPDSDatabaseType
import one.lfa.updater.repository.api.Hash
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI

/**
 * A task that, when evaluated, downloads an OPDS manifest and parses it.
 */

object InventoryTaskOPDSFetch {

  private val logger =
    LoggerFactory.getLogger(InventoryTaskOPDSFetch::class.java)

  fun create(
    uri: URI,
    catalogDirectory: File
  ): InventoryTask<OPDSManifest> {
    return InventoryTaskOPDSManifestFetch.create(uri)
      .flatMap { opdsManifest ->
        this.planAndExecuteTask(
          opdsManifestURI = uri,
          baseDirectory = catalogDirectory,
          opdsManifest = opdsManifest)
      }
  }

  private fun planAndExecuteTask(
    baseDirectory: File,
    opdsManifestURI: URI,
    opdsManifest: OPDSManifest
  ): InventoryTask<OPDSManifest> {

    val plan =
      InventoryOPDSPlanning.planUpdate(
        manifestURI = opdsManifestURI,
        manifest = opdsManifest,
        catalogDirectory = baseDirectory
      )

    val operationTasks = mutableListOf<InventoryTask<Unit>>()
    for (index in 0 until (plan.size)) {
      val operation = plan[index]

      val progressValueMajor =
        InventoryProgressValue.InventoryProgressValueDefinite(
          current = index.toLong(),
          perSecond = 1L,
          maximum = plan.size.toLong()
        )

      operationTasks.add(
        when (operation) {
          is CreateDirectory ->
            this.opCreateDirectoryTask(progressValueMajor, operation)
          is DownloadFile ->
            this.opDownloadFileTask(progressValueMajor, operation)
          is SerializeManifest ->
            this.opSerializeManifestTask(progressValueMajor, operation)
          is DeleteLocalFile ->
            this.opDeleteLocalFileTask(progressValueMajor, operation)
        })
    }

    return InventoryTask.sequenceUnit(operationTasks)
      .map { opdsManifest }
  }

  private fun opDeleteLocalFileTask(
    progressValueMajor: InventoryProgressValue.InventoryProgressValueDefinite,
    operation: DeleteLocalFile
  ): InventoryTask<Unit> {
    return InventoryTask { execution ->
      val strings =
        execution.services.requireService(InventoryStringResourcesType::class.java)
          as InventoryStringOPDSResourcesType

      this.logger.debug("deleting local file {}", operation.localFile)
      val step = InventoryTaskStep(strings.opdsLocalFileDeleting)
      val ok = operation.localFile.delete()
      if (!ok) {
        if (!operation.localFile.exists()) {
          step.resolution = strings.opdsLocalFileDeletingFailed
          step.failed = true
          return@InventoryTask InventoryTaskResult.failed<Unit>(step)
        }
      }
      return@InventoryTask InventoryTaskResult.succeeded(Unit, step)
    }
  }

  private fun opSerializeManifestTask(
    progressValueMajor: InventoryProgressValue.InventoryProgressValueDefinite,
    operation: SerializeManifest
  ): InventoryTask<Unit> {

    return InventoryTask { execution ->
      this.logger.debug("serializing manifest {}", operation.manifest.id)

      val strings =
        execution.services.requireService(InventoryStringResourcesType::class.java)
          as InventoryStringOPDSResourcesType
      val opdsDatabase =
        execution.services.requireService(OPDSDatabaseType::class.java)

      val step =
        InventoryTaskStep(strings.opdsManifestSerializing)

      try {
        opdsDatabase.createOrUpdate(operation.manifest)
        InventoryTaskResult.succeeded(Unit, step)
      } catch (e: Exception) {
        this.logger.error("could not update manifest: ", e)
        step.resolution = strings.opdsManifestSerializeFailed
        step.exception = e
        step.failed = true
        InventoryTaskResult.failed<Unit>(step)
      }
    }
  }

  private fun opDownloadFileTask(
    progressValueMajor: InventoryProgressValue.InventoryProgressValueDefinite,
    operation: DownloadFile
  ): InventoryTask<Unit> {

    return InventoryTask { execution ->
      this.logger.debug("downloading {} to {}", operation.uri, operation.outputFile)

      val httpConfiguration =
        execution.services.requireService(InventoryHTTPConfigurationType::class.java)

      val hash =
        Hash(operation.hash)

      val request =
        InventoryTaskFileDownloadRequest(
          progressValueMajor,
          uri = operation.uri,
          retries = httpConfiguration.retryCount,
          outputFile = operation.outputFile,
          expectedHash = hash
        )

      val taskDownload =
        InventoryTaskFileDownload.create(request)

      val taskVerifyFirst =
        InventoryTaskFileVerify.create(
          progressMajor = progressValueMajor,
          file = operation.outputFile,
          hash = hash,
          deleteOnFailure = false
        )

      taskVerifyFirst.flatMap { verification ->
        downloadIfRequiredTask(verification, taskDownload)
      }.evaluate(execution)
    }
  }

  private fun downloadIfRequiredTask(
    verification: InventoryTaskFileVerify.Verification,
    taskDownload: InventoryTask<File>
  ): InventoryTask<Unit> {
    this.logger.debug("checking if download is required...")
    return when (verification) {
      is FileHashMatched -> {
        InventoryTask {
          this.logger.debug("download is not required")
          InventoryTaskResult.InventoryTaskSucceeded(Unit, listOf())
        }
      }
      is FileHashDidNotMatch -> {
        this.logger.debug("download is required")
        taskDownload.map { }
      }
    }
  }

  private fun opCreateDirectoryTask(
    progressValueMajor: InventoryProgressValue.InventoryProgressValueDefinite,
    operation: CreateDirectory
  ): InventoryTask<Unit> {
    return InventoryTask { execution ->
      val strings =
        execution.services.requireService(InventoryStringResourcesType::class.java)
          as InventoryStringOPDSResourcesType

      this.logger.debug("creating directory {}", operation.directory)
      val step = InventoryTaskStep(strings.opdsDirectoryCreating)
      val ok = operation.directory.mkdirs()
      if (!ok) {
        if (!operation.directory.isDirectory) {
          step.resolution = strings.opdsDirectoryCreatingFailed
          step.failed = true
          return@InventoryTask InventoryTaskResult.failed<Unit>(step)
        }
      }
      return@InventoryTask InventoryTaskResult.succeeded(Unit, step)
    }
  }
}
