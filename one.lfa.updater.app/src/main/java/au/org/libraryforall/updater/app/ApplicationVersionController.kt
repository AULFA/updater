package au.org.libraryforall.updater.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.bluelinelabs.conductor.Controller

class ApplicationVersionController : Controller() {

  private lateinit var openOverview: Button
  private lateinit var developerSettings: ViewGroup
  private lateinit var versionTitle: TextView
  private lateinit var version: TextView
  private lateinit var revision: TextView
  private var versionClicks = 0

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup
  ): View {
    val view = inflater.inflate(R.layout.version, container, false)
    this.revision = view.findViewById(R.id.version_build)
    this.versionTitle = view.findViewById(R.id.version_build_title)
    this.version = view.findViewById(R.id.version_version)
    this.developerSettings = view.findViewById(R.id.version_developer)
    this.openOverview = this.developerSettings.findViewById(R.id.openOverviewActivity)
    return view
  }

  override fun onAttach(view: View) {
    super.onAttach(view)

    this.setOptionsMenuHidden(true)

    this.versionTitle.setOnClickListener {
      this.versionClicks = this.versionClicks + 1
      if (this.versionClicks >= 7) {
        this.developerSettings.visibility = View.VISIBLE
      }
    }

    this.openOverview.setOnClickListener {
      val intent = Intent(this.activity, MainActivity::class.java)
      val bundleExtras = Bundle()
      bundleExtras.putString(MainActivity.TARGET_PARAMETER_ID, "overview")
      intent.putExtras(bundleExtras)
      startActivity(intent)
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
}
