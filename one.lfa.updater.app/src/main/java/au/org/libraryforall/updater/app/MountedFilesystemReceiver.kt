package au.org.libraryforall.updater.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_MEDIA_EJECT
import android.content.Intent.ACTION_MEDIA_MOUNTED
import android.content.Intent.ACTION_MEDIA_REMOVED
import android.content.Intent.ACTION_MEDIA_UNMOUNTED
import androidx.core.net.toFile
import one.lfa.updater.inventory.api.InventoryExternalStorageServiceType
import one.lfa.updater.inventory.api.InventoryType
import one.lfa.updater.services.api.Services
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.TimeUnit

class MountedFilesystemReceiver : BroadcastReceiver() {

  private val logger =
    LoggerFactory.getLogger(MountedFilesystemReceiver::class.java)

  override fun onReceive(
    context: Context,
    intent: Intent
  ) {
    val services =
      Services.serviceDirectory()
    val inventory =
      services.requireService(InventoryType::class.java)
    val external =
      services.requireService(InventoryExternalStorageServiceType::class.java)

    val file = intent.data
    return when (val act = intent.action) {
      ACTION_MEDIA_EJECT,
      ACTION_MEDIA_REMOVED,
      ACTION_MEDIA_UNMOUNTED -> {
        if (file == null) {
          this.logger.error("received malformed intent: {} (null data)", intent)
          return
        } else {
          this.logger.debug("media removed/unmounted {}", file)
        }
      }
      ACTION_MEDIA_MOUNTED -> {
        if (file == null) {
          this.logger.error("received malformed intent: {} (null data)", intent)
          return
        } else {
          this.logger.debug("media mounted {}", file)
          external.findDirectoryFor(file.toFile())?.let { baseDirectory ->
            val releases = File(baseDirectory, "releases.xml")
            if (releases.isFile) {
              inventory.inventoryRepositoryAdd(releases.toURI())
            } else {
              this.logger.debug("{} is not a file, ignoring it", releases)
            }
          }
          Unit
        }
      }
      else -> {
        this.logger.debug("received intent: {}", act)
      }
    }
  }
}