package au.org.libraryforall.updater.installed.vanilla

import android.content.Context
import android.content.pm.PackageInfo
import au.org.libraryforall.updater.installed.api.InstalledPackage
import au.org.libraryforall.updater.installed.api.InstalledPackageEvent
import au.org.libraryforall.updater.installed.api.InstalledPackageEvent.*
import au.org.libraryforall.updater.installed.api.InstalledPackagesType
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.slf4j.LoggerFactory

class InstalledPackages private constructor(
  private val context: Context) : InstalledPackagesType {

  private val logger = LoggerFactory.getLogger(InstalledPackages::class.java)

  init {
    this.logger.debug("initialized")
  }

  companion object {
    fun create(context: Context): InstalledPackagesType =
      InstalledPackages(context)
  }

  @Volatile
  private var installedPackages: Set<String> = this.fetchPackages()

  private fun fetchPackages(): Set<String> {
    return this.context.packageManager.getInstalledPackages(0)
      .map(PackageInfo::packageName)
      .toSet()
  }

  private val eventSubject: PublishSubject<InstalledPackageEvent> = PublishSubject.create()

  override fun packages(): Map<String, InstalledPackage> {
    return this.context.packageManager.getInstalledPackages(0)
      .map(this::packageInfoToPackage)
      .map { pkg -> Pair(pkg.id, pkg) }
      .toMap()
  }

  override fun poll() {
    this.installedPackages = this.fetchPackages()
    this.eventSubject.onNext(InstalledPackagesChanged)
  }

  private fun packageInfoToPackage(info: PackageInfo): InstalledPackage =
    InstalledPackage(
      id = info.packageName,
      versionName = info.versionName ?: info.versionCode.toString(),
      versionCode = info.versionCode,
      name = info.applicationInfo.name ?: info.packageName)

  override val events: Observable<InstalledPackageEvent>
    get() = this.eventSubject

}