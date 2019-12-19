package au.org.libraryforall.updater.app

import android.content.res.Resources
import au.org.libraryforall.updater.app.boot.BootStringResourcesType

class MainBootStrings(
  val resources: Resources
) : BootStringResourcesType {

  override val bootStarted: String
    get() = this.resources.getString(R.string.bootInProgress)

  override val bootFailedGeneric: String
    get() = ""

  override val bootCompleted: String
    get() = ""

}
