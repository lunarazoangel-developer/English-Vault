package data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per [Skill] holding the cumulative XP earned in that skill.
 *
 * Phase 7.6 introduces a four-skill progression system (Listening,
 * Speaking, Reading, Writing) that lives next to the existing
 * per-category progression (`category_progress`) and the global XP
 * counter (`user_profile.totalXp`). The schema is intentionally
 * minimal — a primary key, an XP total, and an `updatedAt`
 * timestamp — because the skill system is "infinite": there is no
 * level cap and no learned-percentage requirement, just a growing
 * XP number that drives a cyclic progress bar on the Progress
 * screen.
 *
 * `Migrations.MIGRATION_9_10` seeds one row per [Skill] so the UI
 * never sees a missing key.
 *
 * @property skillKey Stable identifier matching [Skill.key]
 *   (e.g. `"LISTENING"`).
 * @property xpTotal Cumulative XP earned in this skill. Updated by
 *   the gameplay loops once the XP-to-skill grant pipeline lands
 *   in a future phase.
 * @property updatedAt Epoch millis of the last mutation. Useful for
 *   analytics and "last played" surfacing in later iterations.
 */
@Entity(tableName = "skill_progress")
data class SkillProgressEntity(
    @PrimaryKey val skillKey: String,
    val xpTotal: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * Initial state for any skill. Used both by the migration
         * seed and by callers that need a non-null row before the
         * DAO has been queried.
         */
        fun initial(key: String): SkillProgressEntity =
            SkillProgressEntity(
                skillKey = key,
                xpTotal = 0,
                updatedAt = System.currentTimeMillis()
            )
    }
}