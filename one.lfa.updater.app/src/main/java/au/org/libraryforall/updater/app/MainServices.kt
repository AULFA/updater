package au.org.libraryforall.updater.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.res.AssetManager
import android.os.Build
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.SettableFuture
import one.irradia.http.api.HTTPClientType
import one.irradia.http.vanilla.HTTPClientsOkHTTP
import one.lfa.updater.apkinstaller.api.APKInstallerType
import one.lfa.updater.apkinstaller.device.APKInstallerDevice
import one.lfa.updater.credentials.api.BundledCredentials
import one.lfa.updater.credentials.api.Credential
import one.lfa.updater.installed.api.InstalledItemsType
import one.lfa.updater.installed.vanilla.InstalledItems
import one.lfa.updater.inventory.api.InventoryCatalogDirectoryType
import one.lfa.updater.inventory.api.InventoryClock
import one.lfa.updater.inventory.api.InventoryClockType
import one.lfa.updater.inventory.api.InventoryHTTPAuthenticationType
import one.lfa.updater.inventory.api.InventoryHTTPConfigurationType
import one.lfa.updater.inventory.api.InventoryHashIndexedDirectoryType
import one.lfa.updater.inventory.api.InventoryRepositoryDatabaseType
import one.lfa.updater.inventory.api.InventoryStringDownloadResourcesType
import one.lfa.updater.inventory.api.InventoryStringRepositoryResourcesType
import one.lfa.updater.inventory.api.InventoryStringResourcesType
import one.lfa.updater.inventory.api.InventoryStringVerificationResourcesType
import one.lfa.updater.inventory.api.InventoryType
import one.lfa.updater.inventory.vanilla.Inventory
import one.lfa.updater.inventory.vanilla.InventoryHashIndexedDirectory
import one.lfa.updater.inventory.vanilla.InventoryRepositoryDatabase
import one.lfa.updater.opds.database.api.OPDSDatabaseStringsType
import one.lfa.updater.opds.database.api.OPDSDatabaseType
import one.lfa.updater.opds.database.vanilla.OPDSDatabase
import one.lfa.updater.opds.xml.api.OPDSXMLParserProviderType
import one.lfa.updater.opds.xml.api.OPDSXMLParsers
import one.lfa.updater.opds.xml.api.OPDSXMLSerializerProviderType
import one.lfa.updater.opds.xml.api.OPDSXMLSerializers
import one.lfa.updater.repository.xml.api.RepositoryXMLParserProviderType
import one.lfa.updater.repository.xml.api.RepositoryXMLParsers
import one.lfa.updater.repository.xml.api.RepositoryXMLSerializerProviderType
import one.lfa.updater.repository.xml.api.RepositoryXMLSerializers
import one.lfa.updater.services.api.ServiceDirectoryType
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

object MainServices {

  private val logger = LoggerFactory.getLogger(MainServices::class.java)

  private val servicesFuture =
    SettableFuture.create<ServiceDirectoryType>()

  val services: ServiceDirectoryType
    get() = this.servicesFuture.get()

  private fun inventoryDatabaseDirectory(context: Context): File {
    val dir = File(context.filesDir, "Repositories").absoluteFile
    this.logger.debug("using inventory directory: {}", dir)
    return dir
  }

  private fun opdsDatabaseDirectory(context: Context): File {
    val dir = File(context.filesDir, "OPDS").absoluteFile
    this.logger.debug("using OPDS database directory: {}", dir)
    return dir
  }

  private val bootExecutor =
    MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1) { runnable ->
      val thread = Thread(runnable)
      thread.name = "one.lfa.boot[${thread.id}"
      thread
    })

  private val inventoryExecutor =
    MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4) { runnable ->
      val thread = Thread(runnable)
      thread.name = "one.lfa.inventory[${thread.id}"
      thread
    })

  private val backgroundExecutor =
    MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1) { runnable ->
      val thread = Thread(runnable)
      thread.name = "one.lfa.background[${thread.id}"
      thread
    })

  fun backgroundExecutor(): ListeningExecutorService =
    this.backgroundExecutor

  fun apkInstaller(): APKInstallerType =
    this.services.requireService(APKInstallerType::class.java)

  fun inventory(): InventoryType =
    this.services.requireService(InventoryType::class.java)

  fun notificationChannel(): InventoryNotificationChannelReferenceType =
    this.services.requireService(InventoryNotificationChannelReferenceType::class.java)

  fun initialize(context: Context) {
    this.logger.debug("initializing services")

    this.bootExecutor.execute {
      try {
        val directory = ServiceDirectory()

        val httpClient =
          this.httpClient()
        val bundledCredentials =
          this.loadBundledCredentials(context.assets)

        directory.register(
          serviceClass = HTTPClientType::class.java,
          service = httpClient
        )
        directory.register(
          serviceClass = InventoryHTTPAuthenticationType::class.java,
          service = InventoryHTTPAuthentication(bundledCredentials)
        )
        directory.register(
          serviceClass = InventoryHTTPConfigurationType::class.java,
          service = InventoryHTTPConfiguration
        )

        val clock = InventoryClock
        directory.register(
          serviceClass = InventoryClockType::class.java,
          service = clock
        )

        val stringResources = this.stringResources(context)
        directory.registerAll(
          stringResources,
          InventoryStringResourcesType::class.java,
          InventoryStringDownloadResourcesType::class.java,
          InventoryStringRepositoryResourcesType::class.java,
          InventoryStringVerificationResourcesType::class.java,
          OPDSDatabaseStringsType::class.java
        )

        directory.register(
          serviceClass = RepositoryXMLSerializerProviderType::class.java,
          service = RepositoryXMLSerializers.createFromServiceLoader()
        )
        directory.register(
          serviceClass = RepositoryXMLParserProviderType::class.java,
          service = RepositoryXMLParsers.createFromServiceLoader()
        )

        directory.register(
          serviceClass = OPDSXMLSerializerProviderType::class.java,
          service = OPDSXMLSerializers.createFromServiceLoader()
        )
        directory.register(
          serviceClass = OPDSXMLParserProviderType::class.java,
          service = OPDSXMLParsers.createFromServiceLoader()
        )

        directory.register(
          serviceClass = InventoryCatalogDirectoryType::class.java,
          service = this.createCatalogDirectory(context)
        )

        val installedItems = InstalledItems.create(context)
        directory.register(
          serviceClass = InstalledItemsType::class.java,
          service = installedItems
        )
        directory.register(
          serviceClass = APKInstallerType::class.java,
          service = APKInstallerDevice.create(installedItems)
        )

        directory.register(
          serviceClass = InventoryNotificationChannelReferenceType::class.java,
          service = this.notificationChannelReference(context)
        )

        directory.register(
          serviceClass = InventoryHashIndexedDirectoryType::class.java,
          service = InventoryHashIndexedDirectory.create(
            base = this.apkDirectory(context),
            strings = stringResources,
            clock = clock
          )
        )

        directory.register(
          serviceClass = OPDSDatabaseType::class.java,
          service = OPDSDatabase.open(
            strings = directory.requireService(OPDSDatabaseStringsType::class.java),
            directory = opdsDatabaseDirectory(context),
            parsers = directory.requireService(OPDSXMLParserProviderType::class.java),
            serializers = directory.requireService(OPDSXMLSerializerProviderType::class.java)
          )
        )

        directory.register(
          serviceClass = InventoryRepositoryDatabaseType::class.java,
          service = InventoryRepositoryDatabase.create(
            parsers = directory.requireService(RepositoryXMLParserProviderType::class.java),
            serializers = directory.requireService(RepositoryXMLSerializerProviderType::class.java),
            directory = this.inventoryDatabaseDirectory(context)
          )
        )

        directory.register(
          serviceClass = InventoryType::class.java,
          service = Inventory.open(directory, this.inventoryExecutor)
        )

        this.logger.debug("initialized services")
        this.servicesFuture.set(directory)
      } catch (e: Exception) {
        this.logger.error("startup failed: ", e)
        this.servicesFuture.setException(e)
      }
    }
  }

  private fun loadBundledCredentials(assets: AssetManager): List<Credential> {
    return try {
      val fileName = "bundled_credentials.xml"
      assets.open(fileName).use { stream ->
        BundledCredentials.parse(URI.create(fileName), stream)
      }
    } catch (e: Exception) {
      this.logger.error("could not load bundled credentials: ", e)
      listOf<Credential>()
    }
  }

  private fun createCatalogDirectory(context: Context): InventoryCatalogDirectoryType {
    return object : InventoryCatalogDirectoryType {
      override val directory: File
        get() = File(context.filesDir, "OPDS")
    }
  }

  private class ServiceDirectory : ServiceDirectoryType {

    private val logger = LoggerFactory.getLogger(ServiceDirectory::class.java)
    private val services = ConcurrentHashMap<Class<*>, List<Any>>()

    fun <T : Any> register(
      serviceClass: Class<T>,
      service: T
    ) {
      this.logger.debug("registering service {}: {}", serviceClass.canonicalName, service)
      val existing = (this.services[serviceClass] ?: listOf()).plus(service)
      this.services[serviceClass] = existing
    }

    fun <T : Any> registerAll(
      service: T,
      vararg serviceClasses: Class<in T>
    ) {
      for (serviceClass in serviceClasses) {
        this.register(serviceClass as Class<T>, service)
      }
    }

    override fun <T : Any> optionalServices(serviceClass: Class<T>): List<T> {
      return (this.services[serviceClass] as List<T>?) ?: listOf()
    }
  }

  private fun notificationChannelReference(
    context: Context
  ): InventoryNotificationChannelReferenceType {
    val channelId = "au.org.libraryforall.updater.app"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val name = "Updater Notifications"
      val descriptionText = "Updater Notifications"
      val importance = NotificationManager.IMPORTANCE_DEFAULT
      val channel = NotificationChannel(channelId, name, importance).apply {
        this.description = descriptionText
      }
      val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.createNotificationChannel(channel)
    }

    return object : InventoryNotificationChannelReferenceType {
      override val channelId: String
        get() = channelId
    }
  }

  private fun apkDirectory(context: Context): File {
    val dir0 = context.getExternalFilesDir("APKs")?.absoluteFile
    if (dir0 != null) {
      this.logger.debug("using external files dir: {}", dir0)
      return dir0
    }

    val dir1 = File(context.filesDir, "APKs").absoluteFile
    this.logger.debug("using internal files dir: {}", dir1)
    return dir1
  }

  private fun httpClient(): HTTPClientType {
    return HTTPClientsOkHTTP().createClient("LFA Updater 0.0.1")
  }

  private fun stringResources(context: Context): InventoryStringResources {
    val verificationStrings =
      InventoryStringVerificationResources(context.resources)
    val downloadStrings =
      InventoryStringDownloadResources(context.resources)
    val repositoryStrings =
      InventoryStringRepositoryResources(context.resources)
    val fileStrings =
      InventoryStringFileResources(context.resources)
    val opdsStrings =
      InventoryStringOPDSResources(context.resources)
    val opdsDatabaseStrings =
      InventoryStringOPDSDatabaseResources(context.resources)

    return InventoryStringResources(
      context = context,
      verificationStrings = verificationStrings,
      downloadStrings = downloadStrings,
      repositoryStrings = repositoryStrings,
      fileStrings = fileStrings,
      opdsStrings = opdsStrings,
      opdsDatabaseStrings = opdsDatabaseStrings
    )
  }
}
