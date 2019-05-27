package au.org.libraryforall.updater.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import au.org.libraryforall.updater.inventory.api.InventoryInstallResult
import com.bluelinelabs.conductor.Controller
import org.slf4j.LoggerFactory

class InventoryInstallFailureController(arguments: Bundle) : Controller(arguments) {

  constructor(result: InventoryInstallResult) : this(bundleArguments(result))

  companion object {
    private fun bundleArguments(result: InventoryInstallResult): Bundle {
      val bundle = Bundle()
      bundle.putSerializable("installResult", result)
      return bundle
    }
  }

  private val logger = LoggerFactory.getLogger(InventoryInstallFailureController::class.java)

  private lateinit var saveReport: Button
  private lateinit var packageURI: TextView
  private lateinit var packageValue: TextView
  private lateinit var repositoryValue: TextView
  private lateinit var recyclerView: RecyclerView
  private lateinit var adapter: InventoryInstallFailureListAdapter

  private val result =
    arguments.getSerializable("installResult") as InventoryInstallResult

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
    val mainLayout =
      inflater.inflate(R.layout.install_failure, container, false)

    this.repositoryValue =
      mainLayout.findViewById(R.id.installFailureRepositoryValue)
    this.packageValue =
      mainLayout.findViewById(R.id.installFailurePackageNameValue)
    this.packageURI =
      mainLayout.findViewById(R.id.installFailurePackageURIValue)
    this.recyclerView =
      mainLayout.findViewById(R.id.installFailureSteps)
    this.saveReport =
      mainLayout.findViewById(R.id.installFailureSaveReport)

    return mainLayout
  }

  override fun onAttach(view: View) {
    super.onAttach(view)

    this.adapter = InventoryInstallFailureListAdapter(view.context, this.result.steps)

    this.repositoryValue.text = this.result.repositoryId.toString()
    this.packageValue.text =
      String.format(
        "%s %s (%d)",
        this.result.packageName,
        this.result.packageVersionName,
        this.result.packageVersionCode)

    this.packageURI.text = this.result.packageURI.toString()

    this.recyclerView.setHasFixedSize(false)
    this.recyclerView.layoutManager = LinearLayoutManager(view.context);
    this.recyclerView.adapter = this.adapter
    (this.recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
  }
}
