package one.lfa.updater.credentials.api

import one.irradia.http.api.HTTPAuthentication

/**
 * A credential.
 */

data class Credential(

  /**
   * The prefix of the URIs to which this credential applies.
   */

  val uriPrefix: String,

  /**
   * The username.
   */

  val userName: String,

  /**
   * The password.
   */

  val password: String) {

  /**
   * Construct an HTTP authentication value from these credentials.
   */

  fun authentication(): HTTPAuthentication.HTTPAuthenticationBasic {
    return HTTPAuthentication.HTTPAuthenticationBasic(
      userName = this.userName,
      password = this.password)
  }
}
