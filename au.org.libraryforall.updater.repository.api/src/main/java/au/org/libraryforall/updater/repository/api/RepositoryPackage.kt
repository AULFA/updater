package au.org.libraryforall.updater.repository.api

import java.net.URI

data class RepositoryPackage(
  val id: String,
  val versionCode: Int,
  val versionName: String,
  val name: String,
  val source: URI,
  val sha256: Hash)
