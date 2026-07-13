package com.example.englishvault.ui.games.lettersoup.model

/**
 * State machine for a Letter Soup run.
 *
 *  - [Loading] — the dictionary is being queried and the board is
 *    being generated.
 *  - [InProgress] — the player is swapping cells. [selectedCell]
 *    carries the currently highlighted (row, col) or `null` when
 *    nothing is selected. [movesLeft] is the remaining move budget;
 *    [wordsFixed] is the running count of correctly swapped words.
 *    [wordsToWin] mirrors the number of placements generated on this
 *    board (typically 5) and drives the end-of-run check.
 *  - [Finished] — every placement on this board has been fixed. The
 *    board never resets mid-run, so reaching `wordsFixed ==
 *    wordsToWin` ends the game. World-mode runs can also finish
 *    early when [timeRemainingMs] hits zero (see [timedOut]).
 *
 * ## Modes (Phase 7.4)
 *
 * The mini-game supports two modes:
 *  - [HintMode.NORMAL]: unlimited hints, no time limit.
 *  - [HintMode.WORLD]: a 5-minute countdown, two 50/50 location
 *    hints and two English-translation hints. Wrong answers do not
 *    consume moves differently than in normal mode; only the time
 *    budget and the hint inventory make world runs harder.
 *
 * Mode is a per-run choice carried on [InProgress.mode] and on
 * [Finished.mode]. The mode itself is exposed by the ViewModel
 * via a separate [MutableStateFlow] so the dev toggle button can
 * change it without having to round-trip through [startGame].
 */
sealed class LetterSoupGameState {

    object Loading : LetterSoupGameState()

    data class InProgress(
        val level: Int,
        val board: LetterSoupBoard,
        val movesLeft: Int,
        val wordsFixed: Int,
        /**
         * Total placements generated on this board. Becomes the win
         * threshold. Typically `5` but may be lower when the generator
         * could not fit the requested count.
         */
        val wordsToWin: Int,
        val selectedCell: Pair<Int, Int>? = null,
        val xpByCategory: Map<String, Int> = emptyMap(),
        /**
         * `true` for a single tick after a failed swap so the UI can
         * flash the two cells red before clearing the selection.
         */
        val lastSwapFailedCells: List<Pair<Int, Int>> = emptyList(),
        /**
         * The placement that was just fixed by the player's last
         * successful swap, if any. Used by the UI to flash the word
         * green before the board regenerates.
         */
        val lastFixedWord: LetterSoupWord? = null,
        /**
         * `true` while the location hint is showing — the UI marks
         * the wrong-letter cell with the ❌ badge and pulse
         * animation. Auto-clears after [HINT_TIMEOUT_MS].
         */
        val isLocationHintRevealed: Boolean = false,
        /**
         * `true` while the English hint is showing — the UI replaces
         * the active word's Spanish translation in the always-visible
         * list with the English word. Auto-clears after
         * [HINT_TIMEOUT_MS].
         */
        val isEnglishHintRevealed: Boolean = false,
        /**
         * Active mode for this run. Default `NORMAL` preserves the
         * legacy behaviour.
         */
        val mode: HintMode = HintMode.NORMAL,
        /**
         * Remaining location-hint uses for this run. `0` in
         * [HintMode.NORMAL] (unlimited via the toggle flip); starts
         * at [INITIAL_LOCATION_HINTS] in [HintMode.WORLD] and
         * decrements on each successful reveal.
         */
        val locationHintsRemaining: Int = 0,
        /**
         * Remaining English-hint uses for this run. `0` in
         * [HintMode.NORMAL]; starts at [INITIAL_ENGLISH_HINTS] in
         * [HintMode.WORLD].
         */
        val englishHintsRemaining: Int = 0,
        /**
         * Milliseconds left on the world-mode countdown. `0` in
         * [HintMode.NORMAL]; set to [WORLD_GAME_TIME_MS] when a
         * world run starts. Decremented every 100 ms by the VM.
         */
        val timeRemainingMs: Long = 0L,
        /**
         * `true` when the world-mode timer reached zero before the
         * player fixed every placement. Lets the end screen render
         * a "Time's up!" headline.
         */
        val timedOut: Boolean = false
    ) : LetterSoupGameState()

    data class Finished(
        val level: Int,
        val won: Boolean,
        val wordsFixed: Int,
        val wordsToWin: Int,
        val xpByCategory: Map<String, Int>,
        val mode: HintMode = HintMode.NORMAL,
        /**
         * `true` when the world-mode countdown ran out before the
         * player fixed every placement.
         */
        val timedOut: Boolean = false
    ) : LetterSoupGameState()

    companion object {
        /** Total moves granted at the start of a run. */
        const val MAX_MOVES: Int = 20

        /**
         * Target number of placements per board. The generator tries
         * to reach this count; if it cannot, [wordsToWin] reflects
         * the actual placements.
         */
        const val TARGET_PLACEMENTS: Int = 5

        /** How long each hint stays on screen before auto-hiding. */
        const val HINT_TIMEOUT_MS: Long = 3_000

        /**
         * Single bucket used to track Letter Soup XP per dictionary
         * level. Lives next to the existing `category_progress` rows
         * (verbs regular/irregular, nouns, …) so the player can see
         * their progress in the same UI without a new table.
         */
        const val CATEGORY_KEY: String = "LETTER_SOUP"

        /**
         * Location-hint uses granted at the start of a world-mode
         * run. The dedicated inventory system is out of scope for the
         * beta, so the value is hard-coded as the default cap.
         */
        const val INITIAL_LOCATION_HINTS: Int = 2

        /**
         * English-hint uses granted at the start of a world-mode
         * run. Same hard-coded rationale as
         * [INITIAL_LOCATION_HINTS].
         */
        const val INITIAL_ENGLISH_HINTS: Int = 2

        /** Total time budget for a world-mode run, in milliseconds. */
        const val WORLD_GAME_TIME_MS: Long = 300_000L

        /** Cadence of the world-mode timer tick. */
        const val TIMER_TICK_MS: Long = 100L
    }
}

/**
 * Game-mode flag for a Letter Soup run.
 *
 * Kept as an enum so future iterations can add new modes (e.g. a
 * timed-challenge variant) without breaking the call sites.
 */
enum class HintMode { NORMAL, WORLD }