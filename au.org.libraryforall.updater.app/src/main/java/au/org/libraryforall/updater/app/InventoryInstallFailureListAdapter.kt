package au.org.libraryforall.updater.app

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import au.org.libraryforall.updater.inventory.api.InventoryTaskStep

class InventoryInstallFailureListAdapter(
  private val context: Context,
  private val steps: List<InventoryTaskStep>)
  : RecyclerView.Adapter<InventoryInstallFailureListAdapter.ViewHolder>() {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val inflater =
      LayoutInflater.from(parent.context)
    val item =
      inflater.inflate(R.layout.install_failure_item, parent, false)

    return this.ViewHolder(item)
  }

  override fun getItemCount(): Int =
    this.steps.size

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val step = this.steps[position]

    if (step.failed) {
      holder.icon.setImageResource(R.drawable.error_small)
    } else {
      holder.icon.setImageResource(R.drawable.ok_small)
    }

    holder.stepNumber.text = String.format("%d.", position + 1)
    holder.description.text = step.description
    holder.resolution.text = step.resolution
  }

  inner class ViewHolder(parent: View) : RecyclerView.ViewHolder(parent) {
    val icon =
      parent.findViewById<ImageView>(R.id.installFailureIcon)
    val description =
      parent.findViewById<TextView>(R.id.installFailureDescription)
    val resolution =
      parent.findViewById<TextView>(R.id.installFailureResolution)
    val stepNumber =
      parent.findViewById<TextView>(R.id.installFailureStepNumber)
  }
}