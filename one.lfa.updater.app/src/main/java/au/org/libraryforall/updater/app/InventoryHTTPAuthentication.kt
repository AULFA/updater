package au.org.libraryforall.updater.app

import one.irradia.http.api.HTTPAuthentication
import one.lfa.updater.inventory.api.InventoryHTTPAuthenticationType
import java.net.URI

object InventoryHTTPAuthentication : InventoryHTTPAuthenticationType {

  override fun authenticationFor(uri: URI): HTTPAuthentication? {
    return null
  }

}
