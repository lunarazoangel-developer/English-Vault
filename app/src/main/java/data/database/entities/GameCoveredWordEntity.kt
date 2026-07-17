package data.database.entities

import androidx.room.Entity

/**
 * Tracks which words the player has "covered" inside a synthetic
 * mini-game bucket (currently `LETTER_SOUP` and `LISTENING`) at a
 * specific dictionary [level].
 *
 * A row is inserted the first time the player finds the word in
 * Letter Soup or answers it correctly in Listening. Subsequent
 * encounters are ignored at the SQL layer (`INSERT OR IGNORE`) so the
 * table deduplicates by `(categoryKey, wordId, level)` automatically.
 *
 * The hybrid promotion gate for synthetic buckets counts rows per
 * `(categoryKey, level)` and compares the count against the
 * per-level dictionary total. Reaching
 * [data.game.CategoryGating.LEARNED_PCT_REQUIRED] of distinct words
 * at the current level is the second half of the gate (XP being the
 * first half). When the level unlocks, the
 * [data.database.dao.GameCoveredWordsDao.clearLevel] helper wipes
 * the rows for the now-completed level so the next level starts with
 * a clean coverage counter.
 *
 * Storing the covered `wordId` rather than just a count lets the
 * implementation survive dictionary updates: a future re-seed can
 * delete stale rows without losing the semantic of "covered".
 *
 * @property categoryKey Stable synthetic-bucket identifier
 *   (`"LETTER_SOUP"` / `"LISTENING"`). Kept in the table so the
 *   same DAO can back several games without needing one table per
 *   bucket.
 * @property wordId `WordEntity.id` of the covered word (lives in
 *   `core_words` or `user_words`; the table does not care).
 * @property level Dictionary level the word was covered at. The
 *   coverage gate is per level: covering a word at level 2 does
 *   not contribute to the gate at level 1.
 * @property coveredAt Epoch millis of the first coverage. Stored
 *   for analytics / future spaced-repetition scheduling.
 */
@Entity(
    tableName = "game_covered_words",
    primaryKeys = ["categoryKey", "wordId", "level"]
)
data class GameCoveredWordEntity(
    val categoryKey: String,
    val wordId: Long,
    val level: Int,
    val coveredAt: Long
)