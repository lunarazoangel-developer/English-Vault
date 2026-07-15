package data.game

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Event emitted whenever a grammatical category is promoted by the
 * [PromotionGate] (XP and learned percentage both satisfied).
 *
 * Carries enough information for any UI surface to render a
 * celebration: the [categoryKey] (stable, matches
 * `WordTypeFilter.name`), the [previousLevel] the player just left
 * and the [newLevel] they have reached. The [timestamp] is captured
 * at emission time so consumers can drop stale events when they
 * finally subscribe.
 *
 * @property categoryKey Stable identifier of the promoted category.
 * @property previousLevel Level before the promotion (>= 1).
 * @property newLevel Level after the promotion (always
 *   `previousLevel + 1`).
 * @property timestamp Epoch millis when [PromotionNotifier.emit] was
 *   called. Lets consumers ignore events that arrived before the
 *   current screen was composed.
 */
data class PromotionEvent(
    val categoryKey: String,
    val previousLevel: Int,
    val newLevel: Int,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Process-wide bus that broadcasts [PromotionEvent]s.
 *
 * Designed to live in the Hilt `@Singleton` component so every
 * ViewModel (Words list, mini-games, Progress screen) sees the same
 * upstream. Each emission comes from a call site that just observed
 * a [PromotionOutcome.Promoted] return value from
 * [PromotionGate.evaluate].
 *
 * The bus is a [MutableSharedFlow] with `replay = 0` so a late
 * subscriber does not receive a stale event from the past — the
 * Progress screen only sees events emitted while it is composed (or
 * that are still in the buffered window after the user navigates
 * away and comes back).
 *
 * @see PromotionGate.evaluate
 */
@Singleton
class PromotionNotifier @Inject constructor() {

    private val _events = MutableSharedFlow<PromotionEvent>(
        replay = 0,
        extraBufferCapacity = 8
    )

    /**
     * Read-only stream of every promotion that has happened while
     * the process is alive. Late subscribers do not replay past
     * events; only future emissions are visible.
     */
    val events: SharedFlow<PromotionEvent> = _events.asSharedFlow()

    /**
     * Publishes [event] to every active subscriber.
     *
     * Suspends only when [extraBufferCapacity] subscribers are slow
     * to consume; in practice the buffer is generous enough (8) that
     * the producer never blocks.
     */
    suspend fun emit(event: PromotionEvent) {
        _events.emit(event)
    }
}