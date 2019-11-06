package au.org.libraryforall.updater.app

import android.content.res.Resources
import one.lfa.updater.inventory.api.InventoryStringVerificationResourcesType
import one.lfa.updater.repository.api.Hash

class InventoryStringVerificationResources(
  private val resources: Resources
) : InventoryStringVerificationResourcesType {

  override fun verificationFailed(
    expectedHash: Hash,
    receivedHash: String
  ): String =
    this.resources.getString(
      R.string.verificationFailed,
      expectedHash.text,
      receivedHash)

  override val verifyCheckSuccess: String
    get() = this.resources.getString(R.string.verifyCheckSuccess)

  override val verificationSucceeded: String
    get() = this.resources.getString(R.string.verificationSucceeded)

  override val verificationCancelled: String
    get() = this.resources.getString(R.string.verificationCancelled)

  override val verifyingLocalFile: String
    get() = this.resources.getString(R.string.verifyingLocalFile)

}
