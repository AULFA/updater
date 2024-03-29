package au.org.libraryforall.updater.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import org.slf4j.LoggerFactory
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.ArrayList
import java.util.zip.GZIPOutputStream

/**
 * Functions to send reports.
 */

object ErrorReports {

  private val logger = LoggerFactory.getLogger(ErrorReports::class.java)

  /**
   * The result of trying to send.
   */

  sealed class Result {

    /**
     * An attempt was made to send.
     */

    object Sent : Result()

    /**
     * There were no files to send.
     */

    object NoFiles : Result()

    /**
     * An exception was raised.
     */

    data class RaisedException(
      val exception: Exception
    ) : Result()
  }

  /**
   * Try to send a report using the default settings.
   */

  @JvmStatic
  fun sendReportsDefault(
    context: Context,
    address: String,
    subject: String,
    body: String
  ): Result {

    val directories = mutableListOf<File>()
    context.externalCacheDir?.let(directories::add)
    context.getExternalFilesDir("Reports")?.let(directories::add)

    return sendReport(
      context = context,
      baseDirectories = directories.toList(),
      address = address,
      subject = subject,
      body = body,
      includeFile = this::isLogFileOrMigrationReport)
  }

  @JvmStatic
  private fun isLogFileOrMigrationReport(name: String): Boolean {
    if (name.startsWith("report-") && name.endsWith(".xml")) {
      return true
    }
    return name.startsWith("log.txt") && (!name.endsWith(".gz"))
  }

  /**
   * Try to send a report.
   */

  @JvmStatic
  fun sendReport(
    context: Context,
    baseDirectories: List<File>,
    address: String,
    subject: String,
    body: String,
    includeFile: (String) -> Boolean
  ): Result {

    this.logger.debug("preparing report")

    try {
      val files =
        this.collectFiles(baseDirectories, includeFile)
      val compressedFiles =
        files.mapNotNull(this::compressFile)
      val contentUris =
        compressedFiles.map { file -> this.mapFileToContentURI(context, file) }

      this.logger.debug("compressed {} files", compressedFiles.size)

      return if (compressedFiles.isNotEmpty()) {
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
          this.type = "text/plain"
          this.putExtra(Intent.EXTRA_EMAIL, arrayOf(address))
          this.putExtra(Intent.EXTRA_SUBJECT, subject)
          this.putExtra(Intent.EXTRA_TEXT, body)
          val attachments = ArrayList<Uri>(contentUris)
          this.putExtra(Intent.EXTRA_STREAM, attachments)
          this.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
        Result.Sent
      } else {
        Result.NoFiles
      }
    } catch (e: Exception) {
      this.logger.error("failed to send report: ", e)
      return Result.RaisedException(e)
    }
  }

  private fun mapFileToContentURI(context: Context, file: File) =
    FileProvider.getUriForFile(context, "au.org.libraryforall.updater.app", file)

  @JvmStatic
  private fun collectFiles(
    baseDirectories: List<File>,
    includeFile: (String) -> Boolean
  ): MutableList<File> {
    val files = mutableListOf<File>()
    for (baseDirectory in baseDirectories) {
      val list = baseDirectory.absoluteFile.list() ?: arrayOf<String>()
      for (file in list) {
        val filePath = File(baseDirectory, file)
        if (includeFile.invoke(file) && filePath.isFile) {
          this.logger.debug("including {}", file)
          files.add(filePath)
        } else {
          this.logger.debug("excluding {}", file)
        }
      }
    }

    this.logger.debug("collected {} files", files.size)
    return files
  }

  @JvmStatic
  private fun compressFile(file: File): File? {
    return try {
      val parent = file.parentFile
      val fileGz = File(parent, file.name + ".gz")

      FileInputStream(file).use { inputStream ->
        FileOutputStream(fileGz, false).use { stream ->
          BufferedOutputStream(stream).use { bStream ->
            GZIPOutputStream(bStream).use { zStream ->
              inputStream.copyTo(zStream)
              zStream.finish()
              zStream.flush()
              fileGz
            }
          }
        }
      }
    } catch (e: Exception) {
      logger.error("could not compress: {}: ", file, e)
      null
    }
  }

}