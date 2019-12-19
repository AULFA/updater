package au.org.libraryforall.updater.app.boot

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import au.org.libraryforall.updater.app.MainActivity
import au.org.libraryforall.updater.app.MainApplication
import au.org.libraryforall.updater.app.OverviewViewController
import au.org.libraryforall.updater.app.R
import au.org.libraryforall.updater.app.RepositoriesViewController
import au.org.libraryforall.updater.app.UIThread
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.HorizontalChangeHandler
import com.google.common.util.concurrent.MoreExecutors
import org.slf4j.LoggerFactory

class BootViewController : Controller() {

  private val logger = LoggerFactory.getLogger(BootViewController::class.java)
  private lateinit var message: TextView
  private lateinit var progress: ProgressBar

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup
  ): View {
    val layout =
      inflater.inflate(R.layout.boot, container, false)
    this.progress =
      layout.findViewById(R.id.bootProgress)
    this.message =
      layout.findViewById(R.id.bootMessages)
    return layout
  }

  override fun onAttach(view: View) {
    super.onAttach(view)

    MainApplication.application.servicesBooting.addListener(
      Runnable {
        UIThread.execute {
          onBootFinished()
        }
      },
      MoreExecutors.directExecutor())
  }

  private fun onBootFinished() {

    this.router.setRoot(
      RouterTransaction.with(RepositoriesViewController())
        .pushChangeHandler(HorizontalChangeHandler(500L))
        .popChangeHandler(HorizontalChangeHandler(500L)))

    /*
     * If parameters were passed to the activity, act on them!
     */

    val extras = activity!!.intent.extras
    if (extras != null) {
      when (val target = extras.getString(MainActivity.TARGET_PARAMETER_ID)) {
        "overview" -> {
          logger.debug("opening overview")
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
          logger.debug("unrecognized target: {}", target)
        }
      }
    }
  }
}