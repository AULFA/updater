package au.org.libraryforall.updater.app

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * A task that updates the repositories and then publishes a notification if an update
 * is available.
 */

class NotificationWorker(
  context: Context,
  workerParameters: WorkerParameters)
  : Worker(context, workerParameters) {

  private val logger = LoggerFactory.getLogger(NotificationWorker::class.java)

  override fun doWork(): Result {
    val inventory = MainServices.inventory()
    for (repository in inventory.inventoryRepositories()) {
      try {
        repository.update().get(1L, TimeUnit.MINUTES)
      } catch (e: Exception) {
        this.logger.error("could not update repository: ", e)
      }
    }

    for (repository in inventory.inventoryRepositories()) {
      for (repositoryPackage in repository.packages) {
        if (repositoryPackage.isUpdateAvailable) {
          this.publishNotification()
          return Result.success()
        }
      }
    }

    return Result.success()
  }

  private fun publishNotification() {
    val intent = Intent(this.applicationContext, MainActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    val bundleExtras = Bundle()
    bundleExtras.putString(MainActivity.TARGET_PARAMETER_ID, "overview")
    intent.putExtras(bundleExtras)

    val pendingIntent: PendingIntent =
      PendingIntent.getActivity(this.applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

    val resources = this.applicationContext.resources
    val notification =
      NotificationCompat.Builder(this.applicationContext, MainServices.notificationChannel())
        .setSmallIcon(R.drawable.lfa_updater)
        .setContentTitle(resources.getString(R.string.notificationUpdatesTitle))
        .setContentText(resources.getString(R.string.notificationUpdatesText))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()

    with(NotificationManagerCompat.from(this.applicationContext)) {
      this.notify(0, notification)
    }
  }
}