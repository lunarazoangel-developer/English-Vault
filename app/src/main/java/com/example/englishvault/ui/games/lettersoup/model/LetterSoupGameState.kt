package com.example.englishvault.ui.games.lettersoup.model

/**
 * State machine for a Letter Soup run.
 *
 *  - [Loading] — the dictionary is being queried and the board is
 *    being generated.
 *  - [InProgress] — the player is underlining letters on the board.
 *    [selectedCells] is the chain currently in progress (empty when
 *    the player has not started underlining). [wordsFixed] is the
 *    running count of placements the player has found.
 *    [wordsToWin] mirrors the number of placements generated on
 *    this board (typically 5) and drives the end-of-run check.
 *  - [Finished] — every placement on this board has been found. The
 *    board never resets mid-run, so reaching `wordsFixed ==
 *    wordsToWin` ends the game. World-mode runs can also finish
 *    early when [timeRemainingMs] hits zero (see [timedOut]).
 *
 * ## Modes (Phase 7.4)
 *
 * The mini-game supports two modes:
 *  - [HintMode.NORMAL]: unlimited hints, no time limit. The classic
 *    word-search experience: the player takes as long as they need
 *    to find every word.
 *  - [HintMode.WORLD]: a 5-minute countdown, two location hints and
 *    two English-translation hints. The dedicated inventory system
 *    is out of scope for the beta, so the inventories are
 *    hard-coded caps surfaced through the [locationHintsRemaining]
 *    and [englishHintsRemaining] state fields.
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
        val wordsFixed: Int,
        /**
         * Total placements generated on this board. Becomes the win
         * threshold. Typically `5` but may be lower when the generator
         * could not fit the requested count.
         */
        val wordsToWin: Int,
        /**
         * Cells the player has underlined so far, in order. Empty
         * when nothing is being underlined. The chain is built by
         * tapping adjacent cells (king-move) and committed by
         * tapping the first cell of the chain again.
         */
        val selectedCells: List<Pair<Int, Int>> = emptyList(),
        /**
         * Run-level XP earned so far, keyed by the same buckets the
         * Word Match Verbs VM uses (`WordTypeFilter.name` and the
         * synthetic `LETTER_SOUP` row). Persisted at end-of-run.
         */
        val xpByCategory: Map<String, Int> = emptyMap(),
        /**
         * Cells that should flash red because the player just
         * committed a selection that did not match any unfound
         * word. Auto-cleared by the UI after
         * [WRONG_FLASH_TIMEOUT_MS].
         */
        val wrongFlashCells: List<Pair<Int, Int>> = emptyList(),
        /**
         * The placement the player has just found, if any. Drives
         * the brief green flash on its cells before the selection
         * resets. Auto-cleared by the UI after
         * [WRONG_FLASH_TIMEOUT_MS] (re-using the same animation
         * budget).
         */
        val lastFoundWord: LetterSoupWord? = null,
        /**
         * The placement currently highlighted by the location hint,
         * if any. All of its cells stay tinted for
         * [HINT_TIMEOUT_MS] so the player can see the trajectory of
         * the word on the board. `null` when no hint is active.
         */
        val highlightedPlacement: LetterSoupWord? = null,
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
         * player found every placement. Lets the end screen render
         * a "Time's up!" headline.
         */
        val timedOut: Boolean = false
    ) : LetterSoupGameState() {

        /**
         * The placement the English hint should currently reveal.
         * `null` when there is no unfound placement to point at.
         */
        val firstUnfixed: LetterSoupWord?
            get() = board.firstUnfixedPlacement()
    }

    data class Finished(
        val level: Int,
        val won: Boolean,
        val wordsFixed: Int,
        val wordsToWin: Int,
        val xpByCategory: Map<String, Int>,
        val mode: HintMode = HintMode.NORMAL,
        /**
         * `true` when the world-mode countdown ran out before the
         * player found every placement.
         */
        val timedOut: Boolean = false
    ) : LetterSoupGameState()

    companion object {
        /**
         * Target number of placements per board. The generator tries
         * to reach this count; if it cannot, [wordsToWin] reflects
         * the actual placements.
         */
        const val TARGET_PLACEMENTS: Int = 5

        /**
         * Duration of the transient red / green flash after the
         * player commits a selection (whether right or wrong).
         * Short enough that the player can start a new chain
         * quickly even on a 12×12 board, long enough for the
         * eye to register the feedback.
         */
        const val WRONG_FLASH_TIMEOUT_MS: Long = 350

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
