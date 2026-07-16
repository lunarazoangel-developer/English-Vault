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
import data.game.AutoStatusEvaluator
import data.game.CategoryGating
import data.game.PromotionEvent
import data.game.PromotionGate
import data.game.PromotionNotifier
import data.game.PromotionOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.max
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
 *  - [HintMode.NORMAL]: unlimited hints, no time limit. The classic
 *    word-search experience: the player takes as long as they need
 *    to find every word.
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
 * ## Mechanic (Phase 8.x — word search rewrite)
 *
 * The mini-game is a classic word search: the player underlines
 * letters by tapping cells, builds a chain of king-move adjacent
 * cells, and commits the selection by tapping the first cell of the
 * chain again. A successful commit fixes the placement; a failed
 * commit flashes the cells red briefly. There is no move budget.
 *
 * ## Level filter & word pool
 *
 * Every word that ends up on the board originates from
 * `wordDao.getCoreWordsByLengthAndLevel(level, MIN, MAX)`. The DAO
 * query already constrains rows by `level = :level`. The VM then
 * drops every row whose `type` is `verb` so the pool, the
 * placements, the translations list and the hint targets stay
 * inside the chosen level and **never include verbs** (regular or
 * irregular). No other code path introduces words from other
 * categories into the run.
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
     * Highest level that has any non-verb core words of an eligible
     * length (currently 3..12 characters). Drives the number of
     * cards the level selector renders.
     */
    suspend fun maxLetterSoupLevel(): Int =
        wordDao.maxCoreLevelByLength(MIN_WORD_LENGTH, MAX_WORD_LENGTH)
            .let { raw -> raw.coerceAtLeast(1) }

    /**
     * Number of words eligible for play at [level] — the count shown
     * on each level card. Counts **only non-verb** rows so the level
     * selector never offers a level that cannot host a board.
     */
    suspend fun wordsAtLevel(level: Int): Int =
        wordDao.getCoreWordsByLengthAndLevel(level, MIN_WORD_LENGTH, MAX_WORD_LENGTH)
            .count { it.type != VERB_TYPE }

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
     * **Verb exclusion guarantee**: every word that ends up on the
     * board comes from
     * `wordDao.getCoreWordsByLengthAndLevel(level, MIN, MAX)` and
     * additionally passes an in-memory `type != "verb"` filter. The
     * translations list, the word-lookup and the placements stay
     * inside the chosen level and inside the non-verb category set
     * even after hint reveals.
     */
    fun startGame(level: Int) {
        currentLevel = level
        cancelWorldTimer()
        viewModelScope.launch {
            _gameState.value = LetterSoupGameState.Loading
            val raw = wordDao.getCoreWordsByLengthAndLevel(
                level = level,
                min = MIN_WORD_LENGTH,
                max = MAX_WORD_LENGTH
            )
            val entities = raw.filter { it.type != VERB_TYPE }

            wordLookup = entities.associateBy { it.word.uppercase() }
            val pool = entities.map { it.word.uppercase() }
            val translations = entities.associate { it.word.uppercase() to it.translation }
            val wordIds = entities.associate { it.word.uppercase() to it.id }

            val board = BoardGenerator.generate(
                pool = pool,
                translations = translations,
                wordIds = wordIds
            )
            if (board == null) {
                _gameState.value = LetterSoupGameState.Loading
                return@launch
            }
            val mode = _currentHintMode.value
            _gameState.value = LetterSoupGameState.InProgress(
                level = level,
                board = board,
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
     * Handles a tap on the cell at ([row], [col]) following the
     * word-search input model:
     *
     *  1. **No active selection** → start a new chain with this
     *     cell. Any pending flash is cleared so the visual
     *     feedback of the previous commit does not linger.
     *  2. **The cell is the LAST one in the current selection
     *     (with 2+ cells)** → commit. This is the primary commit
     *     gesture: the player underlines from start to end, then
     *     taps the end of the chain to submit. It is the most
     *     intuitive model and the one most classic word searches
     *     use.
     *  3. **The cell is the FIRST one in the current selection
     *     (with 2+ cells)** → also commit. Kept as an alternative
     *     so the player has two ways to submit, whichever they
     *     reach for first.
     *  4. **The cell is already in the current selection (middle
     *     of the chain)** → truncate the chain to that cell,
     *     acting as a backspace. Useful when the player
     *     mis-underlined a letter and wants to back up.
     *  5. **The cell is king-move adjacent to the last cell in the
     *     current selection** → extend the chain by one.
     *  6. **Anything else** → ignore the tap. The player has to
     *     commit or backspace before starting a new chain from a
     *     non-adjacent cell.
     *
     * Note: taps are **never blocked** by the wrong / found flash.
     * The flash is purely a transient visual cue; the player can
     * start a new selection immediately after a commit without
     * waiting for the flash to clear. This is what makes the
     * interaction feel responsive on a 12×12 board where the cells
     * are small.
     */
    fun onCellTapped(row: Int, col: Int) {
        val state = _gameState.value as? LetterSoupGameState.InProgress ?: return
        val tapped = row to col
        val current = state.selectedCells

        if (current.isEmpty()) {
            // Starting a new selection always clears the flash so
            // the previous commit's red / green tint does not bleed
            // into the new chain.
            _gameState.value = state.copy(
                selectedCells = listOf(tapped),
                wrongFlashCells = emptyList(),
                lastFoundWord = null
            )
            return
        }
        if (current.size >= 2) {
            if (current.last() == tapped || current.first() == tapped) {
                commitSelection(state)
                return
            }
        }
        val existingIndex = current.indexOf(tapped)
        if (existingIndex >= 0) {
            _gameState.value = state.copy(selectedCells = current.take(existingIndex + 1))
            return
        }
        val last = current.last()
        if (isKingMoveAdjacent(last, tapped)) {
            _gameState.value = state.copy(selectedCells = current + tapped)
        }
        // else: ignore
    }

    /**
     * `true` when [a] and [b] are king-move adjacent (8 directions,
     * each axis at most 1 step away). This is the rule every
     * classic word-search follows for extending a chain.
     */
    private fun isKingMoveAdjacent(a: Pair<Int, Int>, b: Pair<Int, Int>): Boolean {
        val dr = kotlin.math.abs(a.first - b.first)
        val dc = kotlin.math.abs(a.second - b.second)
        return max(dr, dc) == 1
    }

    /**
     * Reads the current selection in order and looks for an unfound
     * placement whose `original` matches the read string or its
     * reverse. On a hit, marks the placement `fixed`, grants XP and
     * plays the correct SFX. On a miss, flashes the selected cells
     * red briefly.
     */
    private fun commitSelection(state: LetterSoupGameState.InProgress) {
        val current = state.selectedCells
        if (current.isEmpty()) return
        val candidate = current.joinToString("") { (r, c) -> state.board[r, c].toString() }
        val reversed = candidate.reversed()
        val match = state.board.placements
            .firstOrNull { !it.fixed && (it.original.equals(candidate, ignoreCase = true) ||
                it.original.equals(reversed, ignoreCase = true)) }

        if (match != null) {
            val updatedPlacements = state.board.placements.map { word ->
                if (word === match) word.copy(fixed = true) else word
            }
            val newBoard = state.board.copy(placements = updatedPlacements)
            val entity = wordLookup[match.original]
            val grammaticalKey = entity?.let { classifyWordSafely(it).name }
            val xpByCategory = state.xpByCategory.toMutableMap()
            if (grammaticalKey != null) {
                xpByCategory[grammaticalKey] =
                    (xpByCategory[grammaticalKey] ?: 0) +
                        CategoryGating.XP_PER_CORRECT_ANSWER
            }
            xpByCategory[CATEGORY_KEY] =
                (xpByCategory[CATEGORY_KEY] ?: 0) +
                    CategoryGating.XP_PER_CORRECT_ANSWER

            val newWordsFixed = state.wordsFixed + 1
            val isWin = newWordsFixed >= state.wordsToWin
            _gameState.value = state.copy(
                board = newBoard,
                wordsFixed = newWordsFixed,
                selectedCells = emptyList(),
                xpByCategory = xpByCategory,
                lastFoundWord = match,
                wrongFlashCells = emptyList(),
                highlightedPlacement = null,
                isEnglishHintRevealed = false
            )
            if (isWin) {
                viewModelScope.launch {
                    soundEffectPlayer.play(SoundKey.Correct, effectsVolume.value)
                    applyAutoStatus(listOf(match))
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
            } else {
                viewModelScope.launch {
                    soundEffectPlayer.play(SoundKey.Correct, effectsVolume.value)
                    applyAutoStatus(listOf(match))
                }
            }
            return
        }

        _gameState.value = state.copy(
            wrongFlashCells = current,
            selectedCells = emptyList(),
            lastFoundWord = null
        )
    }

    /**
     * Phase 7.15 — auto-marking fan-out for the placements the
     * player has just found. Bumps
     * [WordEntity.consecutiveCorrect] on each underlying row and
     * re-evaluates its [LearningStatus] via [AutoStatusEvaluator].
     *
     * Placements with `wordId <= 0` (defensive fallback when the
     * board generator could not resolve a dictionary id) are
     * silently skipped.
     *
     * Errors are swallowed: a failed write should not crash the
     * game flow.
     */
    private fun applyAutoStatus(foundPlacements: List<LetterSoupWord>) {
        viewModelScope.launch {
            runCatching {
                val now = System.currentTimeMillis()
                for (placement in foundPlacements) {
                    val wordId = placement.wordId
                    if (wordId <= 0) continue
                    val previous = wordDao.getWordById(wordId) ?: continue
                    val newCount = previous.consecutiveCorrect + 1
                    wordDao.setConsecutiveCorrect(wordId, newCount, now)
                    val promoted = AutoStatusEvaluator.nextStatus(
                        current = previous.status,
                        consecutiveCorrect = newCount
                    )
                    if (promoted != previous.status) {
                        wordDao.setStatus(wordId, promoted)
                    }
                }
            }
        }
    }

    /**
     * Reveals the trajectory of one unfound word on the board. The
     * UI keeps every cell of the chosen placement tinted for
     * [LetterSoupGameState.HINT_TIMEOUT_MS] so the player can see
     * the path of the word and read it off the grid.
     *
     * In [HintMode.WORLD] the reveal consumes one
     * [LetterSoupGameState.INITIAL_LOCATION_HINTS] use; the call is
     * a no-op when the inventory is empty.
     */
    fun revealLocationHint() {
        val state = _gameState.value as? LetterSoupGameState.InProgress ?: return
        val target = state.board.firstUnfixedPlacement() ?: return
        if (state.mode == HintMode.WORLD && state.locationHintsRemaining <= 0) return
        val newRemaining = if (state.mode == HintMode.WORLD) {
            (state.locationHintsRemaining - 1).coerceAtLeast(0)
        } else {
            state.locationHintsRemaining
        }
        _gameState.value = state.copy(
            highlightedPlacement = target,
            locationHintsRemaining = newRemaining
        )
    }

    /**
     * Reveals the English word for the active placement inline in
     * the always-visible translations list for
     * [LetterSoupGameState.HINT_TIMEOUT_MS].
     *
     * In [HintMode.WORLD] the reveal consumes one
     * [LetterSoupGameState.INITIAL_ENGLISH_HINTS] use; the call is
     * a no-op when the inventory is empty.
     */
    fun revealEnglishHint() {
        val state = _gameState.value as? LetterSoupGameState.InProgress ?: return
        if (state.board.firstUnfixedPlacement() == null) return
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
     * Clears the transient `wrongFlashCells` / `lastFoundWord`
     * markers once the UI has had a chance to animate them. Called
     * by the play screen after
     * [LetterSoupGameState.WRONG_FLASH_TIMEOUT_MS].
     */
    fun acknowledgeFlash() {
        val state = _gameState.value as? LetterSoupGameState.InProgress ?: return
        if (state.wrongFlashCells.isEmpty() && state.lastFoundWord == null) return
        _gameState.value = state.copy(
            wrongFlashCells = emptyList(),
            lastFoundWord = null
        )
    }

    /** Called by the UI when the location-hint timeout expires. */
    fun acknowledgeLocationHint() {
        val state = _gameState.value as? LetterSoupGameState.InProgress ?: return
        if (state.highlightedPlacement == null) return
        _gameState.value = state.copy(highlightedPlacement = null)
    }

    /** Called by the UI when the English-hint timeout expires. */
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

        val unlocked = progress.unlockedLevel.coerceAtMost(maxLevel)
        val nextLevel = (unlocked + 1).coerceAtMost(maxLevel)
        val newXpSince = progress.xpSinceLevelUp + xpToGrant
        val shouldUnlock = newXpSince >= CategoryGating.XP_MIN_PER_LEVEL &&
            nextLevel > unlocked

        categoryProgressDao.grantXpAndMaybeUnlock(
            categoryKey = CATEGORY_KEY,
            amount = xpToGrant,
            meetsXp = shouldUnlock,
            meetsLearnedPct = true,
            targetUnlockedLevel = if (shouldUnlock) nextLevel else unlocked
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

    override fun onCleared() {
        super.onCleared()
        cancelWorldTimer()
    }

    private companion object {
        /** Shortest word the board can comfortably host. */
        const val MIN_WORD_LENGTH: Int = 3

        /**
         * Longest word we are willing to host. Matches the standard
         * 12×12 board so any word that fits horizontally, vertically
         * or diagonally can be placed without overflow.
         */
        const val MAX_WORD_LENGTH: Int = 12

        /** Type literal used to filter verbs out of the pool. */
        const val VERB_TYPE: String = "verb"
    }
}
