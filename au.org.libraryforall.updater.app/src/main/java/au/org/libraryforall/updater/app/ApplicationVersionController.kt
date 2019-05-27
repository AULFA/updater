package au.org.libraryforall.updater.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.bluelinelabs.conductor.Controller

class ApplicationVersionController : Controller() {

  private lateinit var version: TextView
  private lateinit var revision: TextView

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup
  ): View {
    val view = inflater.inflate(R.layout.version, container, false)
    this.revision = view.findViewById<TextView>(R.id.version_build)
    this.version = view.findViewById<TextView>(R.id.version_version)
    return view
  }

  override fun onAttach(view: View) {
    super.onAttach(view)

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
