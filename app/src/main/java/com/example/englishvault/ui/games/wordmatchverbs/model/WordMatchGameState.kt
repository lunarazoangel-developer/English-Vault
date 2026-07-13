package com.example.englishvault.ui.games.wordmatchverbs.model

/**
 * State machine for the Word Match Verbs mini-game.
 *
 *  - [Loading] — the level is being loaded from Room.
 *  - [Empty]  — the level has no eligible verbs (everything is
 *    already LEARNED or there is no data at that level yet).
 *  - [InProgress] — the player is answering questions. [lastAnswer]
 *    carries the most recent response so the UI can render a
 *    transient feedback overlay before auto-advancing.
 *  - [Finished] — every question has been answered (or the run ran
 *    out of lives in [GameMode.WORLD]). [errors] is populated with
 *    the wrong answers for the end screen.
 *
 * ## Modes
 *
 * The mini-game supports two modes:
 *  - [GameMode.NORMAL]: no lives, no timer, no items. The current
 *    behaviour from earlier phases.
 *  - [GameMode.WORLD]: 3 lives, 10-second per-question timer, 2
 *    50/50 help items and 2 time-boost (+5s) items. Wrong answers
 *    and time-outs each cost a life; when lives reach zero the run
 *    ends early.
 *
 * Mode is a per-run choice carried on [InProgress.mode]. The mode
 * itself is exposed by the ViewModel via a separate
 * [MutableStateFlow] so the dev toggle button can change it without
 * having to round-trip through [startGame].
 */
sealed class WordMatchGameState {

    object Loading : WordMatchGameState()

    object Empty : WordMatchGameState()

    data class InProgress(
        val questions: List<WordMatchQuestion>,
        val currentIndex: Int,
        val correctCount: Int,
        val errors: List<WordMatchError>,
        val lastAnswer: WordMatchAnswer? = null,
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
         * Remaining lives. Always `0` in [GameMode.NORMAL]; starts at
         * [INITIAL_LIVES] in [GameMode.WORLD] and decrements on
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
         * Remaining 50/50 help items. Always `0` in
         * [GameMode.NORMAL]; starts at [INITIAL_HELP_ITEMS] in
         * [GameMode.WORLD].
         */
        val helpItems: Int = 0,
        /**
         * Remaining +5s time-boost items. Always `0` in
         * [GameMode.NORMAL]; starts at [INITIAL_TIME_BOOST_ITEMS] in
         * [GameMode.WORLD].
         */
        val timeBoostItems: Int = 0,
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
    ) : WordMatchGameState() {
        val totalQuestions: Int get() = questions.size
        val currentQuestion: WordMatchQuestion? = questions.getOrNull(currentIndex)
        val isLastQuestion: Boolean get() = currentIndex >= questions.lastIndex
    }

    data class Finished(
        val totalQuestions: Int,
        val correctCount: Int,
        val errors: List<WordMatchError>,
        val mode: GameMode = GameMode.NORMAL,
        /**
         * `true` when the run ended because lives reached zero, not
         * because every question was answered. Lets the end screen
         * render a "Out of lives" headline in world mode.
         */
        val outOfLives: Boolean = false
    ) : WordMatchGameState()

    companion object {
        /** Lives granted at the start of a world-mode run. */
        const val INITIAL_LIVES: Int = 3

        /** Countdown duration per question in world mode. */
        const val QUESTION_TIME_MS: Long = 10_000L

        /** 50/50 items granted at the start of a world-mode run. */
        const val INITIAL_HELP_ITEMS: Int = 2

        /** +5s items granted at the start of a world-mode run. */
        const val INITIAL_TIME_BOOST_ITEMS: Int = 2

        /** Milliseconds added per use of the time-boost item. */
        const val TIME_BOOST_MS: Long = 5_000L

        /**
         * Upper cap on the timer so chaining boosts cannot extend a
         * single question indefinitely. Two full cycles by default.
         */
        const val MAX_TIME_REMAINING_MS: Long = QUESTION_TIME_MS * 2
    }
}

/**
 * Game-mode flag for a Word Match Verbs run.
 *
 * The mode is held both by the ViewModel (for the dev toggle) and
 * on every [WordMatchGameState.InProgress] / [Finished] (so the UI
 * can branch on it without re-reading the VM).
 */
enum class GameMode { NORMAL, WORLD }