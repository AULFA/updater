package au.org.libraryforall.updater.main

import android.content.res.Resources
import one.lfa.updater.opds.database.api.OPDSDatabaseStringsType
import java.util.UUID

class InventoryStringOPDSDatabaseResources(
  val resources: Resources
) : OPDSDatabaseStringsType {

  override fun opdsDatabaseErrorIdMismatch(
    expected: UUID,
    received: UUID
  ): String {
    return resources.getString(R.string.opdsDatabaseErrorIdMismatch, expected, received)
  }
}
