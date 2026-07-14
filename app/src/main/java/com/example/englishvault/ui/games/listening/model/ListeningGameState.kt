package com.example.englishvault.ui.games.listening.model

import com.example.englishvault.ui.games.common.GameMode

/**
 * State machine for a Listening mini-game run.
 *
 *  - [Loading] — the level is being loaded from Room.
 *  - [Empty]  — the level has no eligible words (every entry is
 *    already LEARNED or there is no data at that level yet). The UI
 *    renders the same branded blue gradient used by the other mini-
 *    games so the empty state never looks like an unstyled toast.
 *  - [InProgress] — the player is listening to words and picking
 *    spellings. [lastAnswer] carries the most recent response so
 *    the UI can render a transient feedback overlay before
 *    auto-advancing.
 *  - [Finished] — every question has been answered (or the run ran
 *    out of lives in [GameMode.WORLD]). [errors] is populated with
 *    the wrong answers for the end screen.
 *
 * ## Modes (Phase 7.5)
 *
 * Two modes are supported, mirroring the Word Match Verbs mini-game:
 *  - [GameMode.NORMAL]: no lives, no timer, no items. Unlimited
 *    re-listens (the player can re-tap the 🔊 button as many times
 *    as desired without spending anything).
 *  - [GameMode.WORLD]: 3 lives, 10-second per-question timer, 2
 *    extra re-listens, and the 50/50 help (drop two wrong options).
 *    Wrong answers and time-outs each cost a life; when lives reach
 *    zero the run ends early.
 *
 * Mode is a per-run choice carried on [InProgress.mode] and on
 * [Finished.mode]. The mode itself is exposed by the ViewModel via
 * a separate `MutableStateFlow` so the dev toggle button can change
 * it without having to round-trip through `startGame`.
 */
sealed class ListeningGameState {

    object Loading : ListeningGameState()

    object Empty : ListeningGameState()

    data class InProgress(
        val questions: List<ListeningQuestion>,
        val currentIndex: Int,
        val correctCount: Int,
        val errors: List<ListeningError>,
        val lastAnswer: ListeningAnswer? = null,
        /**
         * XP accumulated per category key (matches
         * `WordTypeFilter.name`) during this run. Updated on every
         * correct answer and persisted to `category_progress` when
         * the run transitions to [Finished]. Lives in the state
         * machine so a single end-of-game grant handles all
         * per-category aggregation without needing to remember
         * individual questions.
         */
        val correctXpByCategory: Map<String, Int> = emptyMap(),
        /**
         * Active mode for this run. The default `NORMAL` preserves
         * the legacy behaviour for callers that do not opt in.
         */
        val mode: GameMode = GameMode.NORMAL,
        /**
         * Remaining lives. Always `0` in [GameMode.NORMAL]; starts
         * at [INITIAL_LIVES] in [GameMode.WORLD] and decrements on
         * every wrong answer / time-out.
         */
        val lives: Int = 0,
        /**
         * Milliseconds left on the current question's timer. `0`
         * in [GameMode.NORMAL] (no timer); reset to
         * [QUESTION_TIME_MS] every time a new question becomes
         * active in [GameMode.WORLD].
         */
        val timeRemainingMs: Long = 0,
        /**
         * Remaining "re-listen" uses for this run. Always `0` in
         * [GameMode.NORMAL] (the listen button is unlimited);
         * starts at [INITIAL_RELISTEN_ITEMS] in [GameMode.WORLD]
         * and decrements every time the player taps 🔊 outside the
         * free replay window.
         */
        val relistenItems: Int = 0,
        /**
         * Remaining 50/50 help uses for this run. Always `0` in
         * [GameMode.NORMAL]; starts at [INITIAL_HELP_ITEMS] in
         * [GameMode.WORLD].
         */
        val helpItems: Int = 0,
        /**
         * Options the 50/50 help has hidden on the current
         * question. Cleared automatically when the player advances
         * to the next question (or when the run ends).
         */
        val eliminatedOptions: Set<String> = emptySet(),
        /**
         * `true` when the current question ended because the
         * countdown ran out (rather than because the player tapped
         * an answer). Lets the UI render a "Time's up!" overlay in
         * addition to the standard ✗ feedback.
         */
        val timedOut: Boolean = false
    ) : ListeningGameState() {
        val totalQuestions: Int get() = questions.size
        val currentQuestion: ListeningQuestion? = questions.getOrNull(currentIndex)
        val isLastQuestion: Boolean get() = currentIndex >= questions.lastIndex
    }

    data class Finished(
        val totalQuestions: Int,
        val correctCount: Int,
        val errors: List<ListeningError>,
        val mode: GameMode = GameMode.NORMAL,
        /**
         * `true` when the run ended because lives reached zero,
         * not because every question was answered. Lets the end
         * screen render an "Out of lives" headline in world mode.
         */
        val outOfLives: Boolean = false
    ) : ListeningGameState()

    companion object {
        /** Maximum number of questions shown per run. */
        const val MAX_QUESTIONS_PER_GAME: Int = 20

        /** Lives granted at the start of a world-mode run. */
        const val INITIAL_LIVES: Int = 3

        /** Countdown duration per question in world mode. */
        const val QUESTION_TIME_MS: Long = 10_000L

        /** Extra re-listen items granted at the start of a world-mode run. */
        const val INITIAL_RELISTEN_ITEMS: Int = 2

        /** 50/50 items granted at the start of a world-mode run. */
        const val INITIAL_HELP_ITEMS: Int = 2

        /**
         * Single bucket used to track Listening XP across all
         * dictionary levels. Lives next to the existing
         * `category_progress` rows (verbs regular/irregular,
         * nouns, LETTER_SOUP, …) so the player can see their
         * progress in the same UI without a new table.
         */
        const val CATEGORY_KEY: String = "LISTENING"

        /** How often the world-mode timer ticks the countdown. */
        const val TIMER_TICK_MS: Long = 100L
    }
}