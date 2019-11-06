package one.lfa.updater.repository.api

import java.net.URI

sealed class RepositoryItem {

  abstract val id: String

  abstract val versionCode: Long

  abstract val versionName: String

  abstract val name: String

  abstract val source: URI

  abstract val sha256: Hash

  data class RepositoryAndroidPackage(
    override val id: String,
    override val versionCode: Long,
    override val versionName: String,
    override val name: String,
    override val source: URI,
    override val sha256: Hash)
    : RepositoryItem()

  data class RepositoryOPDSPackage(
    override val id: String,
    override val versionCode: Long,
    override val versionName: String,
    override val name: String,
    override val source: URI,
    override val sha256: Hash)
    : RepositoryItem()
}