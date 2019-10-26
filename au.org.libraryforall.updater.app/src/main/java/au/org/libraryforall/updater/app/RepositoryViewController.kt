package au.org.libraryforall.updater.app

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
import au.org.libraryforall.updater.inventory.api.InventoryEvent
import au.org.libraryforall.updater.inventory.api.InventoryEvent.InventoryRepositoryEvent
import au.org.libraryforall.updater.inventory.api.InventoryEvent.InventoryRepositoryEvent.InventoryRepositoryItemEvent.ItemBecameInvisible
import au.org.libraryforall.updater.inventory.api.InventoryEvent.InventoryRepositoryEvent.InventoryRepositoryItemEvent.ItemBecameVisible
import au.org.libraryforall.updater.inventory.api.InventoryEvent.InventoryRepositoryEvent.InventoryRepositoryItemEvent.ItemChanged
import au.org.libraryforall.updater.inventory.api.InventoryEvent.InventoryRepositoryEvent.RepositoryChanged
import au.org.libraryforall.updater.inventory.api.InventoryFailureReport
import au.org.libraryforall.updater.inventory.api.InventoryItemInstallResult
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryItemType
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryState.RepositoryIdle
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryState.RepositoryUpdateFailed
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryState.RepositoryUpdating
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryType
import au.org.libraryforall.updater.inventory.api.InventoryTaskStep
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.HorizontalChangeHandler
import io.reactivex.disposables.Disposable
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
  private val inventory = MainServices.inventory()
  private val repositoryUUID: UUID = arguments.getSerializable("repositoryUUID") as UUID

  init {
    this.setHasOptionsMenu(true)
  }

  private lateinit var listPackages: MutableList<InventoryRepositoryItemType>
  private lateinit var title: TextView
  private lateinit var recyclerView: RecyclerView
  private lateinit var listAdapter: InventoryListAdapter
  private lateinit var progressError: ImageView
  private lateinit var progess: ProgressBar
  private lateinit var repository: InventoryRepositoryType

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
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
      .setPositiveButton(R.string.repository_delete, { dialog, which ->
        this.inventory.inventoryRepositoryRemove(this.repositoryUUID)
        this.router.popCurrentController()
      })
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
    this.recyclerView.layoutManager = LinearLayoutManager(view.context);
    this.recyclerView.adapter = this.listAdapter
    (this.recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

    this.progess.visibility = View.INVISIBLE
    this.repositoryEventSubscription = this.repository.events.subscribe(this::onRepositoryEvent)
  }

  private fun showRepositoryPackageFailure(
    repositoryPackage: InventoryRepositoryItemType,
    result: InventoryItemInstallResult
  ) {
    this.router.pushController(
      RouterTransaction.with(InventoryFailureViewController(
        this.bundleRepositoryPackageFailure(repositoryPackage, result)))
        .pushChangeHandler(HorizontalChangeHandler())
        .popChangeHandler(HorizontalChangeHandler()))
  }

  private fun bundleRepositoryPackageFailure(
    repositoryPackage: InventoryRepositoryItemType,
    result: InventoryItemInstallResult): InventoryFailureReport {

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

  private fun onRepositoryEvent(event: InventoryEvent) {
    this.logger.trace("event: {}", event)

    return when (event) {
      is InventoryRepositoryEvent ->
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
      is RepositoryUpdating -> {
        this.progess.visibility = View.VISIBLE
        this.progressError.visibility = View.INVISIBLE
      }
      is RepositoryUpdateFailed -> {
        this.progess.visibility = View.INVISIBLE
        this.progressError.visibility = View.VISIBLE
        this.progressError.setOnClickListener {
          this.showRepositoryUpdateFailure(state.steps)
        }
      }
      is RepositoryIdle -> {
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
