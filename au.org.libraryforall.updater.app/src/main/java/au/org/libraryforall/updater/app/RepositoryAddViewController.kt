package au.org.libraryforall.updater.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import au.org.libraryforall.updater.inventory.api.InventoryEvent
import au.org.libraryforall.updater.inventory.api.InventoryState
import au.org.libraryforall.updater.inventory.api.InventoryTaskStep
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.HorizontalChangeHandler
import io.reactivex.disposables.Disposable
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.TreeMap

class RepositoryAddViewController : Controller() {

  private lateinit var errorDetails: Button
  private lateinit var errorConfirm: Button
  private lateinit var groupError: ViewGroup
  private lateinit var groupAdding: ViewGroup
  private lateinit var groupIdle: ViewGroup
  private lateinit var title: TextView
  private lateinit var uriInput: EditText
  private lateinit var idleConfirm: Button

  private var eventSubscription: Disposable? = null
  private val logger = LoggerFactory.getLogger(RepositoryAddViewController::class.java)

  private val inventory = MainServices.inventory()

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
    val layout =
      inflater.inflate(R.layout.repository_add, container, false)

    this.title =
      layout.findViewById(R.id.repositoryAddTitle)
    this.uriInput =
      layout.findViewById(R.id.repositoryAddURI)

    this.groupIdle =
      layout.findViewById(R.id.repositoryIdle)
    this.groupAdding =
      layout.findViewById(R.id.repositoryAdding)
    this.groupError =
      layout.findViewById(R.id.repositoryError)

    this.idleConfirm =
      this.groupIdle.findViewById(R.id.repositoryIdleAddConfirm)

    this.errorDetails =
      this.groupError.findViewById(R.id.repositoryErrorAddDetails)
    this.errorConfirm =
      this.groupError.findViewById(R.id.repositoryErrorAddConfirm)

    return layout
  }

  override fun onAttach(view: View) {
    super.onAttach(view)

    this.setOptionsMenuHidden(true)

    (this.activity as AppCompatActivity).supportActionBar?.title =
      view.context.resources.getString(R.string.repository_add_title)

    this.eventSubscription =
      this.inventory.events.ofType(InventoryEvent.InventoryStateChanged::class.java)
        .subscribe { this.onInventoryStateChanged() }

    this.configureUIForState(this.inventory.state)

    this.idleConfirm.setOnClickListener(this::tryAdd)
    this.errorConfirm.setOnClickListener(this::tryAdd)
  }

  private fun tryAdd(view: View) {
    val uri = try {
      URI(this.uriInput.text.toString())
    } catch (e: Exception) {
      this.uriInput.error = view.context.getString(R.string.repository_uri_invalid)
      null
    }

    if (uri != null) {
      this.uriInput.error = null
      this.uriInput.isEnabled = false
      this.idleConfirm.isEnabled = false
      this.errorConfirm.isEnabled = false
      this.inventory.inventoryRepositoryAdd(uri)
    }
  }

  private fun onInventoryStateChanged() {
    UIThread.execute {
      this.configureUIForState(this.inventory.state)
    }
  }

  private fun configureUIForState(state: InventoryState) {
    when (state) {
      InventoryState.InventoryIdle -> {
        this.groupAdding.visibility = View.INVISIBLE
        this.groupError.visibility = View.INVISIBLE
        this.groupIdle.visibility = View.VISIBLE
        this.uriInput.isEnabled = true
        this.idleConfirm.isEnabled = true
        this.errorConfirm.isEnabled = true
      }

      is InventoryState.InventoryAddingRepository -> {
        this.groupAdding.visibility = View.VISIBLE
        this.groupError.visibility = View.INVISIBLE
        this.groupIdle.visibility = View.INVISIBLE
        this.uriInput.isEnabled = false
        this.idleConfirm.isEnabled = false
        this.errorConfirm.isEnabled = false
      }

      is InventoryState.InventoryAddingRepositoryFailed -> {
        this.groupAdding.visibility = View.INVISIBLE
        this.groupError.visibility = View.VISIBLE
        this.groupIdle.visibility = View.INVISIBLE
        this.uriInput.isEnabled = true
        this.idleConfirm.isEnabled = true
        this.errorConfirm.isEnabled = true
        this.errorDetails.setOnClickListener {
          this.showErrorDetails(state.uri, state.steps)
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
  ): InventoryFailure {

    val resources = this.applicationContext!!.resources
    val attributes = TreeMap<String, String>()

    attributes[resources.getString(R.string.install_failure_repository)] = uri.toString()
    return InventoryFailure(
      title = resources.getString(R.string.repository_add_failed),
      attributes = attributes.toSortedMap(),
      taskSteps = steps)
  }

  override fun onDetach(view: View) {
    super.onDetach(view)

    this.eventSubscription?.dispose()
  }
}
