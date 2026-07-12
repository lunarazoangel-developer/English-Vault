package data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per tracked word category holding the player's progression
 * inside that bucket.
 *
 * Phase 4.6 introduces per-category progression as an alternative
 * to the global `user_profile.totalXp`. Each grammatical type the
 * user can practise — regular verbs, irregular verbs, adjectives,
 * …, interjections — owns its own XP total and unlocked level.
 *
 * The primary key is the [categoryKey] string. The canonical set of
 * keys is the [com.example.englishvault.ui.words.WordTypeFilter.TRACKED]
 * list. `ALL` and `MINE` are intentionally excluded — they are
 * browsing convenience buckets on the Words screen, not categories
 * the player progresses through.
 *
 * `Migrations.MIGRATION_6_7` seeds one row per tracked category so
 * reads never see a missing key.
 *
 * @property categoryKey Stable identifier matching
 *   `WordTypeFilter.name` (e.g. `"VERBS_REGULAR"`).
 * @property xpTotal Cumulative XP earned in this category. Drives the
 *   per-category level via `UserLevel.levelFromXp`.
 * @property unlockedLevel Highest level the player can access in
 *   this category. Increments when both gating requirements (XP
 *   threshold + learned percentage) are satisfied at the current
 *   level.
 * @property xpSinceLevelUp XP accumulated since the last level-up.
 *   Resets to zero on promotion. Drives the "X / Y XP to level up"
 *   progress bar on the Progress screen.
 * @property updatedAt Timestamp of the last mutation. Useful for
 *   debugging and future analytics.
 */
@Entity(tableName = "category_progress")
data class CategoryProgressEntity(
    @PrimaryKey val categoryKey: String,
    val xpTotal: Int = 0,
    val unlockedLevel: Int = 1,
    val xpSinceLevelUp: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * Initial state for any category. Used both by the migration
         * seed and by callers that need a non-null row before the
         * DAO has been queried.
         */
        fun initial(key: String): CategoryProgressEntity =
            CategoryProgressEntity(
                categoryKey = key,
                xpTotal = 0,
                unlockedLevel = 1,
                xpSinceLevelUp = 0,
                updatedAt = System.currentTimeMillis()
            )
    }
}