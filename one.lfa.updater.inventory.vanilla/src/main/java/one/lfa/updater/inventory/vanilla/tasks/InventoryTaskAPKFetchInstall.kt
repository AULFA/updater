package one.lfa.updater.inventory.vanilla.tasks

import one.lfa.updater.inventory.api.InventoryStringResourcesType


/**
 * A task that, when evaluated, downloads an APK file, installs it, and then deletes the APK
 * file on successful installation.
 */

object InventoryTaskAPKFetchInstall {

  fun create(
    request: InventoryTaskAPKFetchInstallRequest
  ): InventoryTask<Unit> {

    val downloadVerifyTask =
      InventoryTaskFileDownload.create(
        InventoryTaskFileDownloadRequest(
          uri = request.downloadURI,
          retries = request.downloadRetries,
          outputFile = request.apkFile,
          expectedHash = request.hash
        )
      )

    val downloadTaskRetrying =
      InventoryTaskRetry.retrying(
        retries = request.downloadRetries,
        pauses = this::pauseProvider,
        one = { downloadVerifyTask })

    val installTask =
      InventoryTaskAPKInstall.create(
        activity = request.activity,
        packageName = request.packageName,
        packageVersionCode = request.packageVersionCode,
        apkFile = request.apkFile)

    val deleteTask =
      InventoryTaskFileDelete.create(request.apkFile)

    return downloadTaskRetrying
      .flatMap { installTask }
      .flatMap { deleteTask }
  }

  private fun pauseProvider(
    execution: InventoryTaskExecutionType,
    retry: InventoryTaskRetryAttempt
  ): InventoryTask<Unit> {
    val strings =
      execution.services.requireService(InventoryStringResourcesType::class.java)
    return InventoryTaskPause.create(
      seconds = 5L,
      description = strings.downloadingHTTPWaitingBeforeRetrying,
      status = { time ->
        strings.downloadingHTTPRetryingInSeconds(
          time,
          retry.attemptCurrent,
          retry.attemptMaximum
        )
      })
  }

}