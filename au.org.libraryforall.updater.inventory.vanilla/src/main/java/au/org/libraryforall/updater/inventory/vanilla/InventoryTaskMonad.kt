package au.org.libraryforall.updater.inventory.vanilla

import au.org.libraryforall.updater.inventory.api.InventoryTaskStep

sealed class InventoryTaskMonad<A> {

  abstract val steps: List<InventoryTaskStep>

  data class InventoryTaskSuccess<A>(
    val value: A,
    override val steps: List<InventoryTaskStep> = listOf())
    : InventoryTaskMonad<A>()

  data class InventoryTaskFailed<A>(
    override val steps: List<InventoryTaskStep> = listOf())
    : InventoryTaskMonad<A>()

  data class InventoryTaskCancelled<A>(
    override val steps: List<InventoryTaskStep> = listOf())
    : InventoryTaskMonad<A>()

  companion object {

    fun startWithStep(step: InventoryTaskStep): InventoryTaskMonad<Unit> {
      return InventoryTaskSuccess(value = Unit, steps = listOf(step))
    }

    fun <A, B> flatMap(
      m: InventoryTaskMonad<A>,
      f: (A) -> InventoryTaskMonad<B>): InventoryTaskMonad<B> {
      return when (m) {
        is InventoryTaskSuccess -> {
          when (val result = f.invoke(m.value)) {
            is InventoryTaskSuccess ->
              InventoryTaskSuccess(result.value, m.steps.plus(result.steps))
            is InventoryTaskFailed ->
              InventoryTaskFailed(m.steps.plus(result.steps))
            is InventoryTaskCancelled ->
              InventoryTaskCancelled(m.steps.plus(result.steps))
          }
        }
        is InventoryTaskFailed ->
          InventoryTaskFailed(m.steps)
        is InventoryTaskCancelled ->
          InventoryTaskCancelled(m.steps)
      }
    }

    fun <A, B> andThen(
      m: InventoryTaskMonad<A>,
      n: InventoryTaskMonad<B>): InventoryTaskMonad<B> {
      return flatMap(m) { n }
    }

    fun <A, B> map(m: InventoryTaskMonad<A>, f: (A) -> B): InventoryTaskMonad<B> {
      return when (m) {
        is InventoryTaskSuccess ->
          InventoryTaskSuccess(f.invoke(m.value), m.steps)
        is InventoryTaskFailed ->
          InventoryTaskFailed(m.steps)
        is InventoryTaskCancelled ->
          InventoryTaskCancelled(m.steps)
      }
    }
  }

  fun <B> flatMap(f: (A) -> InventoryTaskMonad<B>): InventoryTaskMonad<B> =
    Companion.flatMap(this, f)

  fun <B> andThen(m: InventoryTaskMonad<B>): InventoryTaskMonad<B> =
    Companion.andThen(this, m)

  fun <B> map(f: (A) -> B): InventoryTaskMonad<B> =
    Companion.map(this, f)

}
