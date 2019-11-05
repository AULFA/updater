package au.org.libraryforall.updater.app

import android.content.res.Resources
import au.org.libraryforall.updater.inventory.api.InventoryStringFileResourcesType

class InventoryStringFileResources(
  val resources: Resources
) : InventoryStringFileResourcesType {

  override val fileDeleting: String
    get() = this.resources.getString(R.string.fileDeleting)

  override val fileDoesNotExist: String
    get() = this.resources.getString(R.string.fileDoesNotExist)

  override val fileCouldNotDelete: String
    get() = this.resources.getString(R.string.fileCouldNotDelete)

}
