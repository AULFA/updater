package au.org.libraryforall.updater.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.HorizontalChangeHandler
import io.reactivex.disposables.Disposable
import one.lfa.updater.inventory.api.InventoryEvent
import one.lfa.updater.inventory.api.InventoryFailureReport
import one.lfa.updater.inventory.api.InventoryItemResult
import one.lfa.updater.inventory.api.InventoryItemState
import one.lfa.updater.inventory.api.InventoryRepositoryItemType
import one.lfa.updater.inventory.api.InventoryType
import one.lfa.updater.services.api.Services
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

  private lateinit var inventory: InventoryType
  private val logger = LoggerFactory.getLogger(OverviewViewController::class.java)
  private var repositoryEventSubscription: Disposable? = null

  init {
    this.setHasOptionsMenu(true)
  }

  private lateinit var listPackages: MutableList<InventoryRepositoryItemType>
  private lateinit var recyclerView: RecyclerView
  private lateinit var listAdapter: InventoryListAdapter

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup,
    savedViewState: Bundle?
  ): View {
    val layout =
      inflater.inflate(R.layout.overview, container, false)

    this.recyclerView =
      layout.findViewById(R.id.overviewPackageList)

    return layout
  }

  override fun onAttach(view: View) {
    super.onAttach(view)

    this.inventory =
      Services.serviceDirectory()
        .requireService(InventoryType::class.java)

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
    this.recyclerView.layoutManager = LinearLayoutManager(view.context)
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
      mutableMapOf<String, InventoryRepositoryItemType>()

    for (repository in this.inventory.inventoryRepositories()) {
      for (packageCurrent in repository.items) {
        if (this.packageIsSuitableForOverview(packageCurrent)) {
          val existing = updates[packageCurrent.item.id]
          if (existing == null || packageCurrent.item.versionCode > existing.item.versionCode) {
            updates[packageCurrent.item.id] = packageCurrent
          }
        }
      }
    }

    val resultUpdates =
      updates.values.sortedBy { p -> p.item.name }

    UIThread.execute {
      this.listPackages.clear()
      this.listPackages.addAll(resultUpdates)
      this.listAdapter.notifyDataSetChanged()
    }
  }

  private fun packageIsSuitableForOverview(packageCurrent: InventoryRepositoryItemType) =
    packageCurrent.isUpdateAvailable || when (packageCurrent.state) {
      is InventoryItemState.Installed -> false
      is InventoryItemState.Failed -> true
      is InventoryItemState.NotInstalled -> false
      is InventoryItemState.Operating.Installing -> true
      is InventoryItemState.Operating.Uninstalling -> true
    }

  private fun showRepositoryPackageFailure(
    repositoryPackage: InventoryRepositoryItemType,
    result: InventoryItemResult
  ) {
    this.router.pushController(
      RouterTransaction.with(InventoryFailureViewController(
        this.bundleRepositoryPackageFailure(repositoryPackage, result)))
        .pushChangeHandler(HorizontalChangeHandler())
        .popChangeHandler(HorizontalChangeHandler()))
  }

  private fun bundleRepositoryPackageFailure(
    repositoryPackage: InventoryRepositoryItemType,
    result: InventoryItemResult): InventoryFailureReport {

    val resources = this.applicationContext!!.resources
    val attributes = TreeMap<String, String>()

    attributes[resources.getString(R.string.install_failure_repository)] =
      result.repositoryId.toString()
    attributes[resources.getString(R.string.install_failure_package)] =
      "${repositoryPackage.item.id} ${repositoryPackage.item.versionName} (${repositoryPackage.item.versionCode})"
    attributes[resources.getString(R.string.install_failure_package_uri)] =
      repositoryPackage.item.source.toString()

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
