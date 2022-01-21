package au.org.libraryforall.updater.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import com.bluelinelabs.conductor.Controller
import io.reactivex.disposables.Disposable

class ApplicationVersionController : Controller() {

  private lateinit var showTestingRepos: SwitchCompat
  private lateinit var developerSettings: ViewGroup
  private lateinit var versionTitle: TextView
  private lateinit var version: TextView
  private lateinit var revision: TextView
  private lateinit var testingReposSubscription : Disposable
  private var versionClicks = 0

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup,
    savedViewState: Bundle?
  ): View {
    val view = inflater.inflate(R.layout.version, container, false)
    this.revision = view.findViewById(R.id.version_build)
    this.versionTitle = view.findViewById(R.id.version_build_title)
    this.version = view.findViewById(R.id.version_version)
    this.developerSettings = view.findViewById(R.id.version_developer)
    this.developerSettings.visibility = View.GONE
    this.showTestingRepos = view.findViewById(R.id.showTestingRepositories)
    return view
  }

  override fun onAttach(view: View) {
    super.onAttach(view)

    this.setOptionsMenuHidden(true)

    /*
     * Subscribe to the "show testing repositories" subject, and enable/disable the switch
     * as necessary.
     */

    this.testingReposSubscription =
      MainDeveloperSettings.showTestingRepositories.subscribe { enabled ->
        UIThread.execute {
          this.showTestingRepos.isChecked = enabled
        }
      }

    /*
     * Clicking "Revision" seven times opens the developer menu.
     */

    this.versionTitle.setOnClickListener {
      this.versionClicks = this.versionClicks + 1
      if (this.versionClicks == 3) {
        this.activity?.let {
          Toast.makeText(it, "Nearly there…", Toast.LENGTH_SHORT).show()
        }
      }
      if (this.versionClicks >= 7) {
        this.developerSettings.visibility = View.VISIBLE
      }
    }

    this.showTestingRepos.setOnClickListener {
      MainDeveloperSettings.setShowTestingRepositories(this.showTestingRepos.isChecked)
    }

    this.version.text =
      String.format(
        "%s (%d) [%s]",
        BuildConfig.VERSION_NAME,
        BuildConfig.VERSION_CODE,
        BuildConfig.BUILD_TYPE)

    this.revision.text =
      BuildConfig.GIT_COMMIT
  }

  override fun onDetach(view: View) {
    super.onDetach(view)

    this.testingReposSubscription.dispose()
  }
}
