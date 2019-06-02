package au.org.libraryforall.updater.inventory.api

import au.org.libraryforall.updater.inventory.api.InventoryAPKDirectoryType.VerificationProgressType

object InventoryAPKDirectoryReceivers {

  fun throttledReceiver(
    approximateCalls: Int = 100,
    progress: (VerificationProgressType) -> Unit): (VerificationProgressType) -> Unit {
    var fCurrentPrev = 0.0
    return { verificationProgress ->
      val fMaximum = verificationProgress.maximumBytes.toDouble()
      val fDivider = fMaximum / approximateCalls.toDouble()
      val fCurrentNext = verificationProgress.currentBytes.toDouble()
      val call = fCurrentNext - fCurrentPrev >= fDivider
      if (call) {
        fCurrentPrev = fCurrentNext
        progress.invoke(verificationProgress)
      } else {
        true
      }
    }
  }

}