package au.org.libraryforall.updater.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
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
import one.lfa.updater.inventory.api.InventoryEvent.InventoryRepositoryEvent.InventoryRepositoryItemEvent.ItemBecameInvisible
import one.lfa.updater.inventory.api.InventoryEvent.InventoryRepositoryEvent.InventoryRepositoryItemEvent.ItemBecameVisible
import one.lfa.updater.inventory.api.InventoryEvent.InventoryRepositoryEvent.InventoryRepositoryItemEvent.ItemChanged
import one.lfa.updater.inventory.api.InventoryEvent.InventoryRepositoryEvent.RepositoryChanged
import one.lfa.updater.inventory.api.InventoryFailureReport
import one.lfa.updater.inventory.api.InventoryItemResult
import one.lfa.updater.inventory.api.InventoryRepositoryItemType
import one.lfa.updater.inventory.api.InventoryRepositoryState
import one.lfa.updater.inventory.api.InventoryRepositoryType
import one.lfa.updater.inventory.api.InventoryTaskStep
import one.lfa.updater.inventory.api.InventoryType
import one.lfa.updater.services.api.Services
import org.slf4j.LoggerFactory
import java.util.TreeMap
import java.util.UUID

class RepositoryViewController(arguments: Bundle) : Controller(arguments) {

  constructor(repository: UUID) : this(bundleArguments(repository))

  companion object {
    private fun bundleArguments(repository: UUID): Bundle {
      val bundle = Bundle()
      bundle.putSerializable("repositoryUUID", repository)
      return bundle
    }
  }

  private val logger = LoggerFactory.getLogger(RepositoryViewController::class.java)
  private var repositoryEventSubscription: Disposable? = null
  private val repositoryUUID: UUID = arguments.getSerializable("repositoryUUID") as UUID

  init {
    this.setHasOptionsMenu(true)
  }

  private lateinit var inventory: InventoryType
  private lateinit var listAdapter: InventoryListAdapter
  private lateinit var listPackages: MutableList<InventoryRepositoryItemType>
  private lateinit var progess: ProgressBar
  private lateinit var progressError: ImageView
  private lateinit var recyclerView: RecyclerView
  private lateinit var repository: InventoryRepositoryType
  private lateinit var title: TextView

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup,
    savedViewState: Bundle?
  ): View {
    val layout =
      inflater.inflate(R.layout.inventory_repository, container, false)

    this.progess =
      layout.findViewById(R.id.inventoryProgress)
    this.progressError =
      layout.findViewById(R.id.inventoryProgressError)
    this.recyclerView =
      layout.findViewById(R.id.inventoryPackageList)
    this.title =
      layout.findViewById(R.id.inventoryTitle)

    return layout
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    super.onCreateOptionsMenu(menu, inflater)
    inflater.inflate(R.menu.repository, menu)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.menuItemRepositoryReload -> {
        this.onSelectedReload()
        return true
      }

      R.id.menuItemRepositoryDelete -> {
        this.onSelectedDelete()
        return true
      }

      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun onSelectedDelete() {
    AlertDialog.Builder(this.activity!!)
      .setTitle(R.string.repository_delete_confirm_title)
      .setMessage(R.string.repository_delete_confirm)
      .setPositiveButton(R.string.repository_delete) { dialog, which ->
        this.inventory.inventoryRepositoryRemove(this.repositoryUUID)
        this.router.popCurrentController()
      }
      .show()
  }

  private fun onSelectedReload() {
    val repository =
      this.inventory.inventoryRepositorySelect(this.repositoryUUID)

    if (repository == null) {
      this.router.popCurrentController()
      return
    }

    repository.update()
  }

  override fun onAttach(view: View) {
    super.onAttach(view)

    val serviceDirectory =
      Services.serviceDirectory()
    this.inventory =
      serviceDirectory.requireService(InventoryType::class.java)

    (this.activity as AppCompatActivity).supportActionBar?.title =
      view.context.resources.getString(R.string.repository)
    this.setOptionsMenuHidden(false)

    val currentRepository =
      this.inventory.inventoryRepositorySelect(this.repositoryUUID)

    if (currentRepository == null) {
      this.router.popCurrentController()
      return
    }
    this.repository = currentRepository

    this.title.text = this.repository.title

    this.listPackages = mutableListOf()
    this.listPackages.addAll(this.repository.items)
    this.listAdapter =
      InventoryListAdapter(
        context = this.activity!!,
        packages = this.listPackages,
        onShowFailureDetails = this@RepositoryViewController::showRepositoryPackageFailure)

    this.recyclerView.setHasFixedSize(true)
    this.recyclerView.layoutManager = LinearLayoutManager(view.context)
    this.recyclerView.adapter = this.listAdapter
    (this.recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

    this.progess.visibility = View.INVISIBLE
    this.repositoryEventSubscription = this.repository.events.subscribe(this::onRepositoryEvent)
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

  private fun onRepositoryEvent(event: InventoryEvent) {
    this.logger.trace("event: {}", event)

    return when (event) {
      is InventoryEvent.InventoryRepositoryEvent ->
        if (event.repositoryId == this.repositoryUUID) {
          when (event) {
            is ItemBecameVisible,
            is ItemChanged,
            is ItemBecameInvisible -> {
              UIThread.execute {
                this.listPackages.clear()
                this.listPackages.addAll(this.repository.items)
                this.listAdapter.notifyDataSetChanged()
              }
            }
            is RepositoryChanged -> {
              UIThread.execute {
                this.onRepositoryChangedUI()
              }
            }
          }
        } else {

        }
      InventoryEvent.InventoryStateChanged -> Unit
    }
  }

  private fun onRepositoryChangedUI() {
    return when (val state = this.repository.state) {
      is InventoryRepositoryState.RepositoryUpdating -> {
        this.progess.visibility = View.VISIBLE
        this.progressError.visibility = View.INVISIBLE
      }
      is InventoryRepositoryState.RepositoryUpdateFailed -> {
        this.progess.visibility = View.INVISIBLE
        this.progressError.visibility = View.VISIBLE
        this.progressError.setOnClickListener {
          this.showRepositoryUpdateFailure(state.steps)
        }
      }
      is InventoryRepositoryState.RepositoryIdle -> {
        this.progess.visibility = View.INVISIBLE
        this.progressError.visibility = View.INVISIBLE
      }
    }
  }

  private fun bundleRepositoryUpdateFailure(
    steps: List<InventoryTaskStep>): InventoryFailureReport {

    val resources = this.applicationContext!!.resources
    val attributes = TreeMap<String, String>()

    attributes[resources.getString(R.string.install_failure_repository)] =
      this.repository.id.toString()

    return InventoryFailureReport(
      title = resources.getString(R.string.inventory_repository_update_failed_title),
      attributes = attributes.toSortedMap(),
      taskSteps = steps)
  }

  private fun showRepositoryUpdateFailure(steps: List<InventoryTaskStep>) {
    this.setOptionsMenuHidden(true)

    this.router.pushController(
      RouterTransaction.with(InventoryFailureViewController(
        this.bundleRepositoryUpdateFailure(steps)))
        .pushChangeHandler(HorizontalChangeHandler())
        .popChangeHandler(HorizontalChangeHandler()))
  }

  override fun onDetach(view: View) {
    super.onDetach(view)

    this.repositoryEventSubscription?.dispose()
  }
}
