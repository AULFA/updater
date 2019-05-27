package au.org.libraryforall.updater.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import au.org.libraryforall.updater.inventory.api.InventoryEvent
import au.org.libraryforall.updater.inventory.api.InventoryEvent.InventoryRepositoryEvent.InventoryRepositoryPackageEvent.PackageBecameInvisible
import au.org.libraryforall.updater.inventory.api.InventoryEvent.InventoryRepositoryEvent.InventoryRepositoryPackageEvent.PackageBecameVisible
import au.org.libraryforall.updater.inventory.api.InventoryEvent.InventoryRepositoryEvent.InventoryRepositoryPackageEvent.PackageChanged
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.HorizontalChangeHandler
import io.reactivex.disposables.Disposable
import org.slf4j.LoggerFactory
import java.util.UUID

class InventoryViewController(arguments: Bundle) : Controller(arguments) {

  constructor(repository: UUID) : this(bundleArguments(repository))

  companion object {
    private fun bundleArguments(repository: UUID): Bundle {
      val bundle = Bundle()
      bundle.putSerializable("repositoryUUID", repository)
      return bundle
    }
  }

  private val logger = LoggerFactory.getLogger(InventoryViewController::class.java)
  private var subscription: Disposable? = null
  private val inventory = MainServices.inventory()
  private val repositoryUUID: UUID = arguments.getSerializable("repositoryUUID") as UUID

  init {
    this.setHasOptionsMenu(true)
  }

  private lateinit var title: TextView
  private lateinit var recyclerView: RecyclerView
  private lateinit var listAdapter: InventoryListAdapter

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
    val layout =
      inflater.inflate(R.layout.inventory, container, false)

    this.recyclerView =
      layout.findViewById(R.id.inventoryPackageList)
    this.title =
      layout.findViewById(R.id.inventoryTitle)

    return layout
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    super.onCreateOptionsMenu(menu, inflater)
    inflater.inflate(R.menu.inventory, menu)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.inventoryVersion -> {
        this.router.pushController(
          RouterTransaction.with(ApplicationVersionController())
            .pushChangeHandler(HorizontalChangeHandler())
            .popChangeHandler(HorizontalChangeHandler()))
        true
      }

      else -> super.onOptionsItemSelected(item)
    }
  }

  override fun onAttach(view: View) {
    super.onAttach(view)

    val repository =
      this.inventory.inventoryRepositorySelect(this.repositoryUUID)

    if (repository == null) {
      this.router.popCurrentController()
      return
    }

    this.title.text = repository.title
    this.listAdapter =
      InventoryListAdapter(
        context = this.activity!!,
        packages = repository.packages,
        onShowFailureDetails = { repositoryPackage, result ->
          this.router.pushController(
            RouterTransaction.with(InventoryInstallFailureController(result))
              .pushChangeHandler(HorizontalChangeHandler())
              .popChangeHandler(HorizontalChangeHandler()))
        })

    this.recyclerView.setHasFixedSize(true)
    this.recyclerView.layoutManager = LinearLayoutManager(view.context);
    this.recyclerView.adapter = this.listAdapter
    (this.recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

    this.subscription = repository.events.subscribe(this::onRepositoryEvent)
  }

  private fun onRepositoryEvent(event: InventoryEvent) {
    this.logger.debug("event: {}", event)

    return when (event) {
      is InventoryEvent.InventoryRepositoryEvent ->
        if (event.repositoryId == this.repositoryUUID) {
          when (event) {
            is PackageBecameVisible,
            is PackageChanged,
            is PackageBecameInvisible -> {
              UIThread.execute {
                this.listAdapter.notifyDataSetChanged()
              }
            }
          }
        } else {

        }
    }
  }

  override fun onDetach(view: View) {
    super.onDetach(view)

    this.subscription?.dispose()
  }
}
