package au.org.libraryforall.updater.tests.local

import au.org.libraryforall.updater.tests.InventoryTaskDownloadContract
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class InventoryTaskDownloadTest : InventoryTaskDownloadContract() {
  override fun logger(): Logger =
    LoggerFactory.getLogger(InventoryTaskDownloadTest::class.java)
}
