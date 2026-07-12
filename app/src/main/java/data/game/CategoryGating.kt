package data.game

import com.example.englishvault.ui.words.WordTypeFilter

/**
 * Pure constants for the per-category progression system.
 *
 * Phase 4.6 splits progression into eight parallel tracks (one per
 * grammatical category). Each category has its own XP total and
 * unlocked level. Advancing from level N to level N+1 inside a
 * category requires **both**:
 *
 *  1. At least [XP_MIN_PER_LEVEL] XP earned while at level N.
 *  2. At least [LEARNED_PCT_REQUIRED] (fraction, 0..1) of the words
 *     at level N in that category marked `LEARNED`.
 *
 * XP comes exclusively from correct answers in the
 * WordMatchVerbs mini-game; the per-answer grant is
 * [XP_PER_CORRECT_ANSWER].
 *
 * Constants live here (not on a Hilt-managed service) because they
 * are referenced from both the gameplay loop (`submitAnswer`) and
 * the gating evaluator without injecting any dependency.
 */
object CategoryGating {
    /** XP awarded for each correct answer in a mini-game round. */
    const val XP_PER_CORRECT_ANSWER: Int = 10

    /** Minimum XP earned at the current level to unlock the next. */
    const val XP_MIN_PER_LEVEL: Int = 50

    /**
     * Minimum fraction (0.0..1.0) of words at the current level
     * that must be `LEARNED` before the next level unlocks.
     */
    const val LEARNED_PCT_REQUIRED: Float = 0.80f

    /** Default level assigned to fresh installs (and freshly seeded categories). */
    const val DEFAULT_UNLOCKED_LEVEL: Int = 1

    /**
     * Categories eligible for the per-category progression system.
     * Mirrors the rows seeded into `category_progress` by
     * `Migrations.MIGRATION_6_7`.
     */
    val TRACKED_CATEGORIES: List<WordTypeFilter> = WordTypeFilter.TRACKED
}