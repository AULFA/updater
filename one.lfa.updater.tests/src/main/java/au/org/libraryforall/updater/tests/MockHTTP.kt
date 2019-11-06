package au.org.libraryforall.updater.tests

import one.irradia.http.api.HTTPAuthentication
import one.irradia.http.api.HTTPClientType
import one.irradia.http.api.HTTPResult
import one.irradia.mime.api.MIMEType
import java.io.InputStream
import java.net.URI

class MockHTTP : HTTPClientType {

  val responsesByURI =
    mutableMapOf<URI, MutableList<HTTPResult<InputStream>>>()

  override fun close() {

  }

  fun addResponse(
    uri: URI,
    response: HTTPResult<InputStream>
  ) {
    val responses = this.responsesByURI[uri] ?: mutableListOf()
    responses.add(response)
    this.responsesByURI[uri] = responses
  }

  override fun request(
    uri: URI,
    method: String,
    authentication: (URI) -> HTTPAuthentication?,
    offset: Long,
    contentType: MIMEType?,
    body: ByteArray?
  ): HTTPResult<InputStream> {
    val responses =
      this.responsesByURI[uri] ?: throw IllegalStateException("No responses for ${uri}")
    return responses.removeAt(0)
  }
}