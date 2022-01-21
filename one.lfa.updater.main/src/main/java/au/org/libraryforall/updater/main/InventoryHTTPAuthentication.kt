package au.org.libraryforall.updater.main

import one.irradia.http.api.HTTPAuthentication
import one.lfa.updater.credentials.api.Credential
import one.lfa.updater.inventory.api.InventoryHTTPAuthenticationType
import org.slf4j.LoggerFactory
import java.net.URI

class InventoryHTTPAuthentication(
  bundledCredentials: List<Credential>
) : InventoryHTTPAuthenticationType {

  private val logger =
    LoggerFactory.getLogger(InventoryHTTPAuthentication::class.java)

  private val recordsLock = Object()
  private val records = sortedMapOf<String, Credential>()

  init {
    synchronized(this.recordsLock) {
      for (credential in bundledCredentials) {
        this.records[credential.uriPrefix] = credential
      }
    }
  }

  override fun authenticationFor(uri: URI): HTTPAuthentication? {
    val result = this.findAuthenticationFor(uri)
    if (result != null) {
      this.logger.trace("found authentication for {} (user {})", uri, result.userName)
    } else {
      this.logger.trace("no matching authentication for {}", uri)
    }
    return result
  }

  private fun findAuthenticationFor(uri: URI): HTTPAuthentication.HTTPAuthenticationBasic? {
    val matchingRecords =
      synchronized(this.recordsLock) {
        this.records.values.filter { record ->
          uri.toString().startsWith(record.uriPrefix)
        }
      }

    return matchingRecords.sortedBy { record -> record.uriPrefix.length }
      .reversed()
      .getOrNull(0)
      ?.authentication()
  }
}
