package au.org.libraryforall.updater.app

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import one.lfa.updater.inventory.api.InventoryRepositoryType
import org.slf4j.LoggerFactory

object MainDeveloperSettings {

  private val logger =
    LoggerFactory.getLogger(MainDeveloperSettings::class.java)

  @Volatile
  private var showTestingRepositoriesField = false
  private val showTestingRepositoriesSubject =
    BehaviorSubject.create<Boolean>()
      .toSerialized()

  init {
    this.showTestingRepositoriesSubject.onNext(this.showTestingRepositoriesField)
  }

  val showTestingRepositories: Observable<Boolean> =
    this.showTestingRepositoriesSubject

  fun areTestingRepositoriesShown(): Boolean =
    this.showTestingRepositoriesField

  fun setShowTestingRepositories(show: Boolean) {
    this.logger.debug("{} testing repositories", if (show) "enabled" else "disabled")
    this.showTestingRepositoriesField = show
    this.showTestingRepositoriesSubject.onNext(this.showTestingRepositoriesField)
  }

  fun shouldShowRepository(
    repository: InventoryRepositoryType
  ): Boolean {
    val showTesting = areTestingRepositoriesShown()
    if (showTesting) {
      return true
    }
    return !repository.isTesting
  }
}