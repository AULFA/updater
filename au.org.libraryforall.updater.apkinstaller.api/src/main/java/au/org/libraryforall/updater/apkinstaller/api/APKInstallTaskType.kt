package au.org.libraryforall.updater.apkinstaller.api

import com.google.common.util.concurrent.ListenableFuture
import java.io.File

interface APKInstallTaskType {

  val packageName: String

  val packageVersionCode: Int

  val file: File

  val future: ListenableFuture<Int>

}