package au.org.libraryforall.updater.inventory.vanilla.tasks

import au.org.libraryforall.updater.inventory.api.InventoryStringResourcesType

object InventoryTaskAPKFetchInstall {

  fun create(
    request: InventoryTaskAPKFetchInstallRequest
  ): InventoryTask<Unit> {

    val downloadVerifyTask =
      InventoryTaskFileDownload.create(
        InventoryTaskFileDownloadRequest(
          uri = request.downloadURI,
          retries = request.downloadRetries,
          outputFile = request.apkFile))
        .flatMap {
          InventoryTaskFileVerify.createFailing(
            file = request.apkFile,
            hash = request.hash,
            deleteOnFailure = true)
        }

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
      status = strings::downloadingHTTPRetryingInSeconds)
  }

}