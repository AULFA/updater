package one.lfa.updater.installed.vanilla

import android.content.Context
import one.lfa.updater.installed.api.InstalledApplication
import one.lfa.updater.installed.api.InstalledApplicationEvent
import one.lfa.updater.installed.api.InstalledApplicationEvent.InstalledApplicationsChanged.InstalledApplicationAdded
import one.lfa.updater.installed.api.InstalledApplicationEvent.InstalledApplicationsChanged.InstalledApplicationRemoved
import one.lfa.updater.installed.api.InstalledApplicationEvent.InstalledApplicationsChanged.InstalledApplicationUpdated
import one.lfa.updater.installed.api.InstalledApplicationsType
import com.google.common.util.concurrent.MoreExecutors
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.joda.time.Instant
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import android.content.Intent
import android.content.pm.ResolveInfo

/**
 * The default implementation of the [InstalledApplicationsType] interface.
 */

class InstalledApplications private constructor(
  private val context: Context
) : InstalledApplicationsType {

  private val logger = LoggerFactory.getLogger(InstalledApplications::class.java)

  private val executor =
    MoreExecutors.listeningDecorator(
      Executors.newFixedThreadPool(1) { runnable ->
        val th = Thread(runnable)
        th.name = "one.lfa.updater.installed.vanilla.InstalledApplications.poll[${th.id}]"
        android.os.Process.setThreadPriority(19)
        th
      })

  init {
    this.logger.debug("initialized")
    this.executor.execute { this.pollingTask() }
  }

  companion object {
    fun create(context: Context): InstalledApplicationsType =
      InstalledApplications(context)
  }

  private val eventSubject: PublishSubject<InstalledApplicationEvent> = PublishSubject.create()

  override fun items(): Map<String, InstalledApplication> {
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
        this.items()
      } catch (e: Exception) {
        this.logger.error("polling task initial: ", e)
        mapOf<String, InstalledApplication>()
      }

    while (true) {
      try {
        val installedNow =
          this.items()

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

        this.logger.trace("{} items added", added.size)
        for (p in added) {
          this.eventSubject.onNext(InstalledApplicationAdded(p.value))
        }

        this.logger.trace("{} items removed", removed.size)
        for (p in removed) {
          this.eventSubject.onNext(InstalledApplicationRemoved(p.value))
        }

        this.logger.trace("{} items updated", updated.size)
        for (p in updated) {
          this.eventSubject.onNext(InstalledApplicationUpdated(p.value))
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

  private fun packageInfoToPackage(info: ResolveInfo): InstalledApplication? {
    return try {
      val packageInfo =
        this.context.packageManager.getPackageInfo(info.activityInfo.packageName, 0)

      InstalledApplication(
        id = packageInfo.packageName,
        versionName = packageInfo.versionName ?: packageInfo.versionCode.toString(),
        versionCode = packageInfo.versionCode.toLong(),
        lastUpdated = Instant.ofEpochMilli(packageInfo.lastUpdateTime),
        name = packageInfo.applicationInfo.name ?: packageInfo.packageName)
    } catch (e: Exception) {
      this.logger.error("error getting package info for {}: ", info.activityInfo.packageName, e)
      null
    }
  }

  override val events: Observable<InstalledApplicationEvent>
    get() = this.eventSubject

}