package au.org.libraryforall.updater.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TableLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.bluelinelabs.conductor.Controller
import one.lfa.updater.inventory.api.InventoryFailureReport
import org.joda.time.Instant
import org.slf4j.LoggerFactory

class InventoryFailureViewController(arguments: Bundle) : Controller(arguments) {

  constructor(failure: InventoryFailureReport) : this(bundleArguments(failure))

  companion object {
    private fun bundleArguments(failure: InventoryFailureReport): Bundle {
      val bundle = Bundle()
      bundle.putSerializable("failure", failure)
      return bundle
    }
  }

  private val logger = LoggerFactory.getLogger(InventoryFailureViewController::class.java)

  private lateinit var titleView: TextView
  private lateinit var tableView: TableLayout
  private lateinit var saveReport: Button
  private lateinit var recyclerView: RecyclerView
  private lateinit var adapter: InventoryFailureListAdapter

  private val failure =
    arguments.getSerializable("failure") as InventoryFailureReport

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup,
    savedViewState: Bundle?
  ): View {
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

    this.setOptionsMenuHidden(true)
    (this.activity as AppCompatActivity).supportActionBar?.title =
      view.context.resources.getString(R.string.error_report)

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
    this.recyclerView.layoutManager = LinearLayoutManager(view.context)
    this.recyclerView.adapter = this.adapter
    (this.recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

    this.saveReport.setOnClickListener {
      try {
        val file =
          InventoryFailureReports.writeToStorage(
            context = view.context,
            time = Instant.now(),
            report = this.failure
          )

        val result =
          ErrorReports.sendReportsDefault(
            context = view.context,
            address = "",
            subject = "[LFA Updater] error report",
            body = ""
          )

        when (result) {
          ErrorReports.Result.NoFiles,
          ErrorReports.Result.Sent -> {
            AlertDialog.Builder(this.activity!!)
              .setTitle(R.string.failure_wrote_ok_title)
              .setMessage(view.context.getString(R.string.failure_wrote_ok, file.toString()))
              .show()
          }
          is ErrorReports.Result.RaisedException ->
            throw result.exception
        }
      } catch (e: Exception) {
        AlertDialog.Builder(this.activity!!)
          .setTitle(R.string.failure_wrote_failed_title)
          .setMessage(view.context.getString(R.string.failure_wrote_failed, e.message, e))
          .show()
      }
    }
  }
}
