package au.org.libraryforall.updater.app

import au.org.libraryforall.updater.inventory.api.InventoryHTTPAuthenticationType
import one.irradia.http.api.HTTPAuthentication
import java.net.URI

object InventoryHTTPAuthentication : InventoryHTTPAuthenticationType {

  override fun authenticationFor(uri: URI): HTTPAuthentication? {
    return null
  }

}
