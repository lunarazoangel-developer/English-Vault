package data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import data.database.entities.GameCoveredWordEntity

/**
 * Data access object for the `game_covered_words` table.
 *
 * Tracks which `WordEntity` rows the player has encountered inside
 * a synthetic mini-game bucket (`LETTER_SOUP`, `LISTENING`, …) at a
 * given dictionary level. The hybrid promotion gate for those
 * buckets reads [countCovered] and compares it against the
 * per-level dictionary total returned by
 * `WordDao.countWordsAtGameLevel` to enforce the
 * [data.game.CategoryGating.LEARNED_PCT_REQUIRED] rule.
 *
 * The DAO is intentionally minimal: only the three operations the
 * gate and its callers need.
 *
 * Provided to the rest of the app via
 * `DatabaseModule.provideGameCoveredWordsDao`.
 */
@Dao
interface GameCoveredWordsDao {

    /**
     * Inserts every row in [rows] with `INSERT OR IGNORE`, so a row
     * that already exists for `(categoryKey, wordId, level)` is
     * silently skipped. Returns the list of inserted row ids from
     * Room (`-1L` for rows that conflicted). The number of
     * non-`-1L` entries is the count of *newly covered* words.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rows: List<GameCoveredWordEntity>): List<Long>

    /**
     * Number of distinct words the player has covered at [level]
     * inside [categoryKey]. Used as the numerator of the hybrid
     * gate's coverage ratio.
     */
    @Query(
        "SELECT COUNT(*) FROM `game_covered_words` " +
            "WHERE `categoryKey` = :categoryKey AND `level` = :level"
    )
    suspend fun countCovered(categoryKey: String, level: Int): Int

    /**
     * Wipes every coverage row for [categoryKey] at [level]. Called
     * by the mini-game VMs immediately after a level unlock so the
     * next level starts with an empty coverage counter.
     *
     * @return Number of rows deleted.
     */
    @Query(
        "DELETE FROM `game_covered_words` " +
            "WHERE `categoryKey` = :categoryKey AND `level` = :level"
    )
    suspend fun clearLevel(categoryKey: String, level: Int): Int
}