package com.example.englishvault.ui.progress.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.database.UserLevel
import data.database.dao.UserProfileDao
import data.database.dao.WordDao
import data.database.entities.Difficulty
import data.database.entities.ProgressStats
import data.database.entities.UserProfileEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the Progress screen.
 *
 * Surfaces every counter and derived value the screen renders as
 * reactive [StateFlow]s so Compose can observe them with
 * `collectAsState` without manual refresh.
 *
 * Derived values:
 *  - [xp] wraps [UserLevel.levelProgress] so the screen can show the
 *    level number plus a bar into the next level.
 *  - [dailyXp] estimates today's XP by multiplying the count of
 *    `lastReview >= startOfToday` rows by [XP_PER_REVIEW]. A real
 *    gamification system will replace this with persisted rewards.
 *  - [units] groups the dictionary by [Difficulty] so the "Your path"
 *    section shows three progress buckets instead of mock labels.
 *
 * Obtained from Compose via `hiltViewModel()`.
 */
@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val userProfileDao: UserProfileDao,
    private val wordDao: WordDao
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

    // region: Derived UI state
    /**
     * Level + XP slice into the current level. The screen renders the
     * level number as the headline and the bar to fill as
     * `xpIntoLevel / xpRequired`.
     *
     * Visual only for now: the actual XP reward mechanic is not in
     * place, but the math already tells us where the user sits on the
     * curve so the UI can show meaningful progress.
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

    /**
     * Three fixed buckets (EASY / MEDIUM / HARD) with `learned / total`
     * counts derived from the full dictionary.
     */
    val units: StateFlow<List<UnitProgress>> = wordDao.getAllWords()
        .map { words ->
            Difficulty.entries.map { diff ->
                val bucket = words.filter { it.difficulty == diff }
                UnitProgress(
                    name = diff.displayName(),
                    total = bucket.size,
                    learned = bucket.count { it.status == data.database.entities.LearningStatus.LEARNED }
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

/** Level snapshot consumed by the XP card. */
data class XpProgress(
    val level: Int,
    val xpIntoLevel: Int,
    val xpRequired: Int,
    val nextLevel: Int
)

/** A single "Your path" bucket displayed by the Progress screen. */
data class UnitProgress(
    val name: String,
    val total: Int,
    val learned: Int
)