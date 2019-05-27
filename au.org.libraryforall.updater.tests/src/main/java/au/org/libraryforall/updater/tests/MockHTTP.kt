package au.org.libraryforall.updater.tests

import one.irradia.http.api.HTTPAuthentication
import one.irradia.http.api.HTTPClientType
import one.irradia.http.api.HTTPResult
import one.irradia.mime.api.MIMEType
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URI

class MockHTTP : HTTPClientType {

  override fun close() {

  }

  override fun request(
    uri: URI,
    method: String,
    authentication: (URI) -> HTTPAuthentication?,
    offset: Long,
    contentType: MIMEType?,
    body: ByteArray?
  ): HTTPResult<InputStream> {
    return HTTPResult.HTTPOK(
      uri = uri,
      contentLength = 0L,
      headers = mapOf(),
      message = "OK",
      statusCode = 200,
      result = ByteArrayInputStream(ByteArray(0))
    )
  }
}