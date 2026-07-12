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
    val createdAt: Long = System.currentTimeMillis(),
    /**
     * Version of the bundled core dictionary that this device has
     * applied. Compared against [data.seed.DictionarySeeder.CORE_DICTIONARY_VERSION]
     * on every launch; when the bundled version is greater, the seeder
     * wipes `core_words` and re-imports the section files under
     * `assets/dictionary/`.
     *
     * Starts at `0` so any existing install (whose profile predates
     * this field) immediately re-seeds when the app upgrades.
     */
    val coreDictionaryVersion: Int = 0,
    /**
     * Lives the player has. Decremented when the player loses a level;
     * when it reaches zero the world map (Phase 7) becomes a game-over
     * state until a refill happens. Defaults to [DEFAULT_HEARTS] so a
     * freshly created profile can play right away.
     */
    val hearts: Int = DEFAULT_HEARTS,
    /**
     * Spendable currency earned for clearing levels and redeemed at
     * the in-world shop. The world map HUD surfaces this counter; the
     * actual shop is a placeholder for a future phase.
     */
    val coins: Int = 0
) {
    companion object {
        /** Fixed primary key — single-user schema. */
        const val SINGLE_USER_ID: Int = 1

        /** Default display name shown before the user customises it. */
        const val DEFAULT_NAME: String = "Player"

        /** Default daily XP target. Surfaced in the Progress screen. */
        const val DEFAULT_DAILY_GOAL: Int = 50

        /** Starting hearts for a fresh profile. */
        const val DEFAULT_HEARTS: Int = 5
    }
}