package data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import data.database.entities.CoreWordEntity
import data.database.entities.Difficulty
import data.database.entities.LearningStatus
import data.database.entities.ProgressStats
import data.database.entities.UserWordEntity
import data.database.entities.WordEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for the dictionary tables.
 *
 * Read paths target the `words_view` UNION, which is why every query
 * below returns [WordEntity]. Write paths target either `core_words`
 * or `user_words` explicitly so the two id sequences stay isolated.
 *
 * Reactive queries return [Flow] so screens can observe the database
 * without manual refresh, while one-shot mutations are `suspend` so they
 * integrate cleanly with coroutines and structured cancellation.
 *
 * Provided to the rest of the app via `DatabaseModule.provideWordDao`.
 *
 * ## Dual-table update pattern
 *
 * State-mutating queries like `setLearned`, `setFavorite`, … cannot be
 * expressed as a single `UPDATE words` statement anymore because there
 * is no unified `words` table. They are implemented as a default method
 * that fans out into two `@Query` calls — one against each underlying
 * table. Because the two `AUTOINCREMENT` sequences are independent,
 * the same id cannot exist in both tables, so exactly one of the two
 * calls touches a row; the other updates zero rows and is a no-op.
 */
@Dao
interface WordDao {

    // region: Inserts
    /**
     * Bulk-inserts core dictionary words. Used during the first-launch
     * seed and when re-importing a newer version of the bundled
     * `assets/dictionary/`.
     *
     * Conflicts (matching primary key) are resolved by replacing the
     * existing row, which keeps the table idempotent across re-imports.
     *
     * @param words Entries to persist into `core_words`.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoreWords(words: List<CoreWordEntity>)

    /**
     * Inserts or replaces a single user-added word.
     *
     * @param word Entry to persist into `user_words`. The id is
     *   typically `0` so SQLite's AUTOINCREMENT picks the next free
     *   value.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserWord(word: UserWordEntity)
    // endregion

    // region: Queries
    /**
     * Returns every word (core + user) ordered alphabetically.
     *
     * Backed by a Room invalidation tracker: the [Flow] re-emits
     * whenever either underlying table is mutated by any DAO call.
     */
    @Query("SELECT * FROM words_view ORDER BY word ASC")
    fun getAllWords(): Flow<List<WordEntity>>

    /**
     * One-shot lookup of any word (core or user) by id.
     *
     * Used by the Words screen to fetch the row that is about to be
     * mutated so the promotion gate can decide whether the new
     * status triggers a level-up. The id space is unique per table
     * but the same id can appear in both `core_words` and
     * `user_words`, so this query picks whichever row owns it (and
     * returns `null` if no row does).
     *
     * @return The persisted word, or `null` if no row matches.
     */
    @Query("SELECT * FROM words_view WHERE id = :id LIMIT 1")
    suspend fun getWordById(id: Int): WordEntity?

    /**
     * One-shot lookup of a single user-added word by its id.
     *
     * Used by the word form to load an existing row when the user
     * taps the edit pencil on a `Mine` card. Scoped to
     * `source = 'user'` so it never picks up a core entry that happens
     * to share the id (the two AUTOINCREMENT sequences are
     * independent).
     *
     * @return The persisted word, or `null` if no user-added row
     *   matches the id.
     */
    @Query("SELECT * FROM words_view WHERE id = :id AND source = 'user' LIMIT 1")
    suspend fun getUserWordById(id: Int): WordEntity?

    /**
     * One-shot list of core words eligible for the Word Match Verbs
     * mini-game at the given [level].
     *
     * Filters applied:
     *  - `source = 'core'` — only bundled dictionary entries.
     *  - `level = :level` — progression bucket.
     *  - `forms IS NOT NULL` — verbs that actually carry conjugation
     *    tables. Interjections, nouns, etc. are excluded.
     *  - `status != 'LEARNED'` — already mastered words are not
     *    re-practiced.
     *
     * Returned alphabetically so the playthrough order is stable.
     */
    @Query(
        """
        SELECT * FROM words_view
        WHERE source = 'core'
          AND level = :level
          AND forms IS NOT NULL
          AND status != 'LEARNED'
        ORDER BY word ASC
        """
    )
    suspend fun getCoreWordsForGame(level: Int): List<WordEntity>

    /**
     * One-shot list of core words at [level] regardless of grammatical
     * type.
     *
     * Unlike [getCoreWordsForGame], this query does NOT require
     * `forms IS NOT NULL` and does NOT filter by `status` — the
     * Listening mini-game uses it to draw words of any category
     * (nouns, adjectives, verbs, …) and reuses already-learned
     * entries as distractors so the distractor pool stays large.
     *
     * Returned alphabetically so the playthrough order is stable.
     */
    @Query(
        """
        SELECT * FROM words_view
        WHERE source = 'core'
          AND level = :level
        ORDER BY word ASC
        """
    )
    suspend fun getCoreWordsAtLevel(level: Int): List<WordEntity>

    /**
     * One-shot list of core words at [level] whose length (in
     * characters) sits between [min] and [max] inclusive.
     *
     * Used by the Letter Soup mini-game to pick words that fit on the
     * board — both the 8×8 default and the 10×10 fallback for longer
     * words. The query filters on `source = 'core'` so user-added
     * entries do not leak into the curated pool, and ignores the
     * learning-status gate that Word Match Verbs applies — Letter Soup
     * is short and varied, and rotating mastered words keeps the
     * board fresh.
     */
    @Query(
        """
        SELECT * FROM words_view
        WHERE source = 'core'
          AND level = :level
          AND LENGTH(word) BETWEEN :min AND :max
        ORDER BY word ASC
        """
    )
    suspend fun getCoreWordsByLengthAndLevel(
        level: Int,
        min: Int,
        max: Int
    ): List<WordEntity>

    /**
     * Highest `level` value present in the dictionary for words that
     * fit on the Letter Soup board (length within [min]..[max]).
     *
     * Drives the count of cards the level selector renders.
     */
    @Query(
        """
        SELECT IFNULL(MAX(level), 0) FROM words_view
        WHERE source = 'core'
          AND LENGTH(word) BETWEEN :min AND :max
        """
    )
    suspend fun maxCoreLevelByLength(min: Int, max: Int): Int

    /**
     * Maximum level currently used by the dictionary. Used by the
     * level selector to know how many level cards to render.
     */
    @Query("SELECT IFNULL(MAX(level), 0) FROM words_view WHERE source = 'core' AND forms IS NOT NULL")
    suspend fun maxCoreLevel(): Int

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
        SELECT * FROM words_view
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
    // The dual-table pattern below is documented in the class-level KDoc.
    @Query("UPDATE core_words SET status = :status WHERE id = :id")
    suspend fun setStatusCore(id: Int, status: LearningStatus): Int

    @Query("UPDATE user_words SET status = :status WHERE id = :id")
    suspend fun setStatusUser(id: Int, status: LearningStatus): Int

    /**
     * Promotes a word's [LearningStatus] (NOT_LEARNED, ALMOST or
     * LEARNED). The id is unique to whichever table holds the row,
     * so the fan-out updates exactly one row.
     *
     * @return Number of rows updated (0 if the id does not exist, 1
     *   otherwise).
     */
    suspend fun setStatus(id: Int, status: LearningStatus): Int =
        setStatusCore(id, status) + setStatusUser(id, status)

    @Query("UPDATE core_words SET favorite = :favorite WHERE id = :id")
    suspend fun setFavoriteCore(id: Int, favorite: Boolean): Int

    @Query("UPDATE user_words SET favorite = :favorite WHERE id = :id")
    suspend fun setFavoriteUser(id: Int, favorite: Boolean): Int

    /** Toggles the favorite flag on a word. */
    suspend fun setFavorite(id: Int, favorite: Boolean): Int =
        setFavoriteCore(id, favorite) + setFavoriteUser(id, favorite)

    @Query("UPDATE core_words SET reviewCount = reviewCount + 1, lastReview = :timestamp WHERE id = :id")
    suspend fun recordReviewCore(id: Int, timestamp: Long): Int

    @Query("UPDATE user_words SET reviewCount = reviewCount + 1, lastReview = :timestamp WHERE id = :id")
    suspend fun recordReviewUser(id: Int, timestamp: Long): Int

    /**
     * Records that the user reviewed a word right now: bumps
     * [WordEntity.reviewCount] and sets [WordEntity.lastReview].
     */
    suspend fun recordReview(id: Int, timestamp: Long): Int =
        recordReviewCore(id, timestamp) + recordReviewUser(id, timestamp)

    @Query("UPDATE core_words SET nextReview = :nextReview WHERE id = :id")
    suspend fun setNextReviewCore(id: Int, nextReview: Long?): Int

    @Query("UPDATE user_words SET nextReview = :nextReview WHERE id = :id")
    suspend fun setNextReviewUser(id: Int, nextReview: Long?): Int

    /**
     * Schedules the next review for a word. Pass `null` to clear the
     * schedule (e.g. when a word is marked as learned).
     */
    suspend fun setNextReview(id: Int, nextReview: Long?): Int =
        setNextReviewCore(id, nextReview) + setNextReviewUser(id, nextReview)

    @Query("UPDATE core_words SET consecutiveCorrect = :value, lastReview = :timestamp WHERE id = :id")
    suspend fun setConsecutiveCorrectCore(id: Int, value: Int, timestamp: Long): Int

    @Query("UPDATE user_words SET consecutiveCorrect = :value, lastReview = :timestamp WHERE id = :id")
    suspend fun setConsecutiveCorrectUser(id: Int, value: Int, timestamp: Long): Int

    /**
     * Backs the auto-marking feature: every correct mini-game answer
     * bumps [WordEntity.consecutiveCorrect] on the underlying row;
     * every wrong answer (or the end of a Letter Soup run without a
     * fix) resets it to `0`. [data.game.AutoStatusEvaluator] then maps
     * the new counter to a [LearningStatus] (`>=1 → ALMOST`,
     * `>=3 → LEARNED`) without ever downgrading a manual mark.
     *
     * `lastReview` is updated at the same time so the existing
     * "last reviewed" copy on the Words screen stays accurate. The
     * `reviewCount` column is intentionally left untouched — that
     * counter is reserved for the future spaced-repetition scheduler
     * (see README §SRS-based review scheduling).
     *
     * @param id Row id (auto-incremented in `core_words` or
     *   `user_words`).
     * @param value New value of the consecutive-correct counter. The
     *   caller decides whether to bump (`previous + 1`) or reset
     *   (`0`); the DAO never clamps or interprets the value.
     * @param timestamp Epoch millis to write into `lastReview`.
     */
    suspend fun setConsecutiveCorrect(id: Int, value: Int, timestamp: Long): Int =
        setConsecutiveCorrectCore(id, value, timestamp) +
            setConsecutiveCorrectUser(id, value, timestamp)

    @Query("UPDATE core_words SET notes = :notes WHERE id = :id")
    suspend fun setNotesCore(id: Int, notes: String): Int

    @Query("UPDATE user_words SET notes = :notes WHERE id = :id")
    suspend fun setNotesUser(id: Int, notes: String): Int

    /** Updates the personal note attached to a word. */
    suspend fun setNotes(id: Int, notes: String): Int =
        setNotesCore(id, notes) + setNotesUser(id, notes)

    @Query("UPDATE core_words SET customDifficulty = :difficulty WHERE id = :id")
    suspend fun setCustomDifficultyCore(id: Int, difficulty: Difficulty?): Int

    @Query("UPDATE user_words SET customDifficulty = :difficulty WHERE id = :id")
    suspend fun setCustomDifficultyUser(id: Int, difficulty: Difficulty?): Int

    /** Overrides the difficulty for a single word. Pass `null` to reset. */
    suspend fun setCustomDifficulty(id: Int, difficulty: Difficulty?): Int =
        setCustomDifficultyCore(id, difficulty) +
            setCustomDifficultyUser(id, difficulty)
    // endregion

    // region: Filtered reactive queries
    /** Reactive list of words the user has marked as learned. */
    @Query("SELECT * FROM words_view WHERE status = 'LEARNED' ORDER BY word ASC")
    fun getLearnedWords(): Flow<List<WordEntity>>

    /** Reactive list of words the user has favorited. */
    @Query("SELECT * FROM words_view WHERE favorite = 1 ORDER BY word ASC")
    fun getFavoriteWords(): Flow<List<WordEntity>>

    /**
     * Reactive list of words whose `nextReview` timestamp is at or
     * before [now] and which have not been learned yet.
     */
    @Query(
        """
        SELECT * FROM words_view
        WHERE status != 'LEARNED'
          AND nextReview IS NOT NULL
          AND nextReview <= :now
        ORDER BY nextReview ASC
        """
    )
    fun getDueForReview(now: Long): Flow<List<WordEntity>>

    /** Words that have been reviewed at least once but are not yet learned. */
    @Query("SELECT * FROM words_view WHERE status = 'ALMOST' ORDER BY word ASC")
    fun getInProgressWords(): Flow<List<WordEntity>>
    // endregion

    // region: Aggregate counts (Flow)
    /** Reactive count of every word in the dictionary. */
    @Query("SELECT COUNT(*) FROM words_view")
    fun countAllFlow(): Flow<Int>

    /** Reactive count of words marked as learned. */
    @Query("SELECT COUNT(*) FROM words_view WHERE status = 'LEARNED'")
    fun countLearnedFlow(): Flow<Int>

    /** Reactive count of words marked as favorite. */
    @Query("SELECT COUNT(*) FROM words_view WHERE favorite = 1")
    fun countFavoritesFlow(): Flow<Int>

    /** Reactive count of words due for review right now. */
    @Query(
        """
        SELECT COUNT(*) FROM words_view
        WHERE status != 'LEARNED'
          AND nextReview IS NOT NULL
          AND nextReview <= :now
        """
    )
    fun countDueForReviewFlow(now: Long): Flow<Int>

    /** Reactive count of words in progress (status = ALMOST). */
    @Query("SELECT COUNT(*) FROM words_view WHERE status = 'ALMOST'")
    fun countInProgressFlow(): Flow<Int>

    /**
     * Reactive count of distinct word rows whose `lastReview` timestamp
     * is at or after [sinceMillis]. Used by the Progress screen to
     * estimate today's XP earnings from the review history.
     */
    @Query("SELECT COUNT(*) FROM words_view WHERE lastReview >= :sinceMillis")
    fun countReviewsSinceFlow(sinceMillis: Long): Flow<Int>
    // endregion

    // region: Aggregate counts (one-shot)
    /**
     * Returns the number of rows currently stored across the two
     * underlying tables.
     *
     * Used by `MainActivity` to decide whether the JSON seed should run
     * (only when the view is empty).
     */
    @Query("SELECT COUNT(*) FROM words_view")
    suspend fun countWords(): Int

    /** One-shot count of learned words. */
    @Query("SELECT COUNT(*) FROM words_view WHERE status = 'LEARNED'")
    suspend fun countLearned(): Int

    /** One-shot count of favorite words. */
    @Query("SELECT COUNT(*) FROM words_view WHERE favorite = 1")
    suspend fun countFavorites(): Int

    /** One-shot count of words due for review at [now]. */
    @Query(
        """
        SELECT COUNT(*) FROM words_view
        WHERE status != 'LEARNED'
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
            (SELECT COUNT(*) FROM words_view) AS totalWords,
            (SELECT COUNT(*) FROM words_view WHERE status = 'LEARNED') AS learnedWords,
            (SELECT COUNT(*) FROM words_view WHERE favorite = 1) AS favoriteWords,
            (SELECT COUNT(*) FROM words_view
                WHERE status != 'LEARNED'
                  AND nextReview IS NOT NULL
                  AND nextReview <= :now) AS dueForReview,
            (SELECT COUNT(*) FROM words_view
                WHERE status = 'ALMOST') AS inProgress
        """
    )
    fun observeProgressStats(now: Long): Flow<ProgressStats>
    // endregion

    // region: Maintenance
    /**
     * Removes a single user-added word by id.
     *
     * The caller is responsible for validating that the row is
     * user-owned (see [data.database.entities.isUserAdded]); the DAO
     * intentionally does not gate deletes so that future tooling
     * can still remove user rows if needed.
     *
     * @return Number of rows deleted (0 if the id does not exist).
     */
    @Query("DELETE FROM user_words WHERE id = :id")
    suspend fun deleteUserWord(id: Int): Int

    /**
     * Removes every row from `core_words`. Intended for re-seed flows
     * that replace the bundled dictionary with a newer version while
     * preserving the contents of `user_words`.
     *
     * @return Number of rows deleted.
     */
    @Query("DELETE FROM core_words")
    suspend fun deleteAllCoreWords(): Int
    // endregion

    // region: Per-category aggregate counts (Phase 4.6)
    /**
     * Total number of words in the dictionary that match [type]
     * (and [regular] when non-null) at the given [level].
     *
     * Used by the per-category gating evaluator to compute the
     * denominator of the learned-percentage requirement.
     *
     * @param type Word type literal (`"verb"`, `"adjective"`, …).
     * @param regular Verb-regularity filter. Pass `null` for
     *   non-verb types so the SQL ignores the column.
     * @param level Progression bucket (`word.level`).
     */
    @Query(
        """
        SELECT COUNT(*) FROM words_view
        WHERE `type` = :type
          AND (:regular IS NULL OR `regular` = :regular)
          AND `level` = :level
        """
    )
    suspend fun countWordsAt(type: String, regular: Boolean?, level: Int): Int

    /**
     * Number of words in the dictionary that match [type]
     * (and [regular] when non-null) at [level] **and** are marked
     * `LEARNED`.
     *
     * Used by the per-category gating evaluator to compute the
     * numerator of the learned-percentage requirement.
     */
    @Query(
        """
        SELECT COUNT(*) FROM words_view
        WHERE `type` = :type
          AND (:regular IS NULL OR `regular` = :regular)
          AND `level` = :level
          AND `status` = 'LEARNED'
        """
    )
    suspend fun countLearnedAt(type: String, regular: Boolean?, level: Int): Int

    /**
     * Highest level present in the dictionary for the given [type]
     * (and [regular] when non-null). Returns `0` when the
     * combination has no rows. Used to cap the category max level
     * displayed on the Progress screen.
     */
    @Query(
        """
        SELECT IFNULL(MAX(`level`), 0) FROM words_view
        WHERE `type` = :type
          AND (:regular IS NULL OR `regular` = :regular)
        """
    )
    suspend fun maxLevelByType(type: String, regular: Boolean?): Int
    // endregion
}