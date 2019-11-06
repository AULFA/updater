package au.org.libraryforall.updater.inventory.vanilla.tasks

import au.org.libraryforall.updater.inventory.api.InventoryProgressValue
import java.io.File
import java.net.URI

data class InventoryTaskFileDownloadRequest(
  val progressMajor: InventoryProgressValue? = null,
  val uri: URI,
  val retries: Int,
  val outputFile: File)