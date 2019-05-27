package au.org.libraryforall.updater.tests.local

import au.org.libraryforall.updater.inventory.api.InventoryHashIndexedDirectoryType
import au.org.libraryforall.updater.inventory.vanilla.InventoryHashIndexedDirectory
import au.org.libraryforall.updater.tests.InventoryHashIndexedDirectoryContract
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

class InventoryHashIndexedDirectoryTest : InventoryHashIndexedDirectoryContract() {

  override fun logger(): Logger =
    LoggerFactory.getLogger(InventoryHashIndexedDirectoryTest::class.java)

  override fun hashIndexedDirectory(directory: File): InventoryHashIndexedDirectoryType =
    InventoryHashIndexedDirectory.create(directory)

}
