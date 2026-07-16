package data.database.entities

/**
 * Tri-state learning progress surfaced by [WordEntity.status] and
 * persisted on every `words` row (both `core_words` and `user_words`).
 *
 *  - [NOT_LEARNED] — fresh entry, never reviewed.
 *  - [ALMOST]      — the user has interacted with the word but has
 *    not yet marked it as fully mastered.
 *  - [LEARNED]     — fully mastered.
 *
 * Room persists this enum as TEXT using the constant name. The DAO
 * queries compare against the string literal (`'LEARNED'` etc.) so
 * renaming any of the values is a hard schema break and must be
 * paired with a Room migration.
 */
enum class LearningStatus {
    NOT_LEARNED,
    ALMOST,
    LEARNED;

    companion object {
        /**
         * Parses a stored value into the corresponding [LearningStatus].
         * Defaults to [NOT_LEARNED] when [value] is null, blank or not
         * part of the enum, which keeps legacy data safe to import.
         */
        fun fromStringOrDefault(value: String?): LearningStatus =
            value?.let { runCatching { valueOf(it) }.getOrNull() } ?: NOT_LEARNED

        /**
         * Monotonic rank used by [data.game.AutoStatusEvaluator] to
         * implement the "auto system only promotes, never degrades"
         * rule without a manual ordering lookup at every call site.
         *
         * `LEARNED (2) > ALMOST (1) > NOT_LEARNED (0)` — so picking the
         * higher ordinal between the current and the candidate status
         * keeps any manual mark (including a manual `LEARNED`) intact
         * when the auto system tries to "promote" a word whose
         * consecutive-correct counter has not yet reached the next
         * threshold.
         */
        private val LearningStatus.ordinalValue: Int
            get() = when (this) {
                NOT_LEARNED -> 0
                ALMOST -> 1
                LEARNED -> 2
            }

        /**
         * Returns whichever of [a] or [b] is the higher learning
         * status. Used by the auto-status pipeline so a manual mark
         * can never be silently downgraded by a subsequent
         * mini-game event.
         */
        fun max(a: LearningStatus, b: LearningStatus): LearningStatus =
            if (a.ordinalValue >= b.ordinalValue) a else b
    }
}