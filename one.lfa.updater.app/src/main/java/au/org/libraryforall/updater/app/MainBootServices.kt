package au.org.libraryforall.updater.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.res.AssetManager
import android.os.Build
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import au.org.libraryforall.updater.app.boot.BootEvent
import com.google.common.util.concurrent.MoreExecutors
import one.irradia.http.api.HTTPClientType
import one.irradia.http.vanilla.HTTPClientsOkHTTP
import one.lfa.updater.apkinstaller.api.APKInstallerType
import one.lfa.updater.apkinstaller.device.APKInstallerDevice
import one.lfa.updater.credentials.api.BundledCredentials
import one.lfa.updater.credentials.api.Credential
import one.lfa.updater.installed.api.InstalledApplicationsType
import one.lfa.updater.installed.vanilla.InstalledApplications
import one.lfa.updater.inventory.api.InventoryCatalogDirectoryType
import one.lfa.updater.inventory.api.InventoryClock
import one.lfa.updater.inventory.api.InventoryClockType
import one.lfa.updater.inventory.api.InventoryExternalStorageServiceType
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
import one.lfa.updater.services.api.ServiceDirectory
import one.lfa.updater.services.api.ServiceDirectoryType
import one.lfa.updater.services.api.Services
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object MainBootServices {

  private val logger =
    LoggerFactory.getLogger(MainBootServices::class.java)

  fun setup(
    context: Context,
    onProgress: (BootEvent) -> Unit
  ): ServiceDirectoryType {

    val inventoryExecutor =
      MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4) { runnable ->
        val thread = Thread(runnable)
        thread.name = "one.lfa.inventory[${thread.id}"
        thread
      })

    val backgroundExecutor =
      MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1) { runnable ->
        val thread = Thread(runnable)
        thread.name = "one.lfa.background[${thread.id}"
        thread
      })

    val directory = ServiceDirectory.builder()

    val httpClient =
      this.httpClient()
    val bundledCredentials =
      this.loadBundledCredentials(context.assets)

    directory.addService(
      interfaceType = BackgroundExecutor::class.java,
      service = BackgroundExecutor(backgroundExecutor)
    )
    directory.addService(
      interfaceType = HTTPClientType::class.java,
      service = httpClient
    )
    directory.addService(
      interfaceType = InventoryHTTPAuthenticationType::class.java,
      service = InventoryHTTPAuthentication(bundledCredentials)
    )
    directory.addService(
      interfaceType = InventoryHTTPConfigurationType::class.java,
      service = InventoryHTTPConfiguration
    )
    directory.addService(
      interfaceType = InventoryExternalStorageServiceType::class.java,
      service = InventoryExternalStorageService(context)
    )

    val clock = InventoryClock
    directory.addService(
      interfaceType = InventoryClockType::class.java,
      service = clock
    )

    val stringResources = this.stringResources(context)
    directory.addService(InventoryStringResourcesType::class.java, stringResources)
    directory.addService(InventoryStringDownloadResourcesType::class.java, stringResources)
    directory.addService(InventoryStringRepositoryResourcesType::class.java, stringResources)
    directory.addService(InventoryStringVerificationResourcesType::class.java, stringResources)
    directory.addService(OPDSDatabaseStringsType::class.java, stringResources)

    val repositoryXMLSerializers = RepositoryXMLSerializers.createFromServiceLoader()
    directory.addService(
      interfaceType = RepositoryXMLSerializerProviderType::class.java,
      service = repositoryXMLSerializers
    )

    val repositoryXMLParsers = RepositoryXMLParsers.createFromServiceLoader()
    directory.addService(
      interfaceType = RepositoryXMLParserProviderType::class.java,
      service = repositoryXMLParsers
    )

    val opdsXMLSerializers = OPDSXMLSerializers.createFromServiceLoader()
    directory.addService(
      interfaceType = OPDSXMLSerializerProviderType::class.java,
      service = opdsXMLSerializers
    )

    val opdsXMLParsers = OPDSXMLParsers.createFromServiceLoader()
    directory.addService(
      interfaceType = OPDSXMLParserProviderType::class.java,
      service = opdsXMLParsers
    )

    directory.addService(
      interfaceType = InventoryCatalogDirectoryType::class.java,
      service = this.createCatalogDirectory(context)
    )

    val installedItems = InstalledApplications.create(context)
    directory.addService(
      interfaceType = InstalledApplicationsType::class.java,
      service = installedItems
    )
    directory.addService(
      interfaceType = APKInstallerType::class.java,
      service = APKInstallerDevice.create(installedItems)
    )

    directory.addService(
      interfaceType = InventoryNotificationChannelReferenceType::class.java,
      service = this.notificationChannelReference(context)
    )

    directory.addService(
      interfaceType = InventoryHashIndexedDirectoryType::class.java,
      service = InventoryHashIndexedDirectory.create(
        base = this.apkDirectory(context),
        strings = stringResources,
        clock = clock
      )
    )

    directory.addService(
      interfaceType = OPDSDatabaseType::class.java,
      service = OPDSDatabase.open(
        strings = stringResources,
        directory = this.opdsDatabaseDirectory(context),
        parsers = opdsXMLParsers,
        serializers = opdsXMLSerializers
      )
    )

    directory.addService(
      interfaceType = InventoryRepositoryDatabaseType::class.java,
      service = InventoryRepositoryDatabase.create(
        parsers = repositoryXMLParsers,
        serializers = repositoryXMLSerializers,
        directory = this.inventoryDatabaseDirectory(context)
      )
    )

    val inventory =
      Inventory.open(directory.build(), inventoryExecutor)

    directory.addService(
      interfaceType = InventoryType::class.java,
      service = inventory
    )

    val finalDirectory = directory.build()
    Services.initialize(finalDirectory)

    BundledRepositoriesTask.execute(context, inventory)

    this.enqueueUpdateTask(context)
    return finalDirectory
  }

  private fun loadBundledCredentials(assets: AssetManager): List<Credential> {
    return try {
      val fileName = "bundled_credentials.xml"
      assets.open(fileName).use { stream ->
        BundledCredentials.parse(URI.create(fileName), stream)
      }
    } catch (e: Exception) {
      this.logger.error("could not load bundled credentials: ", e)
      listOf()
    }
  }

  private fun createCatalogDirectory(context: Context): InventoryCatalogDirectoryType {
    return object : InventoryCatalogDirectoryType {
      override val directory: File
        get() = File(context.filesDir, "OPDS")
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

  private fun enqueueUpdateTask(context: Context) {

    /*
     * Start a task to handle updates.
     */

    val workRequestContraints =
      Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresStorageNotLow(true)
        .build()

    val workRequest =
      PeriodicWorkRequestBuilder<NotificationWorker>(1, TimeUnit.HOURS)
        .setConstraints(workRequestContraints)
        .setInitialDelay(1L, TimeUnit.MINUTES)
        .addTag("au.org.libraryforall.updater.app.Updates")
        .build()

    WorkManager.getInstance(context)
      .enqueue(workRequest)
  }
}