package au.org.libraryforall.updater.app

import android.annotation.SuppressLint
import android.content.Context
import au.org.libraryforall.updater.apkinstaller.api.APKInstallerDevice
import au.org.libraryforall.updater.apkinstaller.api.APKInstallerType
import au.org.libraryforall.updater.installed.api.InstalledPackagesType
import au.org.libraryforall.updater.installed.vanilla.InstalledPackages
import au.org.libraryforall.updater.inventory.api.InventoryAPKDirectoryType
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryDatabaseType
import au.org.libraryforall.updater.inventory.api.InventoryStringResourcesType
import au.org.libraryforall.updater.inventory.api.InventoryType
import au.org.libraryforall.updater.inventory.vanilla.Inventory
import au.org.libraryforall.updater.inventory.vanilla.InventoryAPKDirectory
import au.org.libraryforall.updater.inventory.vanilla.InventoryRepositoryDatabase
import au.org.libraryforall.updater.repository.xml.api.RepositoryXMLParserProviderType
import au.org.libraryforall.updater.repository.xml.api.RepositoryXMLParsers
import au.org.libraryforall.updater.repository.xml.api.RepositoryXMLSerializerProviderType
import au.org.libraryforall.updater.repository.xml.api.RepositoryXMLSerializers
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import one.irradia.http.api.HTTPClientType
import one.irradia.http.vanilla.HTTPClientsOkHTTP
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@SuppressLint("StaticFieldLeak")
object MainServices {

  private val logger = LoggerFactory.getLogger(MainServices::class.java)

  private class AtomicService<T>(private val init: () -> (T)) {
    private val initialized = AtomicBoolean(false)

    @Volatile
    private var service: T? = null

    fun get(): T {
      return if (this.initialized.compareAndSet(false, true)) {
        try {
          this.service = this.init.invoke()
          this.service!!
        } catch (e: Exception) {
          this.initialized.set(false)
          throw e
        }
      } else {
        this.service!!
      }
    }
  }

  private lateinit var context: Context

  private val backgroundExecutor =
    MoreExecutors.listeningDecorator(
      Executors.newFixedThreadPool(1) { runnable ->
        val th = Thread(runnable)
        th.name = "au.org.libraryforall.updater.app.bundled[${th.id}]"
        android.os.Process.setThreadPriority(19)
        th
      })

  private val inventoryExecutor =
    MoreExecutors.listeningDecorator(
      Executors.newFixedThreadPool(4) { runnable ->
        val th = Thread(runnable)
        th.name = "au.org.libraryforall.updater.app.inventory[${th.id}]"
        android.os.Process.setThreadPriority(19)
        th
      })

  private val inventoryStringResources =
    AtomicService {
      InventoryStringResources(this.context)
    }

  private val installedPackagesReference: AtomicService<InstalledPackagesType> =
    AtomicService { InstalledPackages.create(this.context) }

  private var httpClient: AtomicService<HTTPClientType> =
    AtomicService {
      HTTPClientsOkHTTP().createClient("LFA Updater 0.0.1")
    }

  private var repositoryParsers : AtomicService<RepositoryXMLParserProviderType> =
    AtomicService {
      RepositoryXMLParsers.createFromServiceLoader()
    }

  private var repositorySerializers : AtomicService<RepositoryXMLSerializerProviderType> =
    AtomicService {
      RepositoryXMLSerializers.createFromServiceLoader()
    }

  private var inventoryDatabase : AtomicService<InventoryRepositoryDatabaseType> =
    AtomicService {
      InventoryRepositoryDatabase.create(
        parsers = this.repositoryParsers.get(),
        serializers = this.repositorySerializers.get(),
        directory = this.inventoryDatabaseDirectory())
    }

  private fun inventoryDatabaseDirectory(): File {
    val dir = File(this.context.filesDir, "Repositories").absoluteFile
    this.logger.debug("using inventory directory: {}", dir)
    return dir
  }

  private var inventory: AtomicService<InventoryType> =
    AtomicService {
      Inventory.open(
        resources = this.inventoryStringResources(),
        executor = this.inventoryExecutor,
        httpAuthentication = { uri -> null },
        http = this.http(),
        apkDirectory = this.inventoryDirectory(),
        apkInstaller = this.apkInstaller(),
        repositoryParsers = this.repositoryParsers.get(),
        inventoryDatabase = this.inventoryDatabase.get(),
        installedPackages = this.installedPackages())
    }

  private val inventoryDirectoryReference: AtomicService<InventoryAPKDirectoryType> =
    AtomicService { InventoryAPKDirectory.create(apkDirectory()) }

  private fun apkDirectory(): File {
    val dir0 = this.context.getExternalFilesDir("APKs")?.absoluteFile
    if (dir0 != null) {
      this.logger.debug("using external files dir: {}", dir0)
      return dir0
    }

    val dir1 = File(this.context.filesDir, "APKs").absoluteFile
    this.logger.debug("using internal files dir: {}", dir1)
    return dir1
  }

  private val apkInstaller: AtomicService<APKInstallerType> =
    AtomicService { APKInstallerDevice.create(this.installedPackages()) }

  fun apkInstaller(): APKInstallerType =
    this.apkInstaller.get()

  fun http(): HTTPClientType =
    this.httpClient.get()

  fun inventoryDirectory(): InventoryAPKDirectoryType =
    this.inventoryDirectoryReference.get()

  fun installedPackages(): InstalledPackagesType =
    this.installedPackagesReference.get()

  fun inventoryStringResources(): InventoryStringResourcesType =
    this.inventoryStringResources.get()

  fun inventory(): InventoryType =
    this.inventory.get()

  fun backgroundExecutor(): ListeningExecutorService =
    this.backgroundExecutor

  fun initialize(context: Context) {
    this.context = context
  }
}
