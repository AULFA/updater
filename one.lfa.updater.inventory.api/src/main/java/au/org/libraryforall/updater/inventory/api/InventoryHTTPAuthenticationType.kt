package au.org.libraryforall.updater.inventory.api

import one.irradia.http.api.HTTPAuthentication
import java.net.URI

interface InventoryHTTPAuthenticationType {

  fun authenticationFor(uri: URI): HTTPAuthentication?

}