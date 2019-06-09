package au.org.libraryforall.updater.installed.vanilla

import android.content.Context
import au.org.libraryforall.updater.installed.api.InstalledPackage
import au.org.libraryforall.updater.installed.api.InstalledPackageEvent
import au.org.libraryforall.updater.installed.api.InstalledPackageEvent.InstalledPackagesChanged.InstalledPackageAdded
import au.org.libraryforall.updater.installed.api.InstalledPackageEvent.InstalledPackagesChanged.InstalledPackageRemoved
import au.org.libraryforall.updater.installed.api.InstalledPackageEvent.InstalledPackagesChanged.InstalledPackageUpdated
import au.org.libraryforall.updater.installed.api.InstalledPackagesType
import com.google.common.util.concurrent.MoreExecutors
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.joda.time.Instant
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import android.content.Intent
import android.content.pm.ResolveInfo


class InstalledPackages private constructor(
  private val context: Context) : InstalledPackagesType {

  private val logger = LoggerFactory.getLogger(InstalledPackages::class.java)

  private val executor =
    MoreExecutors.listeningDecorator(
      Executors.newFixedThreadPool(1) { runnable ->
        val th = Thread(runnable)
        th.name = "au.org.libraryforall.updater.installed.vanilla.InstalledPackages.poll[${th.id}]"
        android.os.Process.setThreadPriority(19)
        th
      })

  init {
    this.logger.debug("initialized")
    this.executor.execute { this.pollingTask() }
  }

  companion object {
    fun create(context: Context): InstalledPackagesType =
      InstalledPackages(context)
  }

  private val eventSubject: PublishSubject<InstalledPackageEvent> = PublishSubject.create()

  override fun packages(): Map<String, InstalledPackage> {
    val intent = Intent(Intent.ACTION_MAIN, null)
    intent.addCategory(Intent.CATEGORY_LAUNCHER)
    return this.context.packageManager.queryIntentActivities(intent, 0)
      .mapNotNull(this::packageInfoToPackage)
      .map { pkg -> Pair(pkg.id, pkg) }
      .toMap()
  }

  private fun pollingTask() {
    var installedThen =
      try {
        this.packages()
      } catch (e: Exception) {
        this.logger.error("polling task initial: ", e)
        mapOf<String, InstalledPackage>()
      }

    while (true) {
      try {
        val installedNow =
          this.packages()

        val added =
          installedNow.filter { p -> !installedThen.containsKey(p.key) }
        val removed =
          installedThen.filter { p -> !installedNow.containsKey(p.key) }

        val updated =
          installedNow.filter { now ->
            val then = installedThen[now.key]
            if (then != null) {
              then.lastUpdated != now.value.lastUpdated;
            } else {
              false
            }
          }

        this.logger.trace("{} packages added", added.size)
        for (p in added) {
          this.eventSubject.onNext(InstalledPackageAdded(p.value))
        }

        this.logger.trace("{} packages removed", removed.size)
        for (p in removed) {
          this.eventSubject.onNext(InstalledPackageRemoved(p.value))
        }

        this.logger.trace("{} packages updated", updated.size)
        for (p in updated) {
          this.eventSubject.onNext(InstalledPackageUpdated(p.value))
        }

        installedThen = installedNow
      } catch (e: Exception) {
        this.logger.error("polling task: ", e)
      } finally {
        try {
          Thread.sleep(5_000L)
        } catch (e: InterruptedException) {
          Thread.currentThread().interrupt()
        }
      }
    }
  }

  private fun packageInfoToPackage(info: ResolveInfo): InstalledPackage? {
    return try {
      val packageInfo =
        this.context.packageManager.getPackageInfo(info.activityInfo.packageName, 0)

      InstalledPackage(
        id = packageInfo.packageName,
        versionName = packageInfo.versionName ?: packageInfo.versionCode.toString(),
        versionCode = packageInfo.versionCode,
        lastUpdated = Instant.ofEpochMilli(packageInfo.lastUpdateTime),
        name = packageInfo.applicationInfo.name ?: packageInfo.packageName)
    } catch (e: Exception) {
      this.logger.error("error getting package info for {}: ", info.activityInfo.packageName, e)
      null
    }
  }

  override val events: Observable<InstalledPackageEvent>
    get() = this.eventSubject

}