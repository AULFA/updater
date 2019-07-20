package au.org.libraryforall.updater.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import au.org.libraryforall.updater.inventory.api.InventoryEvent
import au.org.libraryforall.updater.inventory.api.InventoryFailureReport
import au.org.libraryforall.updater.inventory.api.InventoryPackageInstallResult
import au.org.libraryforall.updater.inventory.api.InventoryPackageState
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryPackageType
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.HorizontalChangeHandler
import io.reactivex.disposables.Disposable
import org.slf4j.LoggerFactory
import java.util.TreeMap

class OverviewViewController(arguments: Bundle) : Controller(arguments) {

  constructor() : this(bundleArguments())

  companion object {
    private fun bundleArguments(): Bundle {
      val bundle = Bundle()
      return bundle
    }
  }

  private val logger = LoggerFactory.getLogger(OverviewViewController::class.java)
  private var repositoryEventSubscription: Disposable? = null
  private val inventory = MainServices.inventory()

  init {
    this.setHasOptionsMenu(true)
  }

  private lateinit var listPackages: MutableList<InventoryRepositoryPackageType>
  private lateinit var recyclerView: RecyclerView
  private lateinit var listAdapter: InventoryListAdapter

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
    val layout =
      inflater.inflate(R.layout.overview, container, false)

    this.recyclerView =
      layout.findViewById(R.id.overviewPackageList)

    return layout
  }

  override fun onAttach(view: View) {
    super.onAttach(view)

    (this.activity as AppCompatActivity).supportActionBar?.title =
      view.context.resources.getString(R.string.overview)
    this.setOptionsMenuHidden(false)

    this.listPackages = mutableListOf()
    this.listAdapter =
      InventoryListAdapter(
        context = this.activity!!,
        packages = this.listPackages,
        onShowFailureDetails = this@OverviewViewController::showRepositoryPackageFailure)

    this.recyclerView.setHasFixedSize(true)
    this.recyclerView.layoutManager = LinearLayoutManager(view.context);
    this.recyclerView.adapter = this.listAdapter
    (this.recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

    this.repositoryEventSubscription =
      this.inventory.events
        .subscribe { event -> this.onInventoryEvent(event) }

    this.fetchUpdates()
  }

  private fun onInventoryEvent(event: InventoryEvent) {
    this.fetchUpdates()
  }

  private fun fetchUpdates() {
    val updates =
      mutableMapOf<String, InventoryRepositoryPackageType>()

    for (repository in this.inventory.inventoryRepositories()) {
      for (packageCurrent in repository.packages) {
        if (this.packageIsSuitableForOverview(packageCurrent)) {
          val existing = updates[packageCurrent.id]
          if (existing == null || packageCurrent.versionCode > existing.versionCode) {
            updates[packageCurrent.id] = packageCurrent
          }
        }
      }
    }

    val resultUpdates =
      updates.values.sortedBy { p -> p.name }

    UIThread.execute {
      this.listPackages.clear()
      this.listPackages.addAll(resultUpdates)
      this.listAdapter.notifyDataSetChanged()
    }
  }

  private fun packageIsSuitableForOverview(packageCurrent: InventoryRepositoryPackageType) =
    packageCurrent.isUpdateAvailable || when (packageCurrent.state) {
      is InventoryPackageState.NotInstalled -> false
      is InventoryPackageState.Installed -> false
      is InventoryPackageState.InstallFailed -> true
      is InventoryPackageState.Installing -> true
    }

  private fun showRepositoryPackageFailure(
    repositoryPackage: InventoryRepositoryPackageType,
    result: InventoryPackageInstallResult
  ) {
    this.router.pushController(
      RouterTransaction.with(InventoryFailureViewController(
        this.bundleRepositoryPackageFailure(repositoryPackage, result)))
        .pushChangeHandler(HorizontalChangeHandler())
        .popChangeHandler(HorizontalChangeHandler()))
  }

  private fun bundleRepositoryPackageFailure(
    repositoryPackage: InventoryRepositoryPackageType,
    result: InventoryPackageInstallResult): InventoryFailureReport {

    val resources = this.applicationContext!!.resources
    val attributes = TreeMap<String, String>()

    attributes[resources.getString(R.string.install_failure_repository)] =
      result.repositoryId.toString()
    attributes[resources.getString(R.string.install_failure_package)] =
      "${repositoryPackage.id} ${repositoryPackage.versionName} (${repositoryPackage.versionCode})"
    attributes[resources.getString(R.string.install_failure_package_uri)] =
      repositoryPackage.sourceURI.toString()

    return InventoryFailureReport(
      title = resources.getString(R.string.install_failure_title),
      attributes = attributes.toSortedMap(),
      taskSteps = result.steps)
  }

  override fun onDetach(view: View) {
    super.onDetach(view)

    this.repositoryEventSubscription?.dispose()
  }
}
