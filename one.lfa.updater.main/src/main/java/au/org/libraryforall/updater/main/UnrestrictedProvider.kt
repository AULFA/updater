package au.org.libraryforall.updater.main

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import one.lfa.updater.contenturis.OPDSContentURIs
import one.lfa.updater.inventory.api.InventoryCatalogDirectoryType
import one.lfa.updater.services.api.Services
import org.slf4j.LoggerFactory
import java.io.File

class UnrestrictedProvider : ContentProvider() {

  private val logger = LoggerFactory.getLogger(UnrestrictedProvider::class.java)

  override fun openFile(
    uri: Uri,
    mode: String
  ): ParcelFileDescriptor? {
    this.logger.debug("openFile: {}", uri)

    val path = uri.path ?: return run {
      this.logger.debug("no path provided in URI, aborting!")
      null
    }

    val contentURI =
      OPDSContentURIs.parseContentURI(path) ?: return null

    val serviceDirectory =
      Services.serviceDirectory()
    val catalogDirectory =
      serviceDirectory.requireService(InventoryCatalogDirectoryType::class.java)
    val resolved =
      File(catalogDirectory.directory.absoluteFile, "${contentURI.catalogId}/${contentURI.path}")

    this.logger.debug("resolved: {}", resolved)
    return try {
      ParcelFileDescriptor.open(resolved, ParcelFileDescriptor.MODE_READ_ONLY)
    } catch (e: Exception) {
      this.logger.error("could not open file: ", e)
      null
    }
  }

  override fun insert(
    uri: Uri,
    values: ContentValues?
  ): Uri? {
    this.logger.debug("insert: {}", uri)
    throw UnsupportedOperationException()
  }

  override fun query(
    uri: Uri,
    projection: Array<String>?,
    selection: String?,
    selectionArgs: Array<String>?,
    sortOrder: String?
  ): Cursor? {
    this.logger.debug("query: {}", uri)
    throw UnsupportedOperationException()
  }

  override fun onCreate(): Boolean {
    return true
  }

  override fun update(
    uri: Uri,
    values: ContentValues?,
    selection: String?,
    selectionArgs: Array<String>?
  ): Int {
    this.logger.debug("update: {}", uri)
    throw UnsupportedOperationException()
  }

  override fun delete(
    uri: Uri,
    selection: String?,
    selectionArgs: Array<String>?
  ): Int {
    this.logger.debug("delete: {}", uri)
    throw UnsupportedOperationException()
  }

  override fun getType(uri: Uri): String? {
    this.logger.debug("getType: {}", uri)
    return "application/octet-stream"
  }
}