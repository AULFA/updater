package au.org.libraryforall.updater.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import one.lfa.updater.inventory.api.InventoryRepositoryType
import org.slf4j.LoggerFactory

class RepositoryListAdapter(
  private val onItemClicked: (InventoryRepositoryType) -> Unit,
  private val onItemFilter: (InventoryRepositoryType) -> Boolean)
  : ListAdapter<InventoryRepositoryType, RepositoryListAdapter.ViewHolder>(au.org.libraryforall.updater.main.RepositoryListAdapter.Companion.diffUtilCallback) {

  private val logger =
    LoggerFactory.getLogger(RepositoryListAdapter::class.java)

  private var unfilteredData =
    listOf<InventoryRepositoryType>()

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): ViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    val item = inflater.inflate(R.layout.repository, parent, false)
    return this.ViewHolder(item)
  }

  override fun onBindViewHolder(
    holder: ViewHolder,
    position: Int
  ) {
    holder.bind(this.getItem(position), this.onItemClicked)
  }

  override fun submitList(list: List<InventoryRepositoryType>?) {
    this.unfilteredData = list!!.toList()
    val filteredData = list.filter { this.onItemFilter.invoke(it) }
    this.logger.debug("unfiltered: {} items", this.unfilteredData.size)
    this.logger.debug("filtered:   {} items", filteredData.size)
    super.submitList(filteredData)
  }

  override fun submitList(
    list: List<InventoryRepositoryType>?,
    commitCallback: Runnable?
  ) {
    this.unfilteredData = list!!.toList()
    val filteredData = list.filter { this.onItemFilter.invoke(it) }
    this.logger.debug("unfiltered: {} items", this.unfilteredData.size)
    this.logger.debug("filtered:   {} items", filteredData.size)
    super.submitList(filteredData, commitCallback)
  }

  inner class ViewHolder(
    val parent: View
  ) : RecyclerView.ViewHolder(parent) {
    val titleView =
      this.parent.findViewById<TextView>(R.id.repositoryName)
    val uriView =
      this.parent.findViewById<TextView>(R.id.repositoryURI)

    fun bind(
      repository: InventoryRepositoryType,
      onItemClicked: (InventoryRepositoryType) -> Unit
    ) {
      this.parent.setOnClickListener { onItemClicked.invoke(repository) }
      this.titleView.text = repository.title
      this.uriView.text = repository.updateURI.toString()
    }
  }

  companion object {
    private val diffUtilCallback =
      object : DiffUtil.ItemCallback<InventoryRepositoryType>() {
        override fun areItemsTheSame(
          oldItem: InventoryRepositoryType,
          newItem: InventoryRepositoryType
        ): Boolean {
          return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
          oldItem: InventoryRepositoryType,
          newItem: InventoryRepositoryType
        ): Boolean {
          return oldItem.title == newItem.title
        }
      }
  }
}