package com.example.englishvault.ui.progress.viewmodel

import data.database.entities.Skill

/**
 * Per-skill progression bundle rendered by the Progress screen.
 *
 * The skill system is intentionally "infinite": there is no level
 * cap, no learned-percentage gate and no promotion logic. The
 * [progressFraction] is therefore derived against a [cycleSize]
 * (1000 XP by default) so the bar fills up as the user practices,
 * resets to zero when a cycle completes, and grows the cycle
 * counter. This gives the user a satisfying "chunking" feel
 * without pretending the system has a hard maximum.
 *
 * @property skill The [Skill] this row represents.
 * @property xpTotal Cumulative XP earned in this skill.
 * @property cycleSize XP target for one visible bar fill. Tweaked
 *   here (rather than in the DAO) so the visual rhythm can evolve
 *   without a schema migration.
 */
data class SkillProgressUi(
    val skill: Skill,
    val xpTotal: Int,
    val cycleSize: Int = DEFAULT_CYCLE_SIZE
) {
    /**
     * Number of completed cycles (0-indexed). A user with 2500 XP
     * has completed 2 cycles and is working on the 3rd.
     */
    val cycleIndex: Int get() = xpTotal / cycleSize

    /**
     * XP accumulated inside the current cycle, in `[0, cycleSize)`.
     */
    val xpInCycle: Int get() = xpTotal % cycleSize

    /**
     * Fractional fill of the current cycle, in `[0f, 1f]`. Used as
     * the `progress` argument to `LinearProgressIndicator`.
     */
    val progressFraction: Float get() = xpInCycle.toFloat() / cycleSize

    companion object {
        /** XP needed to fill one visible cycle on the progress bar. */
        const val DEFAULT_CYCLE_SIZE: Int = 1000

        /**
         * Empty placeholder used while the underlying flow has not
         * produced its first emission. Keeps the screen rendering
         * a stable layout from the very first frame.
         */
        fun empty(skill: Skill): SkillProgressUi =
            SkillProgressUi(skill = skill, xpTotal = 0)
    }
}