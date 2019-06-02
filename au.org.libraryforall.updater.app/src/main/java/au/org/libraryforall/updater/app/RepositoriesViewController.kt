package au.org.libraryforall.updater.app

import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import au.org.libraryforall.updater.inventory.api.InventoryEvent
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryType
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.HorizontalChangeHandler
import io.reactivex.disposables.Disposable
import org.slf4j.LoggerFactory

class RepositoriesViewController : Controller() {

  init {
    this.setHasOptionsMenu(true)
  }

  private val logger = LoggerFactory.getLogger(RepositoriesViewController::class.java)
  private val inventory = MainServices.inventory()

  private var repositoryEventSubscription: Disposable? = null

  private lateinit var listRepositories: MutableList<InventoryRepositoryType>
  private lateinit var recyclerView: RecyclerView
  private lateinit var listAdapter: RepositoryListAdapter

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
    val layout =
      inflater.inflate(R.layout.repositories, container, false)

    this.recyclerView =
      layout.findViewById(R.id.repositoryList)

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
      R.id.menuItemVersion -> {
        this.onSelectedVersion()
        true
      }
      R.id.menuItemRepositoryAdd -> {
        this.onSelectedRepositoryAdd()
        true
      }

      else -> super.onOptionsItemSelected(item)
    }
  }

  override fun onAttach(view: View) {
    super.onAttach(view)

    this.setOptionsMenuHidden(false)
    (this.activity as AppCompatActivity).supportActionBar?.title =
      view.context.resources.getString(R.string.main_title)

    this.listRepositories = mutableListOf()
    this.listRepositories.addAll(this.inventory.inventoryRepositories())
    this.listAdapter =
      RepositoryListAdapter(
        context = this.activity!!,
        onItemClicked = { repository -> this.onSelectedRepository(repository) },
        repositories = this.listRepositories)

    this.recyclerView.setHasFixedSize(true)
    this.recyclerView.layoutManager = LinearLayoutManager(view.context);
    this.recyclerView.adapter = this.listAdapter
    (this.recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

    this.repositoryEventSubscription =
      this.inventory.events.ofType(InventoryEvent.InventoryStateChanged::class.java)
        .subscribe { this.onInventoryStateChanged() }
  }

  private fun onInventoryStateChanged() {
    UIThread.execute {
      this.listRepositories.clear()
      this.listRepositories.addAll(this.inventory.inventoryRepositories())
      this.listAdapter.notifyDataSetChanged()
    }
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
  }
}
