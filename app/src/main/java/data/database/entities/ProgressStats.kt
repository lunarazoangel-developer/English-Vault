package data.database.entities

/**
 * Aggregated counters surfaced to the Progress screen.
 *
 * Returned by `WordDao.observeProgressStats` as a single Flow emission
 * so screens can render everything they need without subscribing to
 * five separate Flows.
 *
 * All counters are non-negative. `EMPTY` is exposed for convenience when
 * the database has not been seeded yet.
 *
 * @property totalWords Every word in the dictionary.
 * @property learnedWords Words marked as learned by the user.
 * @property favoriteWords Words the user marked as favorites.
 * @property dueForReview Words whose `nextReview` timestamp is in the past.
 * @property inProgress Words that have been reviewed at least once but
 *   are not yet marked as learned.
 */
data class ProgressStats(
    val totalWords: Int,
    val learnedWords: Int,
    val favoriteWords: Int,
    val dueForReview: Int,
    val inProgress: Int
) {
    companion object {
        /** Safe default used by UI layers before the first emission. */
        val EMPTY: ProgressStats = ProgressStats(0, 0, 0, 0, 0)
    }
}