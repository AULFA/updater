package au.org.libraryforall.updater.inventory.vanilla.tasks

import au.org.libraryforall.updater.repository.api.Hash
import java.io.File
import java.net.URI

data class InventoryTaskAPKFetchInstallRequest(
  val activity: Any,
  val packageName: String,
  val packageVersionCode: Int,
  val downloadURI: URI,
  val downloadRetries: Int,
  val apkFile: File,
  val hash: Hash)