package au.org.libraryforall.updater.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.HorizontalChangeHandler
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

  private val logger = LoggerFactory.getLogger(MainActivity::class.java)

  private lateinit var router: Router

  private val inventory = MainServices.inventory()
  private val apkInstaller = MainServices.apkInstaller()

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    this.logger.debug("MainActivity: requestCode: {}", requestCode)
    this.logger.debug("MainActivity: resultCode:  {}", resultCode)
    this.logger.debug("MainActivity: data:        {}", data)

    this.apkInstaller.reportStatus(requestCode, resultCode)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.logger.debug("inventory: {}", this.inventory)
    this.setContentView(R.layout.main_activity)

    val container =
      this.findViewById<View>(R.id.main_container) as ViewGroup

    this.router = Conductor.attachRouter(this, container, savedInstanceState)
    if (!this.router.hasRootController()) {
      MainServices.backgroundExecutor().execute {
        BundledRepositoriesTask(this, this.inventory).execute()
      }

      this.router.setRoot(RouterTransaction.with(RepositoriesViewController()))
    }

    /*
     * If parameters were passed to the activity, act on them!
     */

    val extras = intent.extras
    if (extras != null) {
      when (val target = extras.getString(TARGET_PARAMETER_ID)) {
        "overview" -> {
          this.logger.debug("opening overview")
          UIThread.executeLater(
            runnable = {
              this.router.pushController(
                RouterTransaction.with(OverviewViewController())
                  .pushChangeHandler(HorizontalChangeHandler(500L))
                  .popChangeHandler(HorizontalChangeHandler(500L)))
            },
            milliseconds = 500L)
        }

        else -> {
          this.logger.debug("unrecognized target: {}", target)
        }
      }
    }

    this.enqueueUpdateTask()
  }

  private fun enqueueUpdateTask() {

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

    WorkManager.getInstance(this)
      .enqueue(workRequest)
  }

  override fun onBackPressed() {
    if (!this.router.handleBack()) {
      super.onBackPressed()
    }
  }

  companion object {
    const val TARGET_PARAMETER_ID =
      "au.org.libraryforall.updater.app.MainActivity.target"
  }
}
