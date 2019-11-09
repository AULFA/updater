package one.lfa.updater.inventory.vanilla

import one.lfa.updater.opds.api.OPDSManifest
import java.io.File
import java.net.URI
import java.util.Locale

/**
 * Functions to create plans to download OPDS catalogs.
 */

object InventoryOPDSPlanning {

  /**
   * Determine the list of operations required to download an OPDS catalog described by
   * the given manifest.
   */

  fun planUpdate(
    manifestURI: URI,
    manifest: OPDSManifest,
    catalogDirectory: File
  ): List<InventoryOPDSOperation> {

    val operations = mutableListOf<InventoryOPDSOperation>()

    operations.add(InventoryOPDSOperation.CreateDirectory(catalogDirectory))
    val outputDirectory = File(catalogDirectory, manifest.id.toString())
    operations.add(InventoryOPDSOperation.CreateDirectory(outputDirectory))

    val booksDirectory = File(outputDirectory, "books")
    operations.add(InventoryOPDSOperation.CreateDirectory(booksDirectory))
    val feedsDirectory = File(outputDirectory, "feeds")
    operations.add(InventoryOPDSOperation.CreateDirectory(feedsDirectory))
    val imageDirectory = File(outputDirectory, "images")
    operations.add(InventoryOPDSOperation.CreateDirectory(imageDirectory))

    val existingFiles = mutableSetOf<File>()
    existingFiles.addAll(listDirectory(booksDirectory))
    existingFiles.addAll(listDirectory(feedsDirectory))
    existingFiles.addAll(listDirectory(imageDirectory))

    val baseURI = this.determineBaseURI(
      opdsManifest = manifest,
      manifestURI = manifestURI
    )

    for (file in manifest.files) {
      val relativeFile =
        baseURI.relativize(file.file)
      val outputFile =
        File(outputDirectory, relativeFile.path).absoluteFile

      operations.add(InventoryOPDSOperation.DownloadFile(
        uri = baseURI.resolve(file.file),
        hashAlgorithm = file.hashAlgorithm,
        hash = file.hash.toLowerCase(Locale.ROOT),
        outputFile = outputFile))

      existingFiles.remove(outputFile)
    }

    for (file in existingFiles) {
      operations.add(InventoryOPDSOperation.DeleteLocalFile(file))
    }

    operations.add(InventoryOPDSOperation.SerializeManifest(manifest))
    return operations.toList()
  }

  private fun listDirectory(directory: File): List<File> {
    return (directory.listFiles()?.toList() ?: listOf())
      .map { file -> file.absoluteFile }
  }

  private fun determineBaseURI(
    opdsManifest: OPDSManifest,
    manifestURI: URI
  ): URI {
    return opdsManifest.baseURI ?: URI.create("${manifestURI}/../").normalize()
  }

}