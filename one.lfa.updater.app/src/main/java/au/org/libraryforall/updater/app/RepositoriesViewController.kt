package au.org.libraryforall.updater.app

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
import au.org.libraryforall.updater.inventory.api.InventoryHashIndexedDirectoryType
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
  private val listRepositories: MutableList<InventoryRepositoryType> = mutableListOf()

  private var repositoryEventSubscription: Disposable? = null

  private lateinit var isEmpty: TextView
  private lateinit var recyclerView: RecyclerView
  private lateinit var listAdapter: RepositoryListAdapter

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
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
      .setPositiveButton(R.string.delete_cached, { dialog, which ->
        val future = this.inventory.inventoryDeleteCachedData()
        future.addListener(Runnable {
          this.onDeletedCachedData(future.get())
        }, MainServices.backgroundExecutor())
      })
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

    this.setOptionsMenuHidden(false)
    (this.activity as AppCompatActivity).supportActionBar?.title =
      view.context.resources.getString(R.string.main_title)

    this.recyclerView.visibility = View.VISIBLE
    this.isEmpty.visibility = View.INVISIBLE

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

    this.onInventoryStateChanged()
  }

  private fun onInventoryStateChanged() {
    UIThread.execute {
      this.listRepositories.clear()

      val currentRepositories = this.inventory.inventoryRepositories()
      if (currentRepositories.isEmpty()) {
        this.recyclerView.visibility = View.INVISIBLE
        this.isEmpty.visibility = View.VISIBLE
      } else {
        this.recyclerView.visibility = View.VISIBLE
        this.isEmpty.visibility = View.INVISIBLE
        this.listRepositories.addAll(currentRepositories)
        this.listAdapter.notifyDataSetChanged()
      }
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
