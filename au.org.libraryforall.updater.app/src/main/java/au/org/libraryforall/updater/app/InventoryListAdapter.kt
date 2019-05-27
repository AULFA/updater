package au.org.libraryforall.updater.app

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import au.org.libraryforall.updater.inventory.api.InventoryInstallResult
import au.org.libraryforall.updater.inventory.api.InventoryPackageState
import au.org.libraryforall.updater.inventory.api.InventoryPackageState.InstallingStatus.InstallingStatusDefinite
import au.org.libraryforall.updater.inventory.api.InventoryPackageState.InstallingStatus.InstallingStatusIndefinite
import au.org.libraryforall.updater.inventory.api.InventoryRepositoryPackageType

class InventoryListAdapter(
  private val context: Activity,
  private val packages: List<InventoryRepositoryPackageType>,
  private val onShowFailureDetails: (InventoryRepositoryPackageType, InventoryInstallResult) -> Unit)
  : RecyclerView.Adapter<InventoryListAdapter.ViewHolder>() {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

    val inflater =
      LayoutInflater.from(parent.context)
    val item =
      inflater.inflate(R.layout.inventory_item, parent, false)

    return this.ViewHolder(
      parent = item,
      installFailed = item.findViewById(R.id.installFailed),
      installed = item.findViewById(R.id.installed),
      installing = item.findViewById(R.id.installing),
      notInstalled = item.findViewById(R.id.notInstalled))
  }

  class ViewHolderInstalled(val view: View) {
    val packageName =
      view.findViewById<TextView>(R.id.installedPackageName)
    val packageAvailable =
      view.findViewById<TextView>(R.id.installedPackageAvailable)
    val packageButton =
      view.findViewById<Button>(R.id.installedPackageButton)
    val packageIcon =
      view.findViewById<ImageView>(R.id.installedPackageIcon)
    val packageInstalled =
      view.findViewById<TextView>(R.id.installedPackageInstalled)
  }

  class ViewHolderNotInstalled(val view: View) {
    val packageName =
      view.findViewById<TextView>(R.id.notInstalledPackageName)
    val packageAvailable =
      view.findViewById<TextView>(R.id.notInstalledPackageAvailable)
    val packageButton =
      view.findViewById<Button>(R.id.notInstalledPackageButton)
    val packageIcon =
      view.findViewById<ImageView>(R.id.notInstalledPackageIcon)
  }

  class ViewHolderInstalling(val view: View) {
    val packageButton =
      view.findViewById<Button>(R.id.installingPackageButton)
    val packageName =
      view.findViewById<TextView>(R.id.installingPackageName)
    val progressBar =
      view.findViewById<ProgressBar>(R.id.installingProgress)
    val progressState =
      view.findViewById<TextView>(R.id.installingProgressState)
  }

  class ViewHolderInstallFailed(val view: View) {
    val retry =
      view.findViewById<Button>(R.id.installFailedPackageButton)
    val details =
      view.findViewById<Button>(R.id.installFailedPackageDetailsButton)
    val packageName =
      view.findViewById<TextView>(R.id.installFailedPackageName)
  }

  override fun getItemCount(): Int =
    this.packages.size

  companion object {
    @JvmStatic
    private fun setVisibility(view: View, visibility: Int) {
      if (view.visibility != visibility) {
        view.visibility = visibility
      }
    }
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val repositoryPackage = this.packages[position]
    when (val state = repositoryPackage.state) {
      is InventoryPackageState.NotInstalled -> {
        setVisibility(holder.viewHolderInstallFailed.view, View.INVISIBLE)
        setVisibility(holder.viewHolderInstalled.view, View.INVISIBLE)
        setVisibility(holder.viewHolderInstalling.view, View.INVISIBLE)
        setVisibility(holder.viewHolderNotInstalled.view, View.VISIBLE)

        holder.viewHolderNotInstalled.packageName.text = repositoryPackage.name
        holder.viewHolderNotInstalled.packageAvailable.text =
          this.context.resources.getString(
            R.string.package_state_available,
            repositoryPackage.versionName,
            repositoryPackage.versionCode)

        holder.viewHolderNotInstalled.packageButton.isEnabled = true
        holder.viewHolderNotInstalled.packageButton.setOnClickListener {
          holder.viewHolderNotInstalled.packageButton.isEnabled = false
          repositoryPackage.install(this.context)
        }
      }

      is InventoryPackageState.Installed -> {
        setVisibility(holder.viewHolderInstallFailed.view, View.INVISIBLE)
        setVisibility(holder.viewHolderInstalled.view, View.VISIBLE)
        setVisibility(holder.viewHolderInstalling.view, View.INVISIBLE)
        setVisibility(holder.viewHolderNotInstalled.view, View.INVISIBLE)

        if (repositoryPackage.isUpdateAvailable) {
          setVisibility(holder.viewHolderNotInstalled.packageButton, View.VISIBLE)
          holder.viewHolderNotInstalled.packageButton.isEnabled = true
          holder.viewHolderNotInstalled.packageButton.setOnClickListener {
            holder.viewHolderNotInstalled.packageButton.isEnabled = false
            repositoryPackage.install(this.context)
          }
        } else {
          setVisibility(holder.viewHolderNotInstalled.packageButton, View.INVISIBLE)
        }
      }

      is InventoryPackageState.Installing -> {
        setVisibility(holder.viewHolderInstallFailed.view, View.INVISIBLE)
        setVisibility(holder.viewHolderInstalled.view, View.INVISIBLE)
        setVisibility(holder.viewHolderInstalling.view, View.VISIBLE)
        setVisibility(holder.viewHolderNotInstalled.view, View.INVISIBLE)

        holder.viewHolderInstalling.packageButton.isEnabled = true
        holder.viewHolderInstalling.packageButton.setOnClickListener {
          holder.viewHolderInstalling.packageButton.isEnabled = false
          throw NullPointerException()
        }

        val progressState = state.state
        holder.viewHolderInstalling.progressState.text = progressState.status
        holder.viewHolderInstalling.packageName.text = repositoryPackage.name
        when (progressState) {
          is InstallingStatusIndefinite -> {
            holder.viewHolderInstalling.progressBar.isIndeterminate = true
          }
          is InstallingStatusDefinite -> {
            holder.viewHolderInstalling.progressBar.isIndeterminate = false
            holder.viewHolderInstalling.progressBar.progress = progressState.percent.toInt()
          }
        }
      }

      is InventoryPackageState.InstallFailed -> {
        setVisibility(holder.viewHolderInstallFailed.view, View.VISIBLE)
        setVisibility(holder.viewHolderInstalled.view, View.INVISIBLE)
        setVisibility(holder.viewHolderInstalling.view, View.INVISIBLE)
        setVisibility(holder.viewHolderNotInstalled.view, View.INVISIBLE)

        holder.viewHolderInstallFailed.packageName.text = repositoryPackage.name

        holder.viewHolderInstallFailed.retry.isEnabled = true
        holder.viewHolderInstallFailed.retry.setOnClickListener {
          holder.viewHolderInstallFailed.retry.isEnabled = false
          repositoryPackage.install(this.context)
        }

        holder.viewHolderInstallFailed.details.isEnabled = true
        holder.viewHolderInstallFailed.details.setOnClickListener {
          holder.viewHolderInstallFailed.details.isEnabled = false
          this.onShowFailureDetails.invoke(repositoryPackage, state.result)
        }
      }
    }
  }

  inner class ViewHolder(
    parent: View,
    installFailed: View,
    installed: View,
    installing: View,
    notInstalled: View) : RecyclerView.ViewHolder(parent) {

    val viewHolderInstallFailed =
      ViewHolderInstallFailed(installFailed)
    val viewHolderInstalled =
      ViewHolderInstalled(installed)
    val viewHolderInstalling =
      ViewHolderInstalling(installing)
    val viewHolderNotInstalled =
      ViewHolderNotInstalled(notInstalled)
  }
}