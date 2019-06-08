package au.org.libraryforall.updater.tests.local

import au.org.libraryforall.updater.inventory.api.InventoryAPKDirectoryType
import au.org.libraryforall.updater.inventory.vanilla.InventoryAPKDirectory
import au.org.libraryforall.updater.tests.InventoryAPKDirectoryContract
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

class InventoryHashIndexedDirectoryTest : InventoryAPKDirectoryContract() {

  override fun logger(): Logger =
    LoggerFactory.getLogger(InventoryHashIndexedDirectoryTest::class.java)

  override fun hashIndexedDirectory(directory: File): InventoryAPKDirectoryType =
    InventoryAPKDirectory.create(directory)

}
