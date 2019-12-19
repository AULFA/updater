package au.org.libraryforall.updater.app

import com.google.common.util.concurrent.ListeningExecutorService

data class BackgroundExecutor(
  val executor: ListeningExecutorService)

