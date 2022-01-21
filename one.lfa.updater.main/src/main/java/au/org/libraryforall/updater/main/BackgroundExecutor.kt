package au.org.libraryforall.updater.main

import com.google.common.util.concurrent.ListeningExecutorService

data class BackgroundExecutor(
  val executor: ListeningExecutorService)

