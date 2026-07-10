package data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row table holding the local user profile.
 *
 * Phase 3 introduces a "game-like" feel to English Vault: the user
 * accumulates XP, levels up, builds a daily streak and tracks a daily
 * goal. All those counters live here.
 *
 * The schema intentionally uses a fixed primary key
 * ([SINGLE_USER_ID]) because the app is single-user for now. When
 * multi-account support lands (Phase 5+) the primary key will become
 * auto-generated and an extra "current user id" preference will pick
 * which row the UI reads.
 *
 * Level is **not** persisted — it is derived from [totalXp] via
 * `data.database.UserLevel.levelFromXp`. Storing it would risk
 * drift between the two values; deriving keeps them in sync.
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = SINGLE_USER_ID,
    val name: String = DEFAULT_NAME,
    val totalXp: Int = 0,
    val streakDays: Int = 0,
    /** Epoch millis of the most recent day the user practised. */
    val lastStreakDate: Long? = null,
    val dailyGoalXp: Int = DEFAULT_DAILY_GOAL,
    /** Epoch millis when this row was first created. */
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        /** Fixed primary key — single-user schema. */
        const val SINGLE_USER_ID: Int = 1

        /** Default display name shown before the user customises it. */
        const val DEFAULT_NAME: String = "Player"

        /** Default daily XP target. Surfaced in the Progress screen. */
        const val DEFAULT_DAILY_GOAL: Int = 50
    }
}