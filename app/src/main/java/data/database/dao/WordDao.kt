package data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import data.database.entities.Difficulty
import data.database.entities.ProgressStats
import data.database.entities.WordEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for the `words` table.
 *
 * Reactive queries return [Flow] so screens can observe the database
 * without manual refresh, while one-shot mutations are `suspend` so they
 * integrate cleanly with coroutines and structured cancellation.
 *
 * Provided to the rest of the app via `DatabaseModule.provideWordDao`.
 */
@Dao
interface WordDao {

    // region: Inserts
    /**
     * Bulk-inserts a list of words. Used during the first-launch seed and
     * when importing larger batches from external sources.
     *
     * Conflicts (matching primary key) are resolved by replacing the
     * existing row, which keeps the table idempotent across re-imports.
     *
     * @param words Entries to persist.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<WordEntity>)

    /**
     * Inserts or replaces a single word.
     *
     * @param word Entry to persist.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: WordEntity)
    // endregion

    // region: Queries
    /**
     * Returns every word ordered alphabetically.
     *
     * Backed by a Room invalidation tracker: the [Flow] re-emits whenever
     * the table is mutated by any DAO call.
     */
    @Query("SELECT * FROM words ORDER BY word ASC")
    fun getAllWords(): Flow<List<WordEntity>>

    /**
     * Case-insensitive search across the word, its translation, category
     * tags and word tags.
     *
     * Results are ordered alphabetically and emitted reactively.
     *
     * @param query Free-text filter applied with `LIKE '%query%'`.
     */
    @Query(
        """
        SELECT * FROM words
        WHERE word LIKE '%' || :query || '%'
           OR translation LIKE '%' || :query || '%'
           OR category LIKE '%' || :query || '%'
           OR tags LIKE '%' || :query || '%'
        ORDER BY word ASC
        """
    )
    fun searchWords(query: String): Flow<List<WordEntity>>
    // endregion

    // region: Updates (user-owned fields)
    /**
     * Marks a word as learned or not.
     *
     * @return Number of rows updated (0 if the id does not exist).
     */
    @Query("UPDATE words SET learned = :learned WHERE id = :id")
    suspend fun setLearned(id: Int, learned: Boolean): Int

    /** Toggles the favorite flag on a word. */
    @Query("UPDATE words SET favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Int, favorite: Boolean): Int

    /**
     * Records that the user reviewed a word right now: bumps
     * [WordEntity.reviewCount] and sets [WordEntity.lastReview].
     */
    @Query("UPDATE words SET reviewCount = reviewCount + 1, lastReview = :timestamp WHERE id = :id")
    suspend fun recordReview(id: Int, timestamp: Long): Int

    /**
     * Schedules the next review for a word. Pass `null` to clear the
     * schedule (e.g. when a word is marked as learned).
     */
    @Query("UPDATE words SET nextReview = :nextReview WHERE id = :id")
    suspend fun setNextReview(id: Int, nextReview: Long?): Int

    /** Updates the personal note attached to a word. */
    @Query("UPDATE words SET notes = :notes WHERE id = :id")
    suspend fun setNotes(id: Int, notes: String): Int

    /** Overrides the difficulty for a single word. Pass `null` to reset. */
    @Query("UPDATE words SET customDifficulty = :difficulty WHERE id = :id")
    suspend fun setCustomDifficulty(id: Int, difficulty: Difficulty?): Int
    // endregion

    // region: Filtered reactive queries
    /** Reactive list of words the user has marked as learned. */
    @Query("SELECT * FROM words WHERE learned = 1 ORDER BY word ASC")
    fun getLearnedWords(): Flow<List<WordEntity>>

    /** Reactive list of words the user has favorited. */
    @Query("SELECT * FROM words WHERE favorite = 1 ORDER BY word ASC")
    fun getFavoriteWords(): Flow<List<WordEntity>>

    /**
     * Reactive list of words whose `nextReview` timestamp is at or
     * before [now] and which have not been learned yet.
     */
    @Query(
        """
        SELECT * FROM words
        WHERE learned = 0
          AND nextReview IS NOT NULL
          AND nextReview <= :now
        ORDER BY nextReview ASC
        """
    )
    fun getDueForReview(now: Long): Flow<List<WordEntity>>

    /** Words that have been reviewed at least once but are not yet learned. */
    @Query("SELECT * FROM words WHERE learned = 0 AND reviewCount > 0 ORDER BY word ASC")
    fun getInProgressWords(): Flow<List<WordEntity>>
    // endregion

    // region: Aggregate counts (Flow)
    /** Reactive count of every word in the dictionary. */
    @Query("SELECT COUNT(*) FROM words")
    fun countAllFlow(): Flow<Int>

    /** Reactive count of words marked as learned. */
    @Query("SELECT COUNT(*) FROM words WHERE learned = 1")
    fun countLearnedFlow(): Flow<Int>

    /** Reactive count of words marked as favorite. */
    @Query("SELECT COUNT(*) FROM words WHERE favorite = 1")
    fun countFavoritesFlow(): Flow<Int>

    /** Reactive count of words due for review right now. */
    @Query(
        """
        SELECT COUNT(*) FROM words
        WHERE learned = 0
          AND nextReview IS NOT NULL
          AND nextReview <= :now
        """
    )
    fun countDueForReviewFlow(now: Long): Flow<Int>

    /** Reactive count of words in progress (reviewed but not learned). */
    @Query("SELECT COUNT(*) FROM words WHERE learned = 0 AND reviewCount > 0")
    fun countInProgressFlow(): Flow<Int>
    // endregion

    // region: Aggregate counts (one-shot)
    /**
     * Returns the number of rows currently stored in the `words` table.
     *
     * Used by `MainActivity` to decide whether the JSON seed should run
     * (only when the database is empty).
     */
    @Query("SELECT COUNT(*) FROM words")
    suspend fun countWords(): Int

    /** One-shot count of learned words. */
    @Query("SELECT COUNT(*) FROM words WHERE learned = 1")
    suspend fun countLearned(): Int

    /** One-shot count of favorite words. */
    @Query("SELECT COUNT(*) FROM words WHERE favorite = 1")
    suspend fun countFavorites(): Int

    /** One-shot count of words due for review at [now]. */
    @Query(
        """
        SELECT COUNT(*) FROM words
        WHERE learned = 0
          AND nextReview IS NOT NULL
          AND nextReview <= :now
        """
    )
    suspend fun countDueForReview(now: Long): Int
    // endregion

    // region: Aggregated progress stats
    /**
     * Single Flow that emits every counter the Progress screen needs in
     * one go. Re-emits whenever any of the underlying tables change.
     *
     * The column aliases (`totalWords`, `learnedWords`, …) must match
     * the [ProgressStats] property names; Room uses them to populate the
     * data class via its constructor.
     */
    @Query(
        """
        SELECT
            (SELECT COUNT(*) FROM words) AS totalWords,
            (SELECT COUNT(*) FROM words WHERE learned = 1) AS learnedWords,
            (SELECT COUNT(*) FROM words WHERE favorite = 1) AS favoriteWords,
            (SELECT COUNT(*) FROM words
                WHERE learned = 0
                  AND nextReview IS NOT NULL
                  AND nextReview <= :now) AS dueForReview,
            (SELECT COUNT(*) FROM words
                WHERE learned = 0
                  AND reviewCount > 0) AS inProgress
        """
    )
    fun observeProgressStats(now: Long): Flow<ProgressStats>
    // endregion

    // region: Maintenance
    /**
     * Removes a single word by id.
     *
     * The caller is responsible for validating that the row is
     * user-owned (see [data.database.entities.isUserAdded]); the DAO
     * intentionally does not gate deletes so that future tooling
     * (e.g. an admin reset) can still wipe seeded rows if needed.
     *
     * @return Number of rows deleted (0 if the id does not exist).
     */
    @Query("DELETE FROM words WHERE id = :id")
    suspend fun deleteWord(id: Int): Int

    /** Removes every word from the table. Intended for debug / reset flows. */
    @Query("DELETE FROM words")
    suspend fun deleteAll()
    // endregion
}