package au.org.libraryforall.updater.app

import android.app.Application
import au.org.libraryforall.updater.repository.api.Hash
import au.org.libraryforall.updater.repository.api.Repository
import au.org.libraryforall.updater.repository.api.RepositoryPackage
import org.joda.time.LocalDateTime
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.UUID

class MainApplication : Application() {

  private val logger = LoggerFactory.getLogger(MainApplication::class.java)

  override fun onCreate() {
    super.onCreate()

    this.logger.debug("starting")

    MainServices.initialize(this)

    val package0 =
      RepositoryPackage(
        id = "com.example.alpha",
        versionCode = 23,
        versionName = "1.0.0",
        name = "Alpha",
        source = URI.create("https://www.io7m.com/index.xhtml"),
        sha256 = Hash("01ba4719c80b6fe911b091a7c05124b64eeece964e09c058ef8f9805daca546b"))

    val package1 =
      RepositoryPackage(
        id = "com.example.beta",
        versionCode = 24,
        versionName = "1.1.0",
        name = "Beta",
        source = URI.create("https://ataxia.io7m.com/2019/05/27/file.apk"),
        sha256 = Hash("5891b5b522d5df086d0ff0b110fbd9d21bb4fc7163af34d08286a2e846f6be03"))

    val package2 =
      RepositoryPackage(
        id = "com.example.delta",
        versionCode = 25,
        versionName = "1.1.2",
        name = "Delta",
        source = URI.create("http://pkg.lfa.one/1"),
        sha256 = Hash("01ba4719c80b6fe911b091a7c05124b64eeece964e09c058ef8f9805daca546b"))

    val package3 =
      RepositoryPackage(
        id = "com.example.gamma",
        versionCode = 26,
        versionName = "1.1.3",
        name = "Gamma",
        source = URI.create("http://pkg.lfa.one/1"),
        sha256 = Hash("01ba4719c80b6fe911b091a7c05124b64eeece964e09c058ef8f9805daca546b"))

    val inventory = MainServices.inventory()
    this.logger.debug("inventory: {}", inventory)

    inventory.inventoryRepositoryPut(
        Repository(
          id = UUID.randomUUID(),
          title = "LFA",
          updated = LocalDateTime.now(),
          packages = listOf(package0, package1, package2, package3),
          source = URI.create("http://www.example.com")))

  }
}