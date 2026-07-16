package data.game

import data.database.entities.LearningStatus

/**
 * Pure helper that maps a word's consecutive-correct counter to a
 * [LearningStatus], implementing the auto-marking feature.
 *
 * ## Why it lives here
 *
 * Every mini-game ViewModel (Word Match Verbs, Letter Soup,
 * Listening) calls into this object after persisting the user's
 * answer. Centralising the rule guarantees that:
 *
 *  - All three mini-games use the same thresholds.
 *  - The "auto system only promotes, never degrades" rule is enforced
 *    in one place, so a manual `LEARNED` mark can never be silently
 *    overwritten by a wrong answer in a mini-game.
 *  - The thresholds are tunable in a single spot (the two
 *    `THRESHOLD_*` constants below) without touching any DAO or VM
 *    call site.
 *
 * ## Rule
 *
 * Given the previous [current] status and the new
 * [consecutiveCorrect] counter:
 *
 *  - `consecutiveCorrect >= [THRESHOLD_LEARNED]` ⇒ candidate
 *    [LearningStatus.LEARNED].
 *  - `consecutiveCorrect >= [THRESHOLD_ALMOST]` ⇒ candidate
 *    [LearningStatus.ALMOST].
 *  - otherwise ⇒ candidate is the same as [current] (no change).
 *
 * The function then returns
 * [LearningStatus.Companion.max]`(current, candidate)`, which is a
 * no-op when [current] is already higher and a pure "promote" step
 * otherwise. A manual mark of [LearningStatus.LEARNED] therefore
 * survives a wrong answer (which would otherwise reset the counter
 * to 0 and produce a `NOT_LEARNED` candidate — the `max` snaps it
 * back to `LEARNED`).
 */
object AutoStatusEvaluator {

    /**
     * Number of consecutive correct answers that promotes a word
     * from `NOT_LEARNED` to [LearningStatus.ALMOST]. Tunable.
     */
    const val THRESHOLD_ALMOST: Int = 1

    /**
     * Number of consecutive correct answers that promotes a word to
     * [LearningStatus.LEARNED]. Tunable.
     */
    const val THRESHOLD_LEARNED: Int = 3

    /**
     * Returns the [LearningStatus] a word with the given
     * [consecutiveCorrect] counter should hold right now, given that
     * it was previously on [current]. The result is always `current`
     * or strictly higher — the auto pipeline never lowers a status.
     *
     * @param current The word's current status as read from the
     *   database (possibly set manually by the user).
     * @param consecutiveCorrect The updated counter after the latest
     *   mini-game event (caller is responsible for clamping to `>= 0`).
     */
    fun nextStatus(current: LearningStatus, consecutiveCorrect: Int): LearningStatus {
        val candidate = when {
            consecutiveCorrect >= THRESHOLD_LEARNED -> LearningStatus.LEARNED
            consecutiveCorrect >= THRESHOLD_ALMOST -> LearningStatus.ALMOST
            else -> current
        }
        return LearningStatus.max(current, candidate)
    }
}
