package au.org.libraryforall.updater.main

import android.content.Context
import com.google.common.base.Preconditions
import one.lfa.updater.inventory.api.InventoryExternalStorageServiceType
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI

class InventoryExternalStorageService(
  private val context: Context
) : InventoryExternalStorageServiceType {

  private val logger =
    LoggerFactory.getLogger(InventoryExternalStorageService::class.java)

  override fun allDirectories(): List<File> {
    return this.context.getExternalFilesDirs(null)
      .toList()
      .filterNotNull()
      .map { File(it, "Bundled") }
  }

  override fun findDirectoryFor(
    mount: File
  ): File? {
    val externals = this.allDirectories()
    val mounted = externals.firstOrNull { file -> file.startsWith(mount) }
    this.logger.debug("external storage directory {}", mounted)
    return mounted
  }

  override fun findFile(uri: URI): File? {
    Preconditions.checkArgument(
      uri.scheme == InventoryExternalStorageServiceType.uriScheme,
      "URI scheme must be %s (received %s)",
      InventoryExternalStorageServiceType.uriScheme,
      uri.scheme
    )

    val directories = this.allDirectories()
    for (directory in directories) {
      val file = File(directory, uri.path)
      this.logger.debug("trying {}", file)
      if (file.isFile) {
        this.logger.debug("{} is a file, using it", file)
        return file
      } else {
        this.logger.debug("{} is not a file", file)
      }
    }

    this.logger.error("could not resolve {} to a file", uri)
    return null
  }
}