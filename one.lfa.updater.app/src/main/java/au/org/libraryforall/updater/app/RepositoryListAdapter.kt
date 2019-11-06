package au.org.libraryforall.updater.app

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import one.lfa.updater.inventory.api.InventoryRepositoryType

class RepositoryListAdapter(
  private val context: Activity,
  private val repositories: List<InventoryRepositoryType>,
  private val onItemClicked: (InventoryRepositoryType) -> Unit)
  : RecyclerView.Adapter<RepositoryListAdapter.ViewHolder>() {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

    val inflater =
      LayoutInflater.from(parent.context)
    val item =
      inflater.inflate(R.layout.repository, parent, false)

    return this.ViewHolder(item)
  }

  override fun getItemCount(): Int =
    this.repositories.size

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val repository = this.repositories[position]

    holder.parent.setOnClickListener { this.onItemClicked.invoke(repository) }
    holder.titleView.text = repository.title
    holder.uriView.text = repository.updateURI.toString()
  }

  inner class ViewHolder(val parent: View) : RecyclerView.ViewHolder(parent) {
    val titleView =
      parent.findViewById<TextView>(R.id.repositoryName)
    val uriView =
      parent.findViewById<TextView>(R.id.repositoryURI)
  }
}