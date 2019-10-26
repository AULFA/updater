package au.org.libraryforall.updater.installed.vanilla

import android.content.Context
import au.org.libraryforall.updater.installed.api.InstalledItem
import au.org.libraryforall.updater.installed.api.InstalledItemEvent
import au.org.libraryforall.updater.installed.api.InstalledItemEvent.InstalledItemsChanged.InstalledItemAdded
import au.org.libraryforall.updater.installed.api.InstalledItemEvent.InstalledItemsChanged.InstalledItemRemoved
import au.org.libraryforall.updater.installed.api.InstalledItemEvent.InstalledItemsChanged.InstalledItemUpdated
import au.org.libraryforall.updater.installed.api.InstalledItemsType
import com.google.common.util.concurrent.MoreExecutors
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.joda.time.Instant
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import android.content.Intent
import android.content.pm.ResolveInfo

/**
 * The default implementation of the [InstalledItemsType] interface.
 */

class InstalledItems private constructor(
  private val context: Context
) : InstalledItemsType {

  private val logger = LoggerFactory.getLogger(InstalledItems::class.java)

  private val executor =
    MoreExecutors.listeningDecorator(
      Executors.newFixedThreadPool(1) { runnable ->
        val th = Thread(runnable)
        th.name = "au.org.libraryforall.updater.installed.vanilla.InstalledItems.poll[${th.id}]"
        android.os.Process.setThreadPriority(19)
        th
      })

  init {
    this.logger.debug("initialized")
    this.executor.execute { this.pollingTask() }
  }

  companion object {
    fun create(context: Context): InstalledItemsType =
      InstalledItems(context)
  }

  private val eventSubject: PublishSubject<InstalledItemEvent> = PublishSubject.create()

  override fun items(): Map<String, InstalledItem> {
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
        mapOf<String, InstalledItem>()
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
          this.eventSubject.onNext(InstalledItemAdded(p.value))
        }

        this.logger.trace("{} items removed", removed.size)
        for (p in removed) {
          this.eventSubject.onNext(InstalledItemRemoved(p.value))
        }

        this.logger.trace("{} items updated", updated.size)
        for (p in updated) {
          this.eventSubject.onNext(InstalledItemUpdated(p.value))
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

  private fun packageInfoToPackage(info: ResolveInfo): InstalledItem? {
    return try {
      val packageInfo =
        this.context.packageManager.getPackageInfo(info.activityInfo.packageName, 0)

      InstalledItem(
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

  override val events: Observable<InstalledItemEvent>
    get() = this.eventSubject

}