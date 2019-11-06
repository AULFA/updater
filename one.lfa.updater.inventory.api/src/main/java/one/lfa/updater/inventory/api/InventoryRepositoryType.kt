package one.lfa.updater.inventory.api

import com.google.common.util.concurrent.ListenableFuture
import io.reactivex.Observable
import net.jcip.annotations.ThreadSafe
import org.joda.time.LocalDateTime
import java.net.URI
import java.util.UUID

@ThreadSafe
interface InventoryRepositoryType {

  val events: Observable<InventoryEvent>

  val id: UUID

  val title: String

  val updated: LocalDateTime

  val updateURI: URI

  val items: List<InventoryRepositoryItemType>

  val state: InventoryRepositoryState

  fun update(): ListenableFuture<Unit>

}
