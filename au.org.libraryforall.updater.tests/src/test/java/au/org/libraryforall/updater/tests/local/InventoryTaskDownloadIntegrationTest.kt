package au.org.libraryforall.updater.tests.local

import au.org.libraryforall.updater.tests.InventoryTaskDownloadIntegrationContract
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class InventoryTaskDownloadIntegrationTest : InventoryTaskDownloadIntegrationContract() {
  override fun logger(): Logger {
    return LoggerFactory.getLogger(InventoryTaskDownloadIntegrationTest::class.java)
  }
}
