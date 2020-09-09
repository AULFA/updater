package au.org.libraryforall.updater.app

import android.content.res.Resources
import one.lfa.updater.inventory.api.InventoryStringFileResourcesType

class InventoryStringFileResources(
  val resources: Resources
) : InventoryStringFileResourcesType {

  override val fileFinding: String
    get() = this.resources.getString(R.string.fileFinding)

  override val fileOpening: String
    get() = this.resources.getString(R.string.fileOpening)

  override fun fileOpeningFailed(e: Exception): String {
    return this.resources.getString(R.string.fileOpeningFailed, e)
  }

  override val fileDeleting: String
    get() = this.resources.getString(R.string.fileDeleting)

  override val fileDoesNotExist: String
    get() = this.resources.getString(R.string.fileDoesNotExist)

  override val fileCouldNotDelete: String
    get() = this.resources.getString(R.string.fileCouldNotDelete)

}
