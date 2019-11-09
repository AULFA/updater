package one.lfa.updater.opds.database.api

open class OPDSDatabaseException : Exception {

  constructor(
    message: String)
    : super(message)

  constructor(
    message: String,
    cause: Throwable
  ) : super(message, cause)

  constructor(
    cause: Throwable
  ) : super(cause)

}
