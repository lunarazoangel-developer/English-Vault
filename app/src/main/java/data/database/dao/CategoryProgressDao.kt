package data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import data.database.entities.CategoryProgressEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for the `category_progress` table.
 *
 * Phase 4.6 wires per-category progression into the database. Each
 * tracked category owns one row (see
 * [com.example.englishvault.ui.words.WordTypeFilter.TRACKED]) holding
 * the cumulative XP, the unlocked level and the XP earned since the
 * last promotion.
 *
 * Reactive reads return [Flow] so the Progress screen can observe
 * every category without manual refresh. Mutations are `suspend` so
 * they integrate with the rest of the app's coroutine graph.
 *
 * Provided to the rest of the app via
 * `DatabaseModule.provideCategoryProgressDao`.
 */
@Dao
interface CategoryProgressDao {

    // region: Reads
    /**
     * Reactive stream of every row in `category_progress`. Re-emits
     * whenever any row is inserted, updated or deleted.
     */
    @Query("SELECT * FROM `category_progress` ORDER BY `categoryKey` ASC")
    fun observeAll(): Flow<List<CategoryProgressEntity>>

    /**
     * One-shot lookup of a single category by its stable key.
     *
     * @return The persisted row, or `null` if no row exists for
     *   [categoryKey]. Callers should fall back to
     *   [CategoryProgressEntity.initial] when this returns `null`.
     */
    @Query("SELECT * FROM `category_progress` WHERE `categoryKey` = :categoryKey LIMIT 1")
    suspend fun get(categoryKey: String): CategoryProgressEntity?
    // endregion

    // region: Writes
    /**
     * Inserts the supplied row, ignoring the conflict when a row
     * with the same primary key already exists. The DAO never
     * overwrites existing progression data through this method — use
     * [update] or [grantXpAndMaybeUnlock] for that.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfMissing(row: CategoryProgressEntity): Long

    /**
     * Inserts the initial row for [categoryKey] if and only if no row
     * exists yet. Returns `true` when the row was inserted, `false`
     * when one already existed. Safe to call from anywhere on app
     * start-up.
     */
    suspend fun seedIfMissing(categoryKey: String): Boolean {
        return insertIfMissing(CategoryProgressEntity.initial(categoryKey)) != -1L
    }

    /**
     * Bulk-seeds every tracked category. Idempotent: rows that already
     * exist are left untouched. Designed to be called once after a
     * schema upgrade.
     */
    suspend fun seedAll(categoryKeys: List<String>) {
        for (key in categoryKeys) seedIfMissing(key)
    }

    /**
     * Full-row replacement. Use for ad-hoc patches — the gameplay
     * loop should go through [grantXpAndMaybeUnlock] instead so the
     * `xpSinceLevelUp` reset semantics stay consistent.
     */
    @Update
    suspend fun update(row: CategoryProgressEntity): Int
    // endregion

    // region: Gameplay transactions
    /**
     * Adds [amount] XP to the category [categoryKey], bumping both
     * `xpTotal` and `xpSinceLevelUp`, then evaluates the hybrid gate
     * (`XP_MIN_PER_LEVEL` + `LEARNED_PCT_REQUIRED`) using the
     * caller-supplied counters. When both requirements are met and
     * the player has not yet reached the per-category max level, the
     * new level becomes `unlockedLevel` and `xpSinceLevelUp` resets
     * to zero.
     *
     * The transaction guarantees that the XP grant and the
     * promotion (if any) commit atomically — readers never observe
     * an intermediate state where the XP is granted but the level
     * has not been unlocked yet.
     *
     * @param categoryKey Target row.
     * @param amount XP to grant. Must be non-negative.
     * @param meetsXp Whether the hybrid gate's XP requirement is
     *   satisfied. Typically `xpSinceLevelUp + amount >= XP_MIN_PER_LEVEL`.
     * @param meetsLearnedPct Whether the learned-percentage
     *   requirement is satisfied at the current level.
     * @param targetUnlockedLevel The new value for `unlockedLevel`
     *   when the gate passes. Pass the current value to keep it
     *   unchanged when no promotion fires.
     */
    @Transaction
    suspend fun grantXpAndMaybeUnlock(
        categoryKey: String,
        amount: Int,
        meetsXp: Boolean,
        meetsLearnedPct: Boolean,
        targetUnlockedLevel: Int
    ) {
        require(amount >= 0) { "amount must be non-negative, was $amount" }
        val current = get(categoryKey) ?: CategoryProgressEntity.initial(categoryKey)
        val newXpTotal = current.xpTotal + amount
        val newXpSince = current.xpSinceLevelUp + amount

        val shouldUnlock = meetsXp && meetsLearnedPct &&
            targetUnlockedLevel > current.unlockedLevel
        val finalXpSince = if (shouldUnlock) 0 else newXpSince
        val finalUnlocked = if (shouldUnlock) targetUnlockedLevel else current.unlockedLevel

        update(
            current.copy(
                xpTotal = newXpTotal,
                xpSinceLevelUp = finalXpSince,
                unlockedLevel = finalUnlocked,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
    // endregion
}