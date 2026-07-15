package com.example.englishvault.ui.games.lettersoup.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.englishvault.audio.SoundEffectPlayer
import com.example.englishvault.audio.SoundKey
import com.example.englishvault.ui.games.lettersoup.model.HintMode
import com.example.englishvault.ui.games.lettersoup.model.LetterSoupBoard
import com.example.englishvault.ui.games.lettersoup.model.LetterSoupGameState
import com.example.englishvault.ui.games.lettersoup.model.LetterSoupGameState.Companion.CATEGORY_KEY
import com.example.englishvault.ui.games.lettersoup.model.LetterSoupGameState.Companion.INITIAL_ENGLISH_HINTS
import com.example.englishvault.ui.games.lettersoup.model.LetterSoupGameState.Companion.INITIAL_LOCATION_HINTS
import com.example.englishvault.ui.games.lettersoup.model.LetterSoupGameState.Companion.MAX_MOVES
import com.example.englishvault.ui.games.lettersoup.model.LetterSoupGameState.Companion.TIMER_TICK_MS
import com.example.englishvault.ui.games.lettersoup.model.LetterSoupGameState.Companion.WORLD_GAME_TIME_MS
import com.example.englishvault.ui.games.lettersoup.model.LetterSoupWord
import com.example.englishvault.ui.games.lettersoup.util.BoardGenerator
import com.example.englishvault.ui.words.WordTypeFilter
import data.database.dao.CategoryProgressDao
import data.database.dao.SkillProgressDao
import data.database.dao.UserProfileDao
import data.database.dao.WordDao
import data.database.entities.CategoryProgressEntity
import data.database.entities.Skill
import data.database.entities.UserProfileEntity
import data.database.entities.WordEntity
import data.game.CategoryGating
import data.game.PromotionEvent
import data.game.PromotionGate
import data.game.PromotionNotifier
import data.game.PromotionOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the Letter Soup mini-game.
 *
 * ## Modes (Phase 7.4)
 *
 * Two modes are supported:
 *  - [HintMode.NORMAL]: unlimited hints, no time limit. The
 *    behaviour from earlier phases.
 *  - [HintMode.WORLD]: a 5-minute countdown, two location hints and
 *    two English-translation hints. The dedicated inventory system
 *    is out of scope for the beta, so the inventories are
 *    hard-coded caps surfaced through the [locationHintsRemaining]
 *    and [englishHintsRemaining] state fields.
 *
 * The dev toggle on the play screen flips [currentHintMode] and
 * immediately restarts the run at the same level so the new mode
 * is applied without navigating away.
 *
 * ## Level filter
 *
 * Every word that ends up on the board originates from
 * `wordDao.getCoreWordsByLengthAndLevel(level, MIN, MAX)` — the DAO
 * query already constrains rows by `level = :level`, so the pool,
 * the placements, the translations list and the hint targets all
 * stay inside the chosen level. No other code path introduces
 * words from other levels into the run.
 */
@HiltViewModel
class LetterSoupViewModel @Inject constructor(
    private val wordDao: WordDao,
    private val categoryProgressDao: CategoryProgressDao,
    private val userProfileDao: UserProfileDao,
    private val skillProgressDao: SkillProgressDao,
    private val soundEffectPlayer: SoundEffectPlayer,
    private val promotionNotifier: PromotionNotifier
) : ViewModel() {

    /**
     * Live effects volume in `[0.0, 1.0]`. Sharing is `Eagerly` so
     * `.value` reflects the slider even when the screen has no UI
     * subscribers.
     */
    private val effectsVolume: StateFlow<Float> = userProfileDao.observeProfile()
        .map { profile -> profile?.effectsVolume ?: UserProfileEntity.DEFAULT_VOLUME }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = UserProfileEntity.DEFAULT_VOLUME
        )

    /**
     * Currently selected mode. The dev toggle button on the play
     * screen flips this between [HintMode.NORMAL] and
     * [HintMode.WORLD]; [startGame] freezes the chosen value into the
     * per-run [LetterSoupGameState.InProgress.mode] field.
     */
    private val _currentHintMode = MutableStateFlow(HintMode.NORMAL)
    val currentHintMode: StateFlow<HintMode> = _currentHintMode.asStateFlow()

    /**
     * Level the active run is playing at. Stored so [toggleHintMode]
     * can restart the run in the new mode without the screen having
     * to push the level back through `startGame`.
     */
    private var currentLevel: Int = 1

    /** Active world-mode countdown coroutine, or `null` if none. */
    private var timerJob: Job? = null

    /**
     * Lookup from the dictionary's original word (upper-cased) to its
     * [WordEntity], populated once at game start. The keys here match
     * the placements on the board 1-to-1 — both come from the same
     * level-filtered query.
     */
    private var wordLookup: Map<String, WordEntity> = emptyMap()

    private val _gameState = MutableStateFlow<LetterSoupGameState>(LetterSoupGameState.Loading)
    val gameState: StateFlow<LetterSoupGameState> = _gameState.asStateFlow()

    /**
     * Highest level that has any core words of an eligible length
     * (currently 3..10 characters). Drives the number of cards the
     * level selector renders.
     */
    suspend fun maxLetterSoupLevel(): Int =
        wordDao.maxCoreLevelByLength(MIN_WORD_LENGTH, MAX_WORD_LENGTH).coerceAtLeast(1)

    /**
     * Number of words eligible for play at [level] — the count shown
     * on each level card.
     */
    suspend fun wordsAtLevel(level: Int): Int =
        wordDao.getCoreWordsByLengthAndLevel(level, MIN_WORD_LENGTH, MAX_WORD_LENGTH).size

    /**
     * Highest Letter Soup level the player can currently access.
     *
     * Reads the `LETTER_SOUP` row from `category_progress` and returns
     * its `unlockedLevel`. Falls back to `1` for fresh installs where
     * the row has not been seeded yet.
     */
    suspend fun maxUnlockedLetterSoupLevel(): Int {
        categoryProgressDao.seedIfMissing(CATEGORY_KEY)
        return categoryProgressDao.get(CATEGORY_KEY)?.unlockedLevel ?: 1
    }

    /**
     * Flips the dev toggle between [HintMode.NORMAL] and
     * [HintMode.WORLD] **and immediately restarts the active run** so
     * the new mode is applied without navigating away and back. The
     * previously running game (questions, score, hints, timer) is
     * discarded and reseeded for the chosen mode.
     */
    fun toggleHintMode() {
        _currentHintMode.value = when (_currentHintMode.value) {
            HintMode.NORMAL -> HintMode.WORLD
            HintMode.WORLD -> HintMode.NORMAL
        }
        // Apply the new mode to the current level right away. If no
        // run has been seeded yet, `currentLevel` is still its
        // default (`1`); tapping the toggle before startGame just
        // flips the flag for the upcoming run.
        cancelWorldTimer()
        startGame(currentLevel)
    }

    /**
     * Loads a fresh board from the dictionary pool at [level] and
     * resets the run counters. Seeds mode-specific fields based on
     * [currentHintMode]:
     *  - WORLD runs receive [INITIAL_LOCATION_HINTS] /
     *    [INITIAL_ENGLISH_HINTS] hint uses and start the
     *    [WORLD_GAME_TIME_MS] countdown.
     *  - NORMAL runs leave the hint counters and timer at their
     *    `0` defaults — hints are unlimited and the timer never
     *    ticks.
     *
     * **Level filter guarantee**: every word that ends up on the
     * board comes from
     * `wordDao.getCoreWordsByLengthAndLevel(level, MIN, MAX)`. The
     * DAO query constrains rows by `level = :level`, so the
     * translations list, the word-lookup and the placements stay
     * inside the chosen level even after swaps or hint reveals.
     */
    fun startGame(level: Int) {
        currentLevel = level
        cancelWorldTimer()
        viewModelScope.launch {
            _gameState.value = LetterSoupGameState.Loading
            val entities = wordDao.getCoreWordsByLengthAndLevel(
                level = level,
                min = MIN_WORD_LENGTH,
                max = MAX_WORD_LENGTH
            )
            wordLookup = entities.associateBy { it.word.uppercase() }

            val pool = entities.map { it.word.uppercase() }
            val translations = entities.associate { it.word.uppercase() to it.translation }

            val board = BoardGenerator.generate(pool, translations)
            if (board == null) {
                _gameState.value = LetterSoupGameState.Loading
                return@launch
            }
            val mode = _currentHintMode.value
            _gameState.value = LetterSoupGameState.InProgress(
                level = level,
                board = board,
                movesLeft = MAX_MOVES,
                wordsFixed = 0,
                wordsToWin = board.placements.size,
                xpByCategory = emptyMap(),
                mode = mode,
                locationHintsRemaining = if (mode == HintMode.WORLD) INITIAL_LOCATION_HINTS else 0,
                englishHintsRemaining = if (mode == HintMode.WORLD) INITIAL_ENGLISH_HINTS else 0,
                timeRemainingMs = if (mode == HintMode.WORLD) WORLD_GAME_TIME_MS else 0L
            )
            if (mode == HintMode.WORLD) {
                startWorldTimer()
            }
        }
    }

    /**
     * Handles a tap on the cell at ([row], [col]). Three branches:
     *  - Nothing selected → mark the cell as selected.
     *  - Tapped the already-selected cell → deselect (free move).
     *  - Tapped a different cell → try the swap.
     */
    fun tapCell(row: Int, col: Int) {
        val state = _gameState.value as? LetterSoupGameState.InProgress ?: return
        val role = state.board.roleAt(row, col)
        if (role == com.example.englishvault.ui.games.lettersoup.model.LetterSoupCell.WordFixed) {
            return
        }
        when (state.selectedCell) {
            null -> {
                _gameState.value = state.copy(selectedCell = row to col)
            }
            (row to col) -> {
                _gameState.value = state.copy(selectedCell = null)
            }
            else -> attemptSwap(state, row to col)
        }
    }

    /**
     * Resolves a swap between the previously-selected cell and
     * [target]. If the swap converts any placement into its correct
     * form, the placement is flagged `fixed` and XP is awarded.
     * Otherwise the move budget is decremented and the swap pair is
     * returned to the UI for a brief red flash.
     */
    private fun attemptSwap(
        state: LetterSoupGameState.InProgress,
        target: Pair<Int, Int>
    ) {
        val source = state.selectedCell ?: return
        val cells = state.board.cells.map { it.toMutableList() }.toMutableList()
        val (r1, c1) = source
        val (r2, c2) = target
        val tmp = cells[r1][c1]
        cells[r1][c1] = cells[r2][c2]
        cells[r2][c2] = tmp

        val swapped = LetterSoupBoard(
            cells = cells,
            placements = state.board.placements,
            boardSize = state.board.boardSize
        )
        val fixedNow = swapped.placements
            .filter { !it.fixed }
            .filter { word -> isCorrect(word, swapped) }

        if (fixedNow.isEmpty()) {
            _gameState.value = state.copy(
                selectedCell = null,
                movesLeft = (state.movesLeft - 1).coerceAtLeast(0),
                lastSwapFailedCells = listOf(source, target),
                lastFixedWord = null,
                isLocationHintRevealed = false,
                isEnglishHintRevealed = false
            )
            return
        }

        // Success path.
        val updatedPlacements = swapped.placements.map { word ->
            if (fixedNow.any { it === word }) word.copy(fixed = true) else word
        }
        val boardWithFixedWords = LetterSoupBoard(
            cells = cells,
            placements = updatedPlacements,
            boardSize = state.board.boardSize
        )
        val xpByCategory = state.xpByCategory.toMutableMap()
        fixedNow.forEach { word ->
            // Credit BOTH the grammatical category of the fixed word
            // (drives the per-category progress bar on Progress) AND
            // the single [CATEGORY_KEY] bucket (drives the Letter
            // Soup level unlock gating). The category is resolved
            // through [wordLookup] which maps the upper-cased word
            // text back to its dictionary [WordEntity].
            val entity = wordLookup[word.original]
            val grammaticalKey = entity?.let { classifyWordSafely(it).name }
            if (grammaticalKey != null) {
                xpByCategory[grammaticalKey] =
                    (xpByCategory[grammaticalKey] ?: 0) +
                        CategoryGating.XP_PER_CORRECT_ANSWER
            }
            xpByCategory[CATEGORY_KEY] =
                (xpByCategory[CATEGORY_KEY] ?: 0) +
                    CategoryGating.XP_PER_CORRECT_ANSWER
        }

        val newWordsFixed = state.wordsFixed + fixedNow.size
        val lastFixed = fixedNow.firstOrNull()
        val isWin = newWordsFixed >= state.wordsToWin
        if (isWin) {
            _gameState.value = state.copy(
                board = boardWithFixedWords,
                wordsFixed = newWordsFixed,
                selectedCell = null,
                xpByCategory = xpByCategory,
                lastFixedWord = lastFixed,
                lastSwapFailedCells = emptyList(),
                isLocationHintRevealed = false,
                isEnglishHintRevealed = false
            )
            viewModelScope.launch {
                soundEffectPlayer.play(SoundKey.Correct, effectsVolume.value)
                grantXpAndFinish(
                    level = state.level,
                    won = true,
                    wordsFixed = newWordsFixed,
                    wordsToWin = state.wordsToWin,
                    xpByCategory = xpByCategory,
                    mode = state.mode,
                    timedOut = false
                )
            }
            return
        }

        _gameState.value = state.copy(
            board = boardWithFixedWords,
            wordsFixed = newWordsFixed,
            selectedCell = null,
            xpByCategory = xpByCategory,
            lastFixedWord = lastFixed,
            lastSwapFailedCells = emptyList(),
            isLocationHintRevealed = false,
            isEnglishHintRevealed = false
        )

        viewModelScope.launch {
            soundEffectPlayer.play(SoundKey.Correct, effectsVolume.value)
        }
    }

    /**
     * Reveals the wrong-letter cell on the active placement — the UI
     * marks it with the ❌ badge + pulse for
     * [LetterSoupGameState.HINT_TIMEOUT_MS].
     *
     * In [HintMode.WORLD] the reveal consumes one
     * [LetterSoupGameState.INITIAL_LOCATION_HINTS] use; the call is
     * a no-op when the inventory is empty.
     */
    fun revealLocationHint() {
        val state = _gameState.value as? LetterSoupGameState.InProgress ?: return
        if (state.board.firstActiveWithWrong() == null) return
        if (state.mode == HintMode.WORLD && state.locationHintsRemaining <= 0) return
        val newRemaining = if (state.mode == HintMode.WORLD) {
            (state.locationHintsRemaining - 1).coerceAtLeast(0)
        } else {
            state.locationHintsRemaining
        }
        _gameState.value = state.copy(
            isLocationHintRevealed = true,
            locationHintsRemaining = newRemaining
        )
    }

    /**
     * Reveals the English word for the active placement inline in the
     * always-visible translations list for
     * [LetterSoupGameState.HINT_TIMEOUT_MS].
     *
     * In [HintMode.WORLD] the reveal consumes one
     * [LetterSoupGameState.INITIAL_ENGLISH_HINTS] use; the call is
     * a no-op when the inventory is empty.
     */
    fun revealEnglishHint() {
        val state = _gameState.value as? LetterSoupGameState.InProgress ?: return
        if (state.board.firstActiveWithWrong() == null) return
        if (state.mode == HintMode.WORLD && state.englishHintsRemaining <= 0) return
        val newRemaining = if (state.mode == HintMode.WORLD) {
            (state.englishHintsRemaining - 1).coerceAtLeast(0)
        } else {
            state.englishHintsRemaining
        }
        _gameState.value = state.copy(
            isEnglishHintRevealed = true,
            englishHintsRemaining = newRemaining
        )
    }

    /**
     * Called by the UI after the brief "fixed word" or "failed swap"
     * animation has been on screen long enough to read. Clears the
     * transient flags and — when the move budget has been exhausted —
     * transitions to [LetterSoupGameState.Finished] with `won = false`.
     */
    fun acknowledgeAnimation() {
        val state = _gameState.value as? LetterSoupGameState.InProgress ?: return
        if (state.lastSwapFailedCells.isEmpty() && state.lastFixedWord == null) return
        if (state.lastSwapFailedCells.isNotEmpty() && state.movesLeft <= 0) {
            viewModelScope.launch {
                grantXpAndFinish(
                    level = state.level,
                    won = false,
                    wordsFixed = state.wordsFixed,
                    wordsToWin = state.wordsToWin,
                    xpByCategory = state.xpByCategory,
                    mode = state.mode,
                    timedOut = false
                )
            }
            return
        }
        _gameState.value = state.copy(
            lastSwapFailedCells = emptyList(),
            lastFixedWord = null
        )
    }

    /** Called by the UI when the location hint timeout expires. */
    fun acknowledgeLocationHint() {
        val state = _gameState.value as? LetterSoupGameState.InProgress ?: return
        if (!state.isLocationHintRevealed) return
        _gameState.value = state.copy(isLocationHintRevealed = false)
    }

    /** Called by the UI when the English hint timeout expires. */
    fun acknowledgeEnglishHint() {
        val state = _gameState.value as? LetterSoupGameState.InProgress ?: return
        if (!state.isEnglishHintRevealed) return
        _gameState.value = state.copy(isEnglishHintRevealed = false)
    }

    /**
     * Persists the run's per-category XP via the same DAO call the
     * Word Match Verbs VM uses, then transitions to the finished
     * state. Letter Soup has no per-word "learned" status, so it
     * satisfies the XP-only rule (no learned-percentage
     * requirement).
     */
    private suspend fun grantXpAndFinish(
        level: Int,
        won: Boolean,
        wordsFixed: Int,
        wordsToWin: Int,
        xpByCategory: Map<String, Int>,
        mode: HintMode,
        timedOut: Boolean
    ) {
        grantPerCategoryXp(xpByCategory)
        grantSkillXp(xpByCategory)
        cancelWorldTimer()
        _gameState.value = LetterSoupGameState.Finished(
            level = level,
            won = won,
            wordsFixed = wordsFixed,
            wordsToWin = wordsToWin,
            xpByCategory = xpByCategory,
            mode = mode,
            timedOut = timedOut
        )
    }

    /**
     * Called by the world-mode timer coroutine when the countdown
     * reaches zero. Cancels the timer and transitions to
     * [LetterSoupGameState.Finished] with `won = false` and
     * `timedOut = true` so the end screen can render a "Time's up!"
     * headline.
     */
    private fun onTimeExpired() {
        val state = _gameState.value as? LetterSoupGameState.InProgress ?: return
        cancelWorldTimer()
        viewModelScope.launch {
            grantXpAndFinish(
                level = state.level,
                won = false,
                wordsFixed = state.wordsFixed,
                wordsToWin = state.wordsToWin,
                xpByCategory = state.xpByCategory,
                mode = state.mode,
                timedOut = true
            )
        }
    }

    /**
     * Launches a coroutine that ticks [timeRemainingMs] down by
     * [TIMER_TICK_MS] every iteration. Exits when the countdown
     * reaches zero (calling [onTimeExpired]) or when the run
     * transitions out of [LetterSoupGameState.InProgress].
     *
     * Cancels any previous timer first so at most one tick coroutine
     * is alive at a time.
     */
    private fun startWorldTimer() {
        cancelWorldTimer()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(TIMER_TICK_MS)
                val current = _gameState.value as? LetterSoupGameState.InProgress ?: return@launch
                if (current.mode != HintMode.WORLD) return@launch
                val next = (current.timeRemainingMs - TIMER_TICK_MS).coerceAtLeast(0L)
                _gameState.value = current.copy(timeRemainingMs = next)
                if (next <= 0L) {
                    onTimeExpired()
                    return@launch
                }
            }
        }
    }

    /** Cancels the active world-mode timer coroutine if there is one. */
    private fun cancelWorldTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    /**
     * Iterates [xpByCategory] and grants each non-zero bucket to
     * the matching row in `category_progress`. Two kinds of buckets
     * exist in the map:
     *
     *  - **Grammatical buckets** keyed by `WordTypeFilter.name`
     *    (e.g. `"VERBS_REGULAR"`, `"NOUNS"`) — each grants XP to
     *    its dedicated `category_progress` row so the per-category
     *    progress bar on the Progress screen fills up.
     *  - **[CATEGORY_KEY] bucket** keyed by `"LETTER_SOUP"` — a
     *    single synthetic row that drives the Letter Soup level
     *    unlock gating via [maxUnlockedLetterSoupLevel]. Letter
     *    Soup is a game (no per-word "learned" status), so it
     *    satisfies the XP-only rule (no learned-percentage
     *    requirement).
     *
     * Unknown buckets are skipped defensively so a future category
     * change cannot crash the grant.
     */
    private suspend fun grantPerCategoryXp(xpByCategory: Map<String, Int>) {
        if (xpByCategory.isEmpty()) return
        for ((categoryKey, xp) in xpByCategory) {
            if (xp <= 0) continue
            if (categoryKey == CATEGORY_KEY) {
                // Synthetic Letter Soup bucket keeps its own
                // single-gate (XP only) progression; the hybrid gate
                // does not apply here because the bucket is not
                // tied to the per-word learned percentage.
                grantLetterSoupLevelXp(xp)
                continue
            }
            val outcome = PromotionGate.evaluate(
                categoryKey = categoryKey,
                amount = xp,
                wordDao = wordDao,
                categoryProgressDao = categoryProgressDao
            )
            if (outcome is PromotionOutcome.Promoted) {
                promotionNotifier.emit(
                    PromotionEvent(
                        categoryKey = categoryKey,
                        previousLevel = outcome.previousLevel,
                        newLevel = outcome.newLevel
                    )
                )
            }
        }
    }

    /**
     * Promotes the single [CATEGORY_KEY] Letter Soup bucket when
     * the player has earned at least [CategoryGating.XP_MIN_PER_LEVEL]
     * XP at the current level. Caps the new unlocked level at the
     * highest dictionary level available for the mini-game (see
     * [maxLetterSoupLevel]).
     */
    private suspend fun grantLetterSoupLevelXp(xpToGrant: Int) {
        if (xpToGrant <= 0) return
        val maxLevel = maxLetterSoupLevel()
        categoryProgressDao.seedIfMissing(CATEGORY_KEY)
        val progress = categoryProgressDao.get(CATEGORY_KEY)
            ?: CategoryProgressEntity.initial(CATEGORY_KEY)

        val currentLevel = progress.unlockedLevel.coerceAtMost(maxLevel)
        val nextLevel = (currentLevel + 1).coerceAtMost(maxLevel)
        val newXpSince = progress.xpSinceLevelUp + xpToGrant
        val shouldUnlock = newXpSince >= CategoryGating.XP_MIN_PER_LEVEL &&
            nextLevel > currentLevel

        categoryProgressDao.grantXpAndMaybeUnlock(
            categoryKey = CATEGORY_KEY,
            amount = xpToGrant,
            meetsXp = shouldUnlock,
            meetsLearnedPct = true,
            targetUnlockedLevel = if (shouldUnlock) nextLevel else currentLevel
        )
    }

    /**
     * Atomically credits the run's total XP to the [Skill.READING]
     * row in `skill_progress`. Letter Soup is a reading activity
     * — the player reads letters on the board and unscrambles them
     * into the target word — so the entire run counts toward
     * READING.
     *
     * No-op when the run earned zero XP.
     */
    private suspend fun grantSkillXp(xpByCategory: Map<String, Int>) {
        val totalXp = xpByCategory.values.sum()
        if (totalXp <= 0) return
        skillProgressDao.grantXp(Skill.READING.key, totalXp)
    }

    /**
     * Maps a [WordEntity] to its [WordTypeFilter] bucket. Mirrors
     * the helper used by `WordMatchVerbsViewModel` and
     * `ListeningViewModel` so all mini-games credit the same
     * category row for the same word.
     */
    private fun classifyWordSafely(word: WordEntity): WordTypeFilter {
        val tracked = WordTypeFilter.TRACKED.firstOrNull { it.matches(word) }
        if (tracked != null) return tracked
        return if (word.type == "verb") {
            if (word.regular == false) WordTypeFilter.VERBS_IRREGULAR
            else WordTypeFilter.VERBS_REGULAR
        } else {
            WordTypeFilter.ADJECTIVES
        }
    }

    /**
     * `true` when every cell the [word] occupies currently holds its
     * expected letter from the dictionary form.
     */
    private fun isCorrect(word: LetterSoupWord, board: LetterSoupBoard): Boolean {
        val cells = word.cells()
        for (i in cells.indices) {
            val (r, c) = cells[i]
            if (board[r, c] != word.original[i]) return false
        }
        return true
    }

    override fun onCleared() {
        super.onCleared()
        cancelWorldTimer()
    }

    private companion object {
        /** Shortest word the board can comfortably host. */
        const val MIN_WORD_LENGTH: Int = 3

        /**
         * Longest word we are willing to host. With a 10×10 extended
         * board any word up to 10 characters fits horizontally or
         * vertically. Longer words are silently dropped by the DAO
         * length filter.
         */
        const val MAX_WORD_LENGTH: Int = 10
    }
}