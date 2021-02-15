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
import com.google.android.material.textfield.TextInputEditText
import one.lfa.updater.inventory.api.InventoryItemResult
import one.lfa.updater.inventory.api.InventoryItemState
import one.lfa.updater.inventory.api.InventoryProgressValue
import one.lfa.updater.inventory.api.InventoryRepositoryItemType
import one.lfa.updater.inventory.vanilla.Hex
import one.lfa.updater.repository.api.Hash
import one.lfa.updater.repository.api.RepositoryItem
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest

class InventoryListAdapter(
  private val context: Activity,
  private val packages: List<InventoryRepositoryItemType>,
  private val onShowFailureDetails: (InventoryRepositoryItemType, InventoryItemResult) -> Unit
) : RecyclerView.Adapter<InventoryListAdapter.ViewHolder>() {

  private val logger =
    LoggerFactory.getLogger(InventoryListAdapter::class.java)

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): ViewHolder {
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
      this.view.findViewById<TextView>(R.id.installedPackageName)
    val packageAvailable =
      this.view.findViewById<TextView>(R.id.installedPackageAvailable)
    val packageButtonUpdate =
      this.view.findViewById<Button>(R.id.installedPackageButtonUpdate)
    val packageButtonUninstall =
      this.view.findViewById<Button>(R.id.installedPackageButtonRemove)
    val packageIcon =
      this.view.findViewById<ImageView>(R.id.installedPackageIcon)
    val packageInstalled =
      this.view.findViewById<TextView>(R.id.installedPackageInstalled)
  }

  class ViewHolderNotInstalled(val view: View) {
    val packageName =
      this.view.findViewById<TextView>(R.id.notInstalledPackageName)
    val packageAvailable =
      this.view.findViewById<TextView>(R.id.notInstalledPackageAvailable)
    val packageButton =
      this.view.findViewById<Button>(R.id.notInstalledPackageButton)
    val packageIcon =
      this.view.findViewById<ImageView>(R.id.notInstalledPackageIcon)
  }

  class ViewHolderOperating(val view: View) {
    val buttonCancel =
      this.view.findViewById<Button>(R.id.operatingButtonCancel)
    val packageName =
      this.view.findViewById<TextView>(R.id.operatingPackageName)
    val progressBarMajor =
      this.view.findViewById<ProgressBar>(R.id.operatingProgressMajor)
    val progressBarMinor =
      this.view.findViewById<ProgressBar>(R.id.operatingProgressMinor)
    val progressState =
      this.view.findViewById<TextView>(R.id.operatingProgressStatus)
  }

  class ViewHolderInstallFailed(val view: View) {
    val retry =
      this.view.findViewById<Button>(R.id.installFailedPackageButton)
    val details =
      this.view.findViewById<Button>(R.id.installFailedPackageDetailsButton)
    val packageName =
      this.view.findViewById<TextView>(R.id.installFailedPackageName)
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

        holder.viewHolderNotInstalled.packageButton.setText(
          if (repositoryPackage.item.installPasswordSha256 != null) {
            R.string.package_install_locked
          } else {
            R.string.package_install
          })

        holder.viewHolderNotInstalled.packageButton.isEnabled = true
        holder.viewHolderNotInstalled.packageButton.setOnClickListener {
          holder.viewHolderNotInstalled.packageButton.isEnabled = false
          this.doInstall(repositoryPackage, onCancel = {
            holder.viewHolderNotInstalled.packageButton.isEnabled = true
          })
        }

        holder.viewHolderNotInstalled.packageIcon.setImageResource(
          this.iconFor(repositoryPackage.state.inventoryItem))
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
            this.doInstall(repositoryPackage, onCancel = {
              holder.viewHolderInstalled.packageButtonUpdate.isEnabled = true
            })
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
          this.iconFor(repositoryPackage.state.inventoryItem))
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
          this.doInstall(repositoryPackage, onCancel = {
            holder.viewHolderInstallFailed.retry.isEnabled = true
          })
        }

        holder.viewHolderInstallFailed.details.isEnabled = true
        holder.viewHolderInstallFailed.details.setOnClickListener {
          holder.viewHolderInstallFailed.details.isEnabled = false
          this.onShowFailureDetails.invoke(repositoryPackage, state.result)
        }
      }
    }
  }

  private fun doInstall(
    repositoryPackage: InventoryRepositoryItemType,
    onCancel: () -> Unit
  ) {
    val requiredPasswordHash = repositoryPackage.item.installPasswordSha256
    if (requiredPasswordHash != null) {
      this.doInstallPasswordGated(
        repositoryPackage = repositoryPackage,
        requiredPasswordHash = requiredPasswordHash,
        onCancel = onCancel
      )
    } else {
      repositoryPackage.install(this.context)
    }
  }

  private fun doInstallPasswordGated(
    repositoryPackage: InventoryRepositoryItemType,
    requiredPasswordHash: Hash,
    onCancel: () -> Unit
  ) {
    val layoutInflater =
      this.context.layoutInflater
    val view =
      layoutInflater.inflate(R.layout.password, null)
    val text =
      view.findViewById<TextInputEditText>(R.id.updaterPasswordText)

    AlertDialog.Builder(this.context)
      .setView(view)
      .setPositiveButton(R.string.install_password_enter) { _, _ ->
        val providedText = text.text?.toString() ?: ""
        if (this.checkPasswordHash(providedText, requiredPasswordHash)) {
          repositoryPackage.install(this.context)
        } else {
          AlertDialog.Builder(this.context)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(R.string.install_password_incorrect_title)
            .setMessage(R.string.install_password_incorrect)
            .create()
            .show()
          repositoryPackage.cancel()
          onCancel.invoke()
        }
      }
      .setOnDismissListener {
        onCancel.invoke()
      }
      .create()
      .show()

    text.requestFocus()
  }

  private fun checkPasswordHash(
    providedText: String,
    requiredPasswordHash: Hash
  ): Boolean {
    val digest =
      MessageDigest.getInstance("SHA-256")
    val providedBytes =
      Hex.bytesToHex(digest.digest(providedText.trim().toByteArray(UTF_8)))
    val matches =
      requiredPasswordHash.text.equals(providedBytes, ignoreCase = true)

    this.logger.debug("required hash: {}", requiredPasswordHash.text)
    this.logger.debug("provided hash: {}", providedBytes)
    this.logger.debug("matches:       {}", matches)
    return matches
  }

  private fun onWantConfirmUninstall(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit) {
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