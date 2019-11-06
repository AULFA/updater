package one.lfa.updater.inventory.api

import one.lfa.updater.repository.api.Hash

interface InventoryStringVerificationResourcesType {

  fun verificationFailed(
    expectedHash: Hash,
    receivedHash: String
  ): String

  val verifyCheckSuccess: String

  val verificationSucceeded: String

  val verificationCancelled: String

  val verifyingLocalFile: String

}