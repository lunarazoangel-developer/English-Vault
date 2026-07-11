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
    }
}