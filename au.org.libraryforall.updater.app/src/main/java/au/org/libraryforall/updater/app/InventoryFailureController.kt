package au.org.libraryforall.updater.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TableLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.bluelinelabs.conductor.Controller
import org.slf4j.LoggerFactory

class InventoryFailureController(arguments: Bundle) : Controller(arguments) {

  constructor(failure: InventoryFailure) : this(bundleArguments(failure))

  companion object {
    private fun bundleArguments(failure: InventoryFailure): Bundle {
      val bundle = Bundle()
      bundle.putSerializable("failure", failure)
      return bundle
    }
  }

  private val logger = LoggerFactory.getLogger(InventoryFailureController::class.java)

  private lateinit var titleView: TextView
  private lateinit var tableView: TableLayout
  private lateinit var saveReport: Button
  private lateinit var recyclerView: RecyclerView
  private lateinit var adapter: InventoryFailureListAdapter

  private val failure =
    arguments.getSerializable("failure") as InventoryFailure

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
    val mainLayout =
      inflater.inflate(R.layout.failure, container, false)

    this.titleView =
      mainLayout.findViewById(R.id.failureTitle)
    this.tableView =
      mainLayout.findViewById(R.id.failureTable)
    this.recyclerView =
      mainLayout.findViewById(R.id.failureSteps)
    this.saveReport =
      mainLayout.findViewById(R.id.failureSaveReport)

    return mainLayout
  }

  override fun onAttach(view: View) {
    super.onAttach(view)

    this.adapter =
      InventoryFailureListAdapter(view.context, this.failure.taskSteps)
    this.titleView.text =
      this.failure.title

    val inflater =
      LayoutInflater.from(view.context)

    this.tableView.removeAllViews()

    for (key in this.failure.attributes.keys) {
      val value = this.failure.attributes[key]

      val tableRow =
        inflater.inflate(R.layout.failure_attribute_row, this.tableView, false)
      val tableCell0 =
        tableRow.findViewById<TextView>(R.id.failureAttributeKey)
      val tableCell1 =
        tableRow.findViewById<TextView>(R.id.failureAttributeValue)

      tableCell0.text = key
      tableCell1.text = value
      this.tableView.addView(tableRow)
    }

    this.recyclerView.setHasFixedSize(false)
    this.recyclerView.layoutManager = LinearLayoutManager(view.context);
    this.recyclerView.adapter = this.adapter
    (this.recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
  }
}
