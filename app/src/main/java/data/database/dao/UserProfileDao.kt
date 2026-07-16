package data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import data.database.entities.UserProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for the `user_profile` table.
 *
 * Because the schema is single-row, every query defaults the primary
 * key to [UserProfileEntity.SINGLE_USER_ID] so callers do not need to
 * pass it explicitly. The DAO exposes both reactive ([Flow]) reads for
 * the Progress screen and atomic update methods used by the gamified
 * features (XP rewards, streak bumps, daily-goal updates, …).
 *
 * Provided to the rest of the app via `DatabaseModule.provideUserProfileDao`.
 */
@Dao
interface UserProfileDao {

    // region: Reads
    /**
     * Observes the profile as a Flow. Emits `null` until the seed runs,
     * then the persisted [UserProfileEntity].
     */
    @Query("SELECT * FROM user_profile WHERE id = :id LIMIT 1")
    fun observeProfile(id: Int = UserProfileEntity.SINGLE_USER_ID): Flow<UserProfileEntity?>

    /**
     * One-shot read used during seeding and by background jobs.
     *
     * @return The persisted profile, or null if it has not been seeded.
     */
    @Query("SELECT * FROM user_profile WHERE id = :id LIMIT 1")
    suspend fun getProfile(id: Int = UserProfileEntity.SINGLE_USER_ID): UserProfileEntity?
    // endregion

    // region: Writes
    /**
     * Inserts or replaces the profile row.
     *
     * Conflicts (matching primary key) are resolved by replacing the
     * existing row, which keeps the upsert semantics simple.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: UserProfileEntity)

    /**
     * Atomically adds XP to the profile. Negative amounts are ignored by
     * the SQL engine so callers can guard against accidental XP loss.
     *
     * @return Number of rows updated (0 if the profile is missing).
     */
    @Query("UPDATE user_profile SET totalXp = totalXp + :amount WHERE id = :id AND :amount >= 0")
    suspend fun addXp(
        amount: Int,
        id: Int = UserProfileEntity.SINGLE_USER_ID
    ): Int

    /**
     * Updates the streak counter and the date it corresponds to.
     * Typically called once per day by the streak update routine.
     */
    @Query("UPDATE user_profile SET streakDays = :days, lastStreakDate = :date WHERE id = :id")
    suspend fun updateStreak(
        days: Int,
        date: Long,
        id: Int = UserProfileEntity.SINGLE_USER_ID
    ): Int

    /** Updates the XP target for the daily-goal card. */
    @Query("UPDATE user_profile SET dailyGoalXp = :goal WHERE id = :id")
    suspend fun updateDailyGoal(
        goal: Int,
        id: Int = UserProfileEntity.SINGLE_USER_ID
    ): Int

    /** Renames the user. */
    @Query("UPDATE user_profile SET name = :name WHERE id = :id")
    suspend fun updateName(
        name: String,
        id: Int = UserProfileEntity.SINGLE_USER_ID
    ): Int

    /**
     * Atomically adjusts the player's heart counter. Negative amounts
     * are ignored by the SQL engine so callers can guard against
     * accidental heart loss; pass positive values to refill.
     *
     * @return Number of rows updated (0 if the profile is missing).
     */
    @Query("UPDATE user_profile SET hearts = hearts + :amount WHERE id = :id AND :amount >= 0")
    suspend fun addHearts(
        amount: Int,
        id: Int = UserProfileEntity.SINGLE_USER_ID
    ): Int

    /**
     * Atomically adjusts the player's coin counter. Negative amounts
     * (spending coins) are also accepted because purchases can reduce
     * the balance — unlike XP, coins are spendable currency.
     *
     * @return Number of rows updated (0 if the profile is missing).
     */
    @Query("UPDATE user_profile SET coins = coins + :amount WHERE id = :id")
    suspend fun addCoins(
        amount: Int,
        id: Int = UserProfileEntity.SINGLE_USER_ID
    ): Int

    /**
     * Updates the music volume slider value in `[0.0, 1.0]`. Callers
     * are expected to clamp the input; the slider on the Settings
     * screen already constrains it.
     */
    @Query("UPDATE user_profile SET musicVolume = :volume WHERE id = :id")
    suspend fun updateMusicVolume(
        volume: Float,
        id: Int = UserProfileEntity.SINGLE_USER_ID
    ): Int

    /**
     * Updates the effects volume slider value in `[0.0, 1.0]`. Callers
     * are expected to clamp the input; the slider on the Settings
     * screen already constrains it.
     */
    @Query("UPDATE user_profile SET effectsVolume = :volume WHERE id = :id")
    suspend fun updateEffectsVolume(
        volume: Float,
        id: Int = UserProfileEntity.SINGLE_USER_ID
    ): Int

    /**
     * Persists the user's choice of color scheme. Allowed values are
     * [UserProfileEntity.THEME_MODE_DARK] and
     * [UserProfileEntity.THEME_MODE_LIGHT] — the Settings UI is the
     * only writer and constrains the input, so the DAO does not
     * re-validate.
     */
    @Query("UPDATE user_profile SET themeMode = :mode WHERE id = :id")
    suspend fun updateThemeMode(
        mode: String,
        id: Int = UserProfileEntity.SINGLE_USER_ID
    ): Int
    // endregion

    // region: Maintenance
    /** Removes the profile row. Intended for debug / reset flows. */
    @Query("DELETE FROM user_profile")
    suspend fun resetProfile()
    // endregion
}