package one.lfa.updater.inventory.vanilla.tasks

import java.io.InputStream

data class InventoryPossiblySizedInputStream(
  val contentLength: Long?,
  val inputStream: InputStream
)
