package com.example.englishvault.ui.progress.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.englishvault.ui.words.WordTypeFilter
import data.database.UserLevel
import data.database.dao.CategoryProgressDao
import data.database.dao.UserProfileDao
import data.database.dao.WordDao
import data.database.entities.CategoryProgressEntity
import data.database.entities.Difficulty
import data.database.entities.LearningStatus
import data.database.entities.ProgressStats
import data.database.entities.UserProfileEntity
import data.database.entities.WordEntity
import data.game.CategoryGating
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the Progress screen.
 *
 * Surfaces every counter and derived value the screen renders as
 * reactive [StateFlow]s so Compose can observe them with
 * `collectAsState` without manual refresh.
 *
 * Phase 4.6 — per-category progression:
 *  - [categoryProgress] combines the `category_progress` table with
 *    the full word list to produce one [CategoryProgressUi] per
 *    tracked category, including level, XP bar, learned-percentage,
 *    hybrid-gate status and a `canUnlockNext` flag.
 *  - The eight categories are emitted in the canonical order
 *    declared by `WordTypeFilter.TRACKED`.
 *
 * Legacy state retained for backwards compatibility:
 *  - [units] still groups by [Difficulty] for callers that want it
 *    (the screen no longer renders them).
 */
@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val userProfileDao: UserProfileDao,
    private val wordDao: WordDao,
    private val categoryProgressDao: CategoryProgressDao
) : ViewModel() {

    // region: Profile / stats
    val profile: StateFlow<UserProfileEntity?> = userProfileDao.observeProfile()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    val stats: StateFlow<ProgressStats> = wordDao.observeProgressStats(System.currentTimeMillis())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = ProgressStats.EMPTY
        )
    // endregion

    // region: Derived UI state — global XP / streak
    /**
     * Level + XP slice into the current level for the global
     * `user_profile.totalXp`. The screen renders the level number as
     * the headline and the bar to fill as `xpIntoLevel / xpRequired`.
     */
    val xp: StateFlow<XpProgress> = profile
        .map { p ->
            val totalXp = p?.totalXp ?: 0
            val level = UserLevel.levelFromXp(totalXp)
            val (into, required) = UserLevel.levelProgress(totalXp)
            XpProgress(
                level = level,
                xpIntoLevel = into,
                xpRequired = required,
                nextLevel = level + 1
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = XpProgress(level = 1, xpIntoLevel = 0, xpRequired = 1, nextLevel = 2)
        )

    /**
     * Estimated XP earned today: each review recorded since UTC midnight
     * counts as [XP_PER_REVIEW] points. Will be replaced by an explicit
     * persisted counter when the gamification system lands.
     */
    val dailyXp: StateFlow<Int> = wordDao.countReviewsSinceFlow(startOfTodayMillis())
        .map { reviews -> reviews * XP_PER_REVIEW }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = 0
        )
    // endregion

    // region: Derived UI state — per-category progression (Phase 4.6)
    /**
     * Eight [CategoryProgressUi] entries (one per tracked category) in
     * the canonical [WordTypeFilter.TRACKED] order. Re-computed
     * whenever either the `category_progress` table or the
     * dictionary mutates.
     *
     * The level number is derived from `xpTotal` via
     * [UserLevel.levelFromXp] and clamped to the dictionary max
     * level for that category so a category without level-5 entries
     * does not display an unreachable level.
     */
    val categoryProgress: StateFlow<List<CategoryProgressUi>> = combine(
        categoryProgressDao.observeAll(),
        wordDao.getAllWords()
    ) { rows, words ->
        CategoryGating.TRACKED_CATEGORIES.map { filter ->
            buildCategoryProgress(filter, rows, words)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = CategoryGating.TRACKED_CATEGORIES.map {
            CategoryProgressUi.empty(it)
        }
    )

    /**
     * Projects a single [WordTypeFilter] into the UI bundle. Pulled
     * out so the [combine] block stays declarative.
     */
    private fun buildCategoryProgress(
        filter: WordTypeFilter,
        rows: List<CategoryProgressEntity>,
        words: List<WordEntity>
    ): CategoryProgressUi {
        val row = rows.firstOrNull { it.categoryKey == filter.name }
            ?: CategoryProgressEntity.initial(filter.name)

        val maxLevel = words.asSequence()
            .filter { filter.matches(it) }
            .map { it.level }
            .maxOrNull()
            ?.coerceAtLeast(1)
            ?: 1

        val derivedLevel = UserLevel.levelFromXp(row.xpTotal)
            .coerceIn(1, maxLevel)

        val (xpInto, xpRequired) = UserLevel.levelProgress(row.xpTotal)
            .let { (into, required) -> into to required.coerceAtLeast(1) }

        val bucket = words.filter { filter.matches(it) && it.level == derivedLevel }
        val totalAtLevel = bucket.size
        val learnedAtLevel = bucket.count { it.status == LearningStatus.LEARNED }
        val learnedPct = if (totalAtLevel == 0) 0f else learnedAtLevel.toFloat() / totalAtLevel

        val meetsXp = row.xpSinceLevelUp >= CategoryGating.XP_MIN_PER_LEVEL
        val meetsLearnedPct = learnedPct >= CategoryGating.LEARNED_PCT_REQUIRED
        val atMaxLevel = derivedLevel >= maxLevel

        return CategoryProgressUi(
            filter = filter,
            currentLevel = derivedLevel,
            maxLevel = maxLevel,
            xpIntoLevel = xpInto,
            xpRequired = xpRequired,
            learnedCount = learnedAtLevel,
            totalCount = totalAtLevel,
            learnedPct = learnedPct,
            xpSinceLevelUp = row.xpSinceLevelUp,
            meetsXp = meetsXp,
            meetsLearnedPct = meetsLearnedPct,
            canUnlockNext = !atMaxLevel && meetsXp && meetsLearnedPct,
            locked = atMaxLevel
        )
    }
    // endregion

    // region: Legacy Difficulty buckets (kept for callers; not rendered)
    /**
     * Three fixed buckets (EASY / MEDIUM / HARD) with `learned / total`
     * counts derived from the full dictionary. Retained for any
     * caller that still consumes it; the Progress screen now reads
     * from [categoryProgress] instead.
     */
    val units: StateFlow<List<UnitProgress>> = wordDao.getAllWords()
        .map { words ->
            Difficulty.entries.map { diff ->
                val bucket = words.filter { it.difficulty == diff }
                UnitProgress(
                    name = diff.displayName(),
                    total = bucket.size,
                    learned = bucket.count { it.status == LearningStatus.LEARNED }
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )
    // endregion

    companion object {
        /** Subscription grace period before the upstream Flow is cancelled. */
        private const val STOP_TIMEOUT_MILLIS: Long = 5_000

        /** XP awarded per recorded review. Visual estimate until the rewards table lands. */
        private const val XP_PER_REVIEW: Int = 10

        /** UTC midnight of today in epoch millis. */
        private fun startOfTodayMillis(): Long {
            val oneDayMs = 86_400_000L
            return (System.currentTimeMillis() / oneDayMs) * oneDayMs
        }

        /** Pretty-prints the enum value: `EASY` → `"Easy"`, `MEDIUM` → `"Medium"`. */
        private fun Difficulty.displayName(): String =
            name.lowercase().replaceFirstChar { it.uppercase() }
    }
}

/** Level snapshot consumed by the global XP card. */
data class XpProgress(
    val level: Int,
    val xpIntoLevel: Int,
    val xpRequired: Int,
    val nextLevel: Int
)

/** Legacy Difficulty bucket kept for backwards-compatible callers. */
data class UnitProgress(
    val name: String,
    val total: Int,
    val learned: Int
)

/**
 * Per-category progression bundle rendered by the Progress screen.
 *
 * All numbers are pre-computed in [ProgressViewModel] so the
 * composable layer stays free of business logic.
 *
 * @property filter The grammatical bucket this row represents.
 * @property currentLevel Derived from `xpTotal` and clamped to
 *   [maxLevel] so a category without level-5 entries never
 *   advertises an unreachable level.
 * @property maxLevel Highest level present in the dictionary for
 *   this category.
 * @property xpIntoLevel XP earned within the current level (the
 *   numerator of the XP bar).
 * @property xpRequired XP needed to reach the next level (the
 *   denominator). Always `>= 1` to avoid divide-by-zero in the bar.
 * @property learnedCount Words at [currentLevel] marked `LEARNED`.
 * @property totalCount Words at [currentLevel] total.
 * @property learnedPct `learnedCount / totalCount`, in `[0, 1]`.
 * @property xpSinceLevelUp XP earned since the last promotion.
 *   Drives the "Necesitas X XP" message.
 * @property meetsXp True when [xpSinceLevelUp] >= `XP_MIN_PER_LEVEL`.
 * @property meetsLearnedPct True when [learnedPct] >=
 *   `LEARNED_PCT_REQUIRED`.
 * @property canUnlockNext True when both gates pass and the player
 *   has not reached the category max.
 * @property locked True when [currentLevel] has reached [maxLevel].
 *   UI renders a celebratory "maxed out" badge in that case.
 */
data class CategoryProgressUi(
    val filter: WordTypeFilter,
    val currentLevel: Int,
    val maxLevel: Int,
    val xpIntoLevel: Int,
    val xpRequired: Int,
    val learnedCount: Int,
    val totalCount: Int,
    val learnedPct: Float,
    val xpSinceLevelUp: Int,
    val meetsXp: Boolean,
    val meetsLearnedPct: Boolean,
    val canUnlockNext: Boolean,
    val locked: Boolean
) {
    companion object {
        /**
         * Empty placeholder used while the underlying flows have not
         * produced their first emission. Keeps the screen rendering
         * a stable layout from the very first frame.
         */
        fun empty(filter: WordTypeFilter): CategoryProgressUi = CategoryProgressUi(
            filter = filter,
            currentLevel = 1,
            maxLevel = 1,
            xpIntoLevel = 0,
            xpRequired = 1,
            learnedCount = 0,
            totalCount = 0,
            learnedPct = 0f,
            xpSinceLevelUp = 0,
            meetsXp = false,
            meetsLearnedPct = false,
            canUnlockNext = false,
            locked = false
        )
    }
}