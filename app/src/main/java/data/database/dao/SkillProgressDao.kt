package data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import data.database.entities.SkillProgressEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for the `skill_progress` table.
 *
 * Phase 7.6 introduces a four-skill progression system (Listening,
 * Speaking, Reading, Writing). Each skill owns one row holding its
 * cumulative XP total. The schema is intentionally minimal — there
 * is no level cap, no learned-percentage gate and no promotion
 * logic; the table just grows.
 *
 * Reactive reads return [Flow] so the Progress screen can observe
 * the four bars in a single subscription. Mutations are `suspend`
 * so they integrate with the rest of the app's coroutine graph.
 *
 * Provided to the rest of the app via
 * `DatabaseModule.provideSkillProgressDao`.
 */
@Dao
interface SkillProgressDao {

    // region: Reads
    /**
     * Reactive stream of every row in `skill_progress`, ordered
     * alphabetically by key. Re-emits whenever any row is inserted,
     * updated or deleted.
     */
    @Query("SELECT * FROM `skill_progress` ORDER BY `skillKey` ASC")
    fun observeAll(): Flow<List<SkillProgressEntity>>

    /**
     * One-shot lookup of a single skill by its stable key.
     *
     * @return The persisted row, or `null` if no row exists for
     *   [skillKey]. Callers should fall back to
     *   [SkillProgressEntity.initial] when this returns `null`.
     */
    @Query("SELECT * FROM `skill_progress` WHERE `skillKey` = :skillKey LIMIT 1")
    suspend fun get(skillKey: String): SkillProgressEntity?
    // endregion

    // region: Writes
    /**
     * Inserts the supplied row, ignoring the conflict when a row
     * with the same primary key already exists. The DAO never
     * overwrites existing progression data through this method —
     * use [grantXp] for that.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfMissing(row: SkillProgressEntity): Long

    /**
     * Inserts the initial row for [skillKey] if and only if no row
     * exists yet. Returns `true` when the row was inserted, `false`
     * when one already existed. Safe to call from anywhere on app
     * start-up.
     */
    suspend fun seedIfMissing(skillKey: String): Boolean {
        return insertIfMissing(SkillProgressEntity.initial(skillKey)) != -1L
    }

    /**
     * Bulk-seeds every skill. Idempotent: rows that already exist
     * are left untouched. Designed to be called once after a schema
     * upgrade.
     */
    suspend fun seedAll(skillKeys: List<String>) {
        for (key in skillKeys) seedIfMissing(key)
    }

    /**
     * Atomically adds [amount] XP to the skill [skillKey]. Negative
     * amounts are ignored by the SQL engine so callers can guard
     * against accidental XP loss.
     *
     * Wraps [grantXpInternal] with a defensive [seedIfMissing] so an
     * install that for any reason lacks the row (very old
     * pre-migration install, a wiped table, or a path that bypassed
     * `MIGRATION_9_10`) still ends up with the XP persisted. Without
     * this, Room's `@Update` only touches existing rows and the grant
     * silently fails (returns 0) — the same trap
     * `category_progress.grantXpAndMaybeUnlock` hit before Phase 7.11.
     *
     * @return Number of rows updated (0 if the row is missing — should
     *   not happen now that [seedIfMissing] runs first).
     */
    suspend fun grantXp(
        skillKey: String,
        amount: Int,
        timestamp: Long = System.currentTimeMillis()
    ): Int {
        seedIfMissing(skillKey)
        return grantXpInternal(skillKey, amount, timestamp)
    }

    /**
     * Raw `@Query` backing [grantXp]. Public so Room's generated
     * `SkillProgressDao_Impl` can override it, but the
     * `Internal`-suffixed name signals that callers should use
     * [grantXp] instead — it always seeds the row before calling
     * this.
     */
    @Query(
        "UPDATE `skill_progress` SET xpTotal = xpTotal + :amount, updatedAt = :timestamp " +
            "WHERE `skillKey` = :skillKey AND :amount >= 0"
    )
    suspend fun grantXpInternal(
        skillKey: String,
        amount: Int,
        timestamp: Long
    ): Int
    // endregion
}