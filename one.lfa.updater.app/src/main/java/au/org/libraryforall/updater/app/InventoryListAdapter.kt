package au.org.libraryforall.updater.app

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import one.lfa.updater.inventory.api.InventoryItemResult
import one.lfa.updater.inventory.api.InventoryItemState
import one.lfa.updater.inventory.api.InventoryProgressValue
import one.lfa.updater.inventory.api.InventoryRepositoryItemType
import one.lfa.updater.repository.api.RepositoryItem

class InventoryListAdapter(
  private val context: Activity,
  private val packages: List<InventoryRepositoryItemType>,
  private val onShowFailureDetails: (InventoryRepositoryItemType, InventoryItemResult) -> Unit
) : RecyclerView.Adapter<InventoryListAdapter.ViewHolder>() {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

    val inflater =
      LayoutInflater.from(parent.context)
    val item =
      inflater.inflate(R.layout.inventory_package, parent, false)

    return this.ViewHolder(
      parent = item,
      installFailed = item.findViewById(R.id.installFailed),
      installed = item.findViewById(R.id.installed),
      operating = item.findViewById(R.id.installing),
      notInstalled = item.findViewById(R.id.notInstalled))
  }

  class ViewHolderInstalled(val view: View) {
    val packageName =
      view.findViewById<TextView>(R.id.installedPackageName)
    val packageAvailable =
      view.findViewById<TextView>(R.id.installedPackageAvailable)
    val packageButtonUpdate =
      view.findViewById<Button>(R.id.installedPackageButtonUpdate)
    val packageButtonUninstall =
      view.findViewById<Button>(R.id.installedPackageButtonRemove)
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

  class ViewHolderOperating(val view: View) {
    val buttonCancel =
      view.findViewById<Button>(R.id.operatingButtonCancel)
    val packageName =
      view.findViewById<TextView>(R.id.operatingPackageName)
    val progressBarMajor =
      view.findViewById<ProgressBar>(R.id.operatingProgressMajor)
    val progressBarMinor =
      view.findViewById<ProgressBar>(R.id.operatingProgressMinor)
    val progressState =
      view.findViewById<TextView>(R.id.operatingProgressStatus)
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

  override fun onBindViewHolder(
    holder: ViewHolder,
    position: Int
  ) {
    val repositoryPackage = this.packages[position]
    return when (val state = repositoryPackage.state) {
      is InventoryItemState.NotInstalled -> {
        setVisibility(holder.viewHolderInstallFailed.view, View.INVISIBLE)
        setVisibility(holder.viewHolderInstalled.view, View.INVISIBLE)
        setVisibility(holder.viewHolderOperating.view, View.INVISIBLE)
        setVisibility(holder.viewHolderNotInstalled.view, View.VISIBLE)

        holder.viewHolderNotInstalled.packageName.text = repositoryPackage.item.name
        holder.viewHolderNotInstalled.packageAvailable.text =
          this.context.resources.getString(
            R.string.package_state_available,
            repositoryPackage.item.versionName,
            repositoryPackage.item.versionCode)

        holder.viewHolderNotInstalled.packageButton.isEnabled = true
        holder.viewHolderNotInstalled.packageButton.setOnClickListener {
          holder.viewHolderNotInstalled.packageButton.isEnabled = false
          repositoryPackage.install(this.context)
        }

        holder.viewHolderNotInstalled.packageIcon.setImageResource(
          iconFor(repositoryPackage.state.inventoryItem))
      }

      is InventoryItemState.Installed -> {
        setVisibility(holder.viewHolderInstallFailed.view, View.INVISIBLE)
        setVisibility(holder.viewHolderInstalled.view, View.VISIBLE)
        setVisibility(holder.viewHolderOperating.view, View.INVISIBLE)
        setVisibility(holder.viewHolderNotInstalled.view, View.INVISIBLE)

        holder.viewHolderInstalled.packageName.text = repositoryPackage.item.name
        holder.viewHolderInstalled.packageAvailable.text =
          this.context.resources.getString(
            R.string.package_state_available,
            repositoryPackage.item.versionName,
            repositoryPackage.item.versionCode)

        holder.viewHolderInstalled.packageInstalled.text =
          this.context.resources.getString(
            R.string.package_state_installed,
            state.installedVersionName,
            state.installedVersionCode)

        if (repositoryPackage.isUpdateAvailable) {
          setVisibility(holder.viewHolderInstalled.packageButtonUpdate, View.VISIBLE)
          holder.viewHolderInstalled.packageButtonUpdate.isEnabled = true
          holder.viewHolderInstalled.packageButtonUpdate.setOnClickListener {
            holder.viewHolderInstalled.packageButtonUpdate.isEnabled = false
            repositoryPackage.install(this.context)
          }
        } else {
          setVisibility(holder.viewHolderInstalled.packageButtonUpdate, View.INVISIBLE)
        }

        holder.viewHolderInstalled.packageButtonUninstall.isEnabled = true
        holder.viewHolderInstalled.packageButtonUninstall.setOnClickListener {
          holder.viewHolderInstalled.packageButtonUninstall.isEnabled = false
          this.onWantConfirmUninstall(
            onConfirm = {
              repositoryPackage.uninstall(this.context)
            },
            onDismiss = {
              holder.viewHolderInstalled.packageButtonUninstall.isEnabled = true
            }
          )
        }

        holder.viewHolderInstalled.packageIcon.setImageResource(
          iconFor(repositoryPackage.state.inventoryItem))
      }

      is InventoryItemState.Operating -> {
        setVisibility(holder.viewHolderInstallFailed.view, View.INVISIBLE)
        setVisibility(holder.viewHolderInstalled.view, View.INVISIBLE)
        setVisibility(holder.viewHolderOperating.view, View.VISIBLE)
        setVisibility(holder.viewHolderNotInstalled.view, View.INVISIBLE)

        holder.viewHolderOperating.buttonCancel.isEnabled = true
        holder.viewHolderOperating.buttonCancel.setOnClickListener {
          holder.viewHolderOperating.buttonCancel.isEnabled = false
          repositoryPackage.cancel()
        }

        holder.viewHolderOperating.progressState.text = state.status
        holder.viewHolderOperating.packageName.text = repositoryPackage.item.name

        when (val majorState = state.major) {
          null,
          is InventoryProgressValue.InventoryProgressValueIndefinite -> {
            holder.viewHolderOperating.progressBarMajor.isIndeterminate = true
          }
          is InventoryProgressValue.InventoryProgressValueDefinite -> {
            holder.viewHolderOperating.progressBarMajor.isIndeterminate = false
            holder.viewHolderOperating.progressBarMajor.progress = majorState.percent.toInt()
          }
        }

        when (val minorState = state.minor) {
          is InventoryProgressValue.InventoryProgressValueIndefinite -> {
            holder.viewHolderOperating.progressBarMinor.isIndeterminate = true
          }
          is InventoryProgressValue.InventoryProgressValueDefinite -> {
            holder.viewHolderOperating.progressBarMinor.isIndeterminate = false
            holder.viewHolderOperating.progressBarMinor.progress = minorState.percent.toInt()
          }
        }
      }

      is InventoryItemState.Failed -> {
        setVisibility(holder.viewHolderInstallFailed.view, View.VISIBLE)
        setVisibility(holder.viewHolderInstalled.view, View.INVISIBLE)
        setVisibility(holder.viewHolderOperating.view, View.INVISIBLE)
        setVisibility(holder.viewHolderNotInstalled.view, View.INVISIBLE)

        holder.viewHolderInstallFailed.packageName.text = repositoryPackage.item.name

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

  private fun onWantConfirmUninstall(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit)
  {
    AlertDialog.Builder(this.context)
      .setTitle(R.string.uninstallConfirmTitle)
      .setMessage(R.string.uninstallConfirm)
      .setPositiveButton(R.string.package_uninstall) { _, _ ->
        onConfirm.invoke()
      }
      .setOnDismissListener {
        onDismiss.invoke()
      }
      .show()
  }

  private fun iconFor(inventoryItem: InventoryRepositoryItemType): Int {
    return when (inventoryItem.item) {
      is RepositoryItem.RepositoryAndroidPackage -> R.drawable.apk
      is RepositoryItem.RepositoryOPDSPackage -> R.drawable.opds
    }
  }

  inner class ViewHolder(
    parent: View,
    installFailed: View,
    installed: View,
    operating: View,
    notInstalled: View) : RecyclerView.ViewHolder(parent) {

    val viewHolderInstallFailed =
      ViewHolderInstallFailed(installFailed)
    val viewHolderInstalled =
      ViewHolderInstalled(installed)
    val viewHolderOperating =
      ViewHolderOperating(operating)
    val viewHolderNotInstalled =
      ViewHolderNotInstalled(notInstalled)
  }
}