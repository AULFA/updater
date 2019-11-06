package one.lfa.updater.inventory.vanilla.tasks

import one.lfa.updater.inventory.api.InventoryProgressValue
import java.io.File
import java.net.URI

data class InventoryTaskFileDownloadRequest(
  val progressMajor: InventoryProgressValue? = null,
  val uri: URI,
  val retries: Int,
  val outputFile: File)