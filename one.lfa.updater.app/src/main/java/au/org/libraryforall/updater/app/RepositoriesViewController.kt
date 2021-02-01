package au.org.libraryforall.updater.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
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
import one.lfa.updater.inventory.api.InventoryHashIndexedDirectoryType
import one.lfa.updater.inventory.api.InventoryRepositoryType
import one.lfa.updater.inventory.api.InventoryState
import one.lfa.updater.inventory.api.InventoryTaskStep
import one.lfa.updater.inventory.api.InventoryType
import one.lfa.updater.services.api.Services
import java.net.URI
import java.util.TreeMap

class RepositoriesViewController : Controller() {

  init {
    this.setHasOptionsMenu(true)
  }

  private lateinit var backgroundExecutor: BackgroundExecutor
  private lateinit var inventory: InventoryType
  private lateinit var isEmpty: TextView
  private lateinit var listAdapter: RepositoryListAdapter
  private lateinit var recyclerView: RecyclerView
  private var acceptedRepositoryError: Boolean = false
  private var repositoryEventSubscription: Disposable? = null
  private var showTestingRepositorySubscription: Disposable? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup,
    savedViewState: Bundle?
  ): View {
    val layout =
      inflater.inflate(R.layout.repositories, container, false)

    this.recyclerView =
      layout.findViewById(R.id.repositoryList)
    this.isEmpty =
      layout.findViewById(R.id.repositoryListIsEmpty)

    return layout
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    super.onCreateOptionsMenu(menu, inflater)
    inflater.inflate(R.menu.repositories, menu)
  }

  private fun onSelectedVersion() {
    this.router.pushController(
      RouterTransaction.with(ApplicationVersionController())
        .pushChangeHandler(HorizontalChangeHandler())
        .popChangeHandler(HorizontalChangeHandler()))
  }

  private fun onSelectedRepositoryAdd() {
    this.router.pushController(
      RouterTransaction.with(RepositoryAddViewController())
        .pushChangeHandler(HorizontalChangeHandler())
        .popChangeHandler(HorizontalChangeHandler()))
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.menuItemOverview -> {
        this.onSelectedOverview()
        true
      }

      R.id.menuItemVersion -> {
        this.onSelectedVersion()
        true
      }

      R.id.menuItemRepositoryAdd -> {
        this.onSelectedRepositoryAdd()
        true
      }

      R.id.menuItemDeleteCachedData -> {
        this.onSelectedDeleteCachedData()
        true
      }

      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun onSelectedOverview() {
    this.router.pushController(
      RouterTransaction.with(OverviewViewController())
        .pushChangeHandler(HorizontalChangeHandler())
        .popChangeHandler(HorizontalChangeHandler()))
  }

  private fun onSelectedDeleteCachedData() {
    AlertDialog.Builder(this.activity!!)
      .setTitle(R.string.delete_cached_confirm_title)
      .setMessage(R.string.delete_cached_confirm)
      .setPositiveButton(R.string.delete_cached) { dialog, which ->
        val future = this.inventory.inventoryDeleteCachedData()
        future.addListener(Runnable {
          this.onDeletedCachedData(future.get())
        }, this.backgroundExecutor.executor)
      }
      .show()
  }

  private fun onDeletedCachedData(deletedFiles: List<InventoryHashIndexedDirectoryType.Deleted>) {
    UIThread.execute {
      val deletedSize =
        deletedFiles.fold(0.0, { acc, deleted -> acc + deleted.size })

      val message =
        this.resources!!.getString(
          R.string.deleted_cached,
          deletedFiles.size,
          deletedSize / 1_000_000.0)

      AlertDialog.Builder(this.activity!!)
        .setTitle(R.string.deleted_cached_title)
        .setMessage(message)
        .show()
    }
  }

  override fun onAttach(view: View) {
    super.onAttach(view)

    val serviceDirectory =
      Services.serviceDirectory()
    this.inventory =
      serviceDirectory.requireService(InventoryType::class.java)
    this.backgroundExecutor =
      serviceDirectory.requireService(BackgroundExecutor::class.java)

    this.setOptionsMenuHidden(false)
    (this.activity as AppCompatActivity).supportActionBar?.title =
      view.context.resources.getString(R.string.main_title)

    this.recyclerView.visibility = View.VISIBLE
    this.isEmpty.visibility = View.INVISIBLE

    this.listAdapter =
      RepositoryListAdapter(
        onItemClicked = this::onSelectedRepository,
        onItemFilter = this::onFilterRepository
      )

    this.recyclerView.setHasFixedSize(true)
    this.recyclerView.layoutManager = LinearLayoutManager(view.context)
    this.recyclerView.adapter = this.listAdapter
    (this.recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

    this.repositoryEventSubscription =
      this.inventory.events.ofType(InventoryEvent.InventoryStateChanged::class.java)
        .subscribe { this.onInventoryStateChanged() }

    this.showTestingRepositorySubscription =
      MainDeveloperSettings.showTestingRepositories.subscribe { showTestingRepositories ->
        UIThread.execute {
          this.onInventoryStateChanged()
        }
      }

    this.onInventoryStateChanged()
  }

  private fun onInventoryStateChanged() {
    UIThread.execute {
      val currentRepositories = this.inventory.inventoryRepositories()
      this.listAdapter.submitList(currentRepositories)

      if (this.listAdapter.itemCount > 0) {
        this.recyclerView.visibility = View.VISIBLE
        this.isEmpty.visibility = View.INVISIBLE
      } else {
        this.recyclerView.visibility = View.INVISIBLE
        this.isEmpty.visibility = View.VISIBLE
      }

      when (val state = this.inventory.state) {
        InventoryState.InventoryIdle,
        is InventoryState.InventoryAddingRepository -> {
          this.acceptedRepositoryError = false
        }
        is InventoryState.InventoryAddingRepositoryFailed -> {
          if (!this.acceptedRepositoryError) {
            this.acceptedRepositoryError = true
            AlertDialog.Builder(this.activity!!)
              .setTitle(R.string.repository_add_failed)
              .setMessage(R.string.repository_add_failed)
              .setNeutralButton(R.string.package_details) { dialog, which ->
                this.showErrorDetails(state.uri, state.steps)
              }.show()
          }
        }
      }
    }
  }

  private fun showErrorDetails(uri: URI, steps: List<InventoryTaskStep>) {
    this.router.pushController(
      RouterTransaction.with(InventoryFailureViewController(this.bundleErrorDetails(uri, steps)))
        .pushChangeHandler(HorizontalChangeHandler())
        .popChangeHandler(HorizontalChangeHandler()))
  }

  private fun bundleErrorDetails(
    uri: URI,
    steps: List<InventoryTaskStep>
  ): InventoryFailureReport {

    val resources = this.applicationContext!!.resources
    val attributes = TreeMap<String, String>()
    attributes[resources.getString(R.string.install_failure_repository)] = uri.toString()
    return InventoryFailureReport(
      title = resources.getString(R.string.repository_add_failed),
      attributes = attributes.toSortedMap(),
      taskSteps = steps)
  }

  private fun onFilterRepository(
    repository: InventoryRepositoryType
  ): Boolean {
    val showTesting = MainDeveloperSettings.areTestingRepositoriesShown()
    if (showTesting) {
      return true
    }
    return !repository.isTesting
  }

  private fun onSelectedRepository(repository: InventoryRepositoryType) {
    this.setOptionsMenuHidden(true)

    this.router.pushController(
      RouterTransaction.with(RepositoryViewController(repository.id))
        .pushChangeHandler(HorizontalChangeHandler())
        .popChangeHandler(HorizontalChangeHandler()))
  }

  override fun onDetach(view: View) {
    super.onDetach(view)

    this.repositoryEventSubscription?.dispose()
    this.showTestingRepositorySubscription?.dispose()
  }
}
