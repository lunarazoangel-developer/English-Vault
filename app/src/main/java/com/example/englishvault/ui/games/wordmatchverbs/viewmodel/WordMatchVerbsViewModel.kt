package com.example.englishvault.ui.games.wordmatchverbs.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.englishvault.audio.SoundEffectPlayer
import com.example.englishvault.audio.SoundKey
import com.example.englishvault.ui.games.wordmatchverbs.model.GameMode
import com.example.englishvault.ui.games.wordmatchverbs.model.WordMatchAnswer
import com.example.englishvault.ui.games.wordmatchverbs.model.WordMatchAskType
import com.example.englishvault.ui.games.wordmatchverbs.model.WordMatchError
import com.example.englishvault.ui.games.wordmatchverbs.model.WordMatchGameState
import com.example.englishvault.ui.games.wordmatchverbs.model.WordMatchGameState.Companion.INITIAL_HELP_ITEMS
import com.example.englishvault.ui.games.wordmatchverbs.model.WordMatchGameState.Companion.INITIAL_LIVES
import com.example.englishvault.ui.games.wordmatchverbs.model.WordMatchGameState.Companion.INITIAL_TIME_BOOST_ITEMS
import com.example.englishvault.ui.games.wordmatchverbs.model.WordMatchGameState.Companion.MAX_TIME_REMAINING_MS
import com.example.englishvault.ui.games.wordmatchverbs.model.WordMatchGameState.Companion.QUESTION_TIME_MS
import com.example.englishvault.ui.games.wordmatchverbs.model.WordMatchGameState.Companion.TIME_BOOST_MS
import com.example.englishvault.ui.games.wordmatchverbs.model.WordMatchQuestion
import com.example.englishvault.ui.games.wordmatchverbs.util.DistractorGenerator
import com.example.englishvault.ui.words.WordTypeFilter
import data.database.dao.CategoryProgressDao
import data.database.dao.UserProfileDao
import data.database.dao.WordDao
import data.database.entities.CategoryProgressEntity
import data.database.entities.UserProfileEntity
import data.database.entities.WordEntity
import data.database.UserLevel
import data.game.CategoryGating
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
 * ViewModel for the Word Match Verbs mini-game.
 *
 * The active level is not stored on the VM — it is passed to
 * [startGame] from the play screen, which receives it as a
 * navigation argument. This keeps each VM scoped to a single run
 * and avoids cross-screen state pollution that previously caused the
 * game to hang at the loading screen.
 *
 * ## Modes
 *
 * The VM exposes [currentMode] as a [StateFlow] so the dev toggle
 * button on the play screen can switch between [GameMode.NORMAL] and
 * [GameMode.WORLD] without restarting. [startGame] reads the current
 * mode and seeds the [WordMatchGameState.InProgress] accordingly —
 * world runs get lives, a per-question timer, 50/50 help items and
 * +5s time-boost items.
 *
 * ## Timer
 *
 * World-mode runs start a coroutine inside [viewModelScope] that
 * decrements [WordMatchGameState.InProgress.timeRemainingMs] every
 * 100 ms. The coroutine is cancelled when the player answers (or
 * when the screen leaves composition). On expiry the VM calls
 * [onTimeExpired], which counts as a wrong answer and decrements
 * the lives counter.
 *
 * Phase 4.6 — XP grant pipeline:
 *  - Every correct answer accumulates `CategoryGating.XP_PER_CORRECT_ANSWER`
 *    in `InProgress.correctXpByCategory` under the verb's category key.
 *  - When [acknowledgeAnswer] transitions to [WordMatchGameState.Finished],
 *    the VM iterates that map and calls
 *    [CategoryProgressDao.grantXpAndMaybeUnlock] for each affected
 *    category. The DAO atomically grants the XP and, when the hybrid
 *    gate (`XP_MIN_PER_LEVEL` + `LEARNED_PCT_REQUIRED`) is satisfied,
 *    promotes `unlockedLevel` and resets `xpSinceLevelUp`.
 */
@HiltViewModel
class WordMatchVerbsViewModel @Inject constructor(
    private val wordDao: WordDao,
    private val categoryProgressDao: CategoryProgressDao,
    private val userProfileDao: UserProfileDao,
    private val soundEffectPlayer: SoundEffectPlayer
) : ViewModel() {

    /**
     * Live effects volume in `[0.0, 1.0]` read from the user profile.
     * Used by [submitAnswer] to scale the correct-answer SFX so the
     * Settings slider has an immediate effect on playback.
     *
     * Sharing is `Eagerly` (not `WhileSubscribed`) because this
     * StateFlow has no UI subscribers — only [submitAnswer] reads
     * `.value`. With `WhileSubscribed` the upstream Room flow would
     * never start, leaving `.value` stuck at [initialValue]
     * (`DEFAULT_VOLUME = 1.0`) regardless of the slider. The single-
     * row indexed query is cheap enough that keeping it alive for the
     * lifetime of the VM is fine.
     */
    private val effectsVolume: StateFlow<Float> = userProfileDao.observeProfile()
        .map { profile -> profile?.effectsVolume ?: UserProfileEntity.DEFAULT_VOLUME }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = UserProfileEntity.DEFAULT_VOLUME
        )

    /**
     * Currently selected mode. The dev toggle button flips this
     * between [GameMode.NORMAL] and [GameMode.WORLD] at any time;
     * [startGame] reads the value at run start and freezes the
     * per-run mode into the resulting [WordMatchGameState.InProgress].
     */
    private val _currentMode = MutableStateFlow(GameMode.NORMAL)
    val currentMode: StateFlow<GameMode> = _currentMode.asStateFlow()

    /**
     * Level the active run is playing at. Stored so [toggleMode]
     * can restart the run in the new mode without requiring the
     * screen to push the level back through `startGame` (the screen
     * only seeds the VM via `LaunchedEffect(level)`, which does not
     * re-fire when the toggle is tapped mid-run).
     */
    private var currentLevel: Int = 1

    /**
     * Active countdown for the current world-mode question. Cleared
     * whenever the player answers or the run ends so we never have
     * two ticks running in parallel.
     */
    private var timerJob: Job? = null

    private companion object {
        /**
         * Maximum number of questions shown per run. The eligible
         * verbs at a level are shuffled and truncated to this many
         * so a single game session stays in the 2–3 minute range
         * even when a level contains 30+ verbs. If a level has
         * fewer eligible verbs than this cap, the run uses all of
         * them.
         */
        const val MAX_QUESTIONS_PER_GAME: Int = 20

        /** How often the world-mode timer ticks the countdown. */
        const val TIMER_TICK_MS: Long = 100L
    }

    private val _gameState = MutableStateFlow<WordMatchGameState>(WordMatchGameState.Loading)
    val gameState: StateFlow<WordMatchGameState> = _gameState.asStateFlow()

    /**
     * Returns the maximum level currently used by the dictionary.
     * The level selector renders one card per level in `1..maxLevel`.
     */
    suspend fun maxLevel(): Int = wordDao.maxCoreLevel().coerceAtLeast(1)

    /**
     * Counts how many verbs are eligible for practice at [level].
     * Used by the level selector to show "N to play" on each card.
     */
    suspend fun verbsToPlayAt(level: Int): Int =
        wordDao.getCoreWordsForGame(level).size

    /**
     * Highest verb level the player can currently access.
     *
     * The game mixes regular and irregular verbs at every level,
     * so a level is considered unlocked if either verb category
     * has unlocked it. Returns `1` on a fresh install (no rows
     * yet seeded means both default to `unlockedLevel = 1`).
     */
    suspend fun maxUnlockedVerbLevel(): Int {
        val reg = categoryProgressDao.get(WordTypeFilter.VERBS_REGULAR.name)
            ?.unlockedLevel ?: CategoryGating.DEFAULT_UNLOCKED_LEVEL
        val irreg = categoryProgressDao.get(WordTypeFilter.VERBS_IRREGULAR.name)
            ?.unlockedLevel ?: CategoryGating.DEFAULT_UNLOCKED_LEVEL
        return maxOf(reg, irreg)
    }

    /**
     * Flips the dev toggle between [GameMode.NORMAL] and
     * [GameMode.WORLD] **and immediately restarts the active run**
     * so the new mode is applied without having to navigate away
     * and back. The previously running game (if any) is discarded —
     * questions, score, lives and timer all reset to the values
     * seeded for the new mode.
     */
    fun toggleMode() {
        _currentMode.value = when (_currentMode.value) {
            GameMode.NORMAL -> GameMode.WORLD
            GameMode.WORLD -> GameMode.NORMAL
        }
        // Apply the new mode to the current level right away. If no
        // run has been seeded yet `currentLevel` is still its
        // default (`1`); that's fine — tapping the toggle before
        // startGame runs just flips the flag for the upcoming run.
        cancelTimer()
        startGame(currentLevel)
    }

    /**
     * Loads the questions for [level] and flips the game state into
     * [WordMatchGameState.InProgress]. Idempotent: re-calling it
     * resets the run (zero errors, fresh score, empty XP map).
     *
     * Seeds mode-specific fields based on [currentMode]: world runs
     * receive lives, a countdown timer and help / boost inventories;
     * normal runs leave them at their `0` defaults.
     */
    fun startGame(level: Int) {
        currentLevel = level
        viewModelScope.launch {
            _gameState.value = WordMatchGameState.Loading
            val words = wordDao.getCoreWordsForGame(level)
            if (words.isEmpty()) {
                _gameState.value = WordMatchGameState.Empty
                return@launch
            }
            val selectedWords = words.shuffled().take(MAX_QUESTIONS_PER_GAME)
            val questions = selectedWords.map(::buildQuestion)
            val mode = _currentMode.value
            _gameState.value = WordMatchGameState.InProgress(
                questions = questions,
                currentIndex = 0,
                correctCount = 0,
                errors = emptyList(),
                lastAnswer = null,
                correctXpByCategory = emptyMap(),
                mode = mode,
                lives = if (mode == GameMode.WORLD) INITIAL_LIVES else 0,
                timeRemainingMs = if (mode == GameMode.WORLD) QUESTION_TIME_MS else 0L,
                helpItems = if (mode == GameMode.WORLD) INITIAL_HELP_ITEMS else 0,
                timeBoostItems = if (mode == GameMode.WORLD) INITIAL_TIME_BOOST_ITEMS else 0
            )
            if (mode == GameMode.WORLD) {
                startTimerForCurrentQuestion()
            }
        }
    }

    /**
     * Called by the play screen after the level argument changes
     * (or after `Play again`). Starts a fresh run.
     *
     * The current implementation is the same as [startGame] — the
     * dedicated method exists so call sites read clearly and so a
     * future iteration can hook in analytics without touching the
     * VM API.
     */
    fun startGameWithCurrentMode(level: Int) = startGame(level)

    /**
     * Records [picked] as the player's answer for the current
     * question, mutating the [WordMatchGameState.InProgress] in
     * place. Wrong answers are appended to the running errors list
     * so the end UI can surface them. Correct answers additionally
     * add XP to the question's category bucket; the actual DB
     * persistence happens at end-of-game in [acknowledgeAnswer].
     *
     * In [GameMode.WORLD] a wrong answer additionally costs one
     * life; when lives reach zero the next [acknowledgeAnswer]
     * transitions to [WordMatchGameState.Finished] with
     * `outOfLives = true`.
     *
     * Does NOT advance to the next question — the screen calls
     * [acknowledgeAnswer] after the brief feedback delay.
     */
    fun submitAnswer(picked: String) {
        val state = _gameState.value as? WordMatchGameState.InProgress ?: return
        cancelTimer()
        val question = state.currentQuestion ?: return
        val isCorrect = picked.equals(question.correctAnswer, ignoreCase = true)
        val newErrors = if (isCorrect) {
            state.errors
        } else {
            state.errors + WordMatchError(question = question, userPicked = picked)
        }
        val newCorrect = if (isCorrect) state.correctCount + 1 else state.correctCount

        val newXpByCategory = if (isCorrect) {
            val key = question.category.name
            state.correctXpByCategory +
                (key to ((state.correctXpByCategory[key] ?: 0) +
                    CategoryGating.XP_PER_CORRECT_ANSWER))
        } else {
            state.correctXpByCategory
        }

        val newLives = when {
            state.mode != GameMode.WORLD -> state.lives
            isCorrect -> state.lives
            else -> (state.lives - 1).coerceAtLeast(0)
        }

        _gameState.value = state.copy(
            correctCount = newCorrect,
            errors = newErrors,
            lastAnswer = WordMatchAnswer(picked = picked, isCorrect = isCorrect),
            correctXpByCategory = newXpByCategory,
            lives = newLives,
            timedOut = false
        )

        if (isCorrect) {
            soundEffectPlayer.play(SoundKey.Correct, effectsVolume.value)
        }
    }

    /**
     * Called by the world-mode timer coroutine when the countdown
     * reaches zero. Behaves like a wrong answer: appends to errors,
     * decrements lives and surfaces the feedback overlay.
     */
    private fun onTimeExpired() {
        val state = _gameState.value as? WordMatchGameState.InProgress ?: return
        if (state.lastAnswer != null) return
        cancelTimer()
        val question = state.currentQuestion ?: return
        val newErrors = state.errors + WordMatchError(question = question, userPicked = "")
        val newLives = (state.lives - 1).coerceAtLeast(0)
        _gameState.value = state.copy(
            errors = newErrors,
            lastAnswer = WordMatchAnswer(picked = "", isCorrect = false),
            lives = newLives,
            timedOut = true
        )
    }

    /**
     * Clears [WordMatchGameState.InProgress.lastAnswer] and either
     * advances to the next question or transitions to
     * [WordMatchGameState.Finished] when the run is complete.
     *
     * The end-of-run transition also persists the per-category XP
     * accumulated during the run, evaluating the hybrid gate
     * (XP threshold + learned percentage) for every affected
     * category. The DAO wraps each grant + promotion in a single
     * transaction so readers never observe an in-between state.
     *
     * The screen drives this from a delayed `LaunchedEffect` once the
     * feedback overlay has been on screen long enough to read.
     */
    fun acknowledgeAnswer() {
        val state = _gameState.value as? WordMatchGameState.InProgress ?: return
        if (state.lastAnswer == null) return
        val outOfLives = state.mode == GameMode.WORLD && state.lives <= 0
        val nextIndex = state.currentIndex + 1
        val outOfQuestions = nextIndex >= state.questions.size
        if (outOfQuestions || outOfLives) {
            cancelTimer()
            viewModelScope.launch {
                for ((categoryKey, xp) in state.correctXpByCategory) {
                    tryUnlockCategory(categoryKey, xp)
                }
                _gameState.value = WordMatchGameState.Finished(
                    totalQuestions = state.questions.size,
                    correctCount = state.correctCount,
                    errors = state.errors,
                    mode = state.mode,
                    outOfLives = outOfLives
                )
            }
            return
        }
        _gameState.value = state.copy(
            currentIndex = nextIndex,
            lastAnswer = null,
            timedOut = false,
            eliminatedOptions = emptySet(),
            timeRemainingMs = if (state.mode == GameMode.WORLD) QUESTION_TIME_MS else 0L
        )
        if (state.mode == GameMode.WORLD) {
            startTimerForCurrentQuestion()
        }
    }

    /**
     * Consumes one 50/50 help item: hides two incorrect options for
     * the current question. No-op outside [GameMode.WORLD] or when
     * the inventory is empty / the question has already been
     * answered.
     */
    fun useHelpItem() {
        val state = _gameState.value as? WordMatchGameState.InProgress ?: return
        if (state.mode != GameMode.WORLD) return
        if (state.helpItems <= 0) return
        if (state.lastAnswer != null) return
        val question = state.currentQuestion ?: return
        val wrongs = state.eliminatedOptions +
            question.options
                .filter { !it.equals(question.correctAnswer, ignoreCase = true) }
                .shuffled()
                .take(2 - state.eliminatedOptions.size.coerceAtMost(2))
                .toSet()
        _gameState.value = state.copy(
            helpItems = state.helpItems - 1,
            eliminatedOptions = wrongs
        )
    }

    /**
     * Consumes one +5s time-boost item. The remaining time grows by
     * [TIME_BOOST_MS] but is capped at [MAX_TIME_REMAINING_MS] so a
     * chain of boosts cannot pin a single question open forever.
     */
    fun useTimeBoostItem() {
        val state = _gameState.value as? WordMatchGameState.InProgress ?: return
        if (state.mode != GameMode.WORLD) return
        if (state.timeBoostItems <= 0) return
        if (state.lastAnswer != null) return
        if (state.timeRemainingMs <= 0L) return
        val boosted = (state.timeRemainingMs + TIME_BOOST_MS).coerceAtMost(MAX_TIME_REMAINING_MS)
        _gameState.value = state.copy(
            timeBoostItems = state.timeBoostItems - 1,
            timeRemainingMs = boosted
        )
    }

    /**
     * Launches a coroutine that ticks [timeRemainingMs] down by
     * [TIMER_TICK_MS] every iteration. Exits the loop when the
     * player answers (so the feedback overlay can take its time)
     * or when the countdown reaches zero (which calls
     * [onTimeExpired]). Any previous timer is cancelled first to
     * guarantee at most one tick coroutine is alive at any time.
     */
    private fun startTimerForCurrentQuestion() {
        cancelTimer()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(TIMER_TICK_MS)
                val current = _gameState.value as? WordMatchGameState.InProgress ?: return@launch
                if (current.lastAnswer != null) return@launch
                if (current.mode != GameMode.WORLD) return@launch
                val nextTime = (current.timeRemainingMs - TIMER_TICK_MS).coerceAtLeast(0L)
                _gameState.value = current.copy(timeRemainingMs = nextTime)
                if (nextTime <= 0L) {
                    onTimeExpired()
                    return@launch
                }
            }
        }
    }

    /** Cancels the active timer coroutine if there is one. */
    private fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    /**
     * Persists [xpToGrant] XP for [categoryKey] and, when the hybrid
     * gate (`CategoryGating.XP_MIN_PER_LEVEL` XP + 80% of words at
     * the current level marked `LEARNED`) is satisfied and the
     * category has not reached its per-dictionary max level,
     * promotes `unlockedLevel` to `currentLevel + 1` and resets
     * `xpSinceLevelUp` to zero (handled atomically by the DAO).
     */
    private suspend fun tryUnlockCategory(categoryKey: String, xpToGrant: Int) {
        val category = WordTypeFilter.entries.firstOrNull { it.name == categoryKey }
            ?: return
        // ALL and MINE carry null type/regular and are not tracked by
        // the progression system — skip them defensively even if a
        // bug ever pushes their key into the XP map.
        val typeLiteral = category.type ?: return
        val maxLevel = wordDao.maxLevelByType(typeLiteral, category.regular)
            .coerceAtLeast(1)
        val progress = categoryProgressDao.get(categoryKey)
            ?: CategoryProgressEntity.initial(categoryKey)

        val currentLevel = progress.unlockedLevel.coerceAtMost(maxLevel)
        val nextLevel = (currentLevel + 1).coerceAtMost(maxLevel)

        val xpAfter = progress.xpSinceLevelUp + xpToGrant
        val totalAtLevel = wordDao.countWordsAt(typeLiteral, category.regular, currentLevel)
        val learnedAtLevel = wordDao.countLearnedAt(typeLiteral, category.regular, currentLevel)
        val learnedPct = if (totalAtLevel == 0) 1f else learnedAtLevel.toFloat() / totalAtLevel

        val meetsXp = xpAfter >= CategoryGating.XP_MIN_PER_LEVEL
        val meetsLearnedPct = learnedPct >= CategoryGating.LEARNED_PCT_REQUIRED
        val shouldUnlock = nextLevel > currentLevel && meetsXp && meetsLearnedPct

        categoryProgressDao.grantXpAndMaybeUnlock(
            categoryKey = categoryKey,
            amount = xpToGrant,
            meetsXp = shouldUnlock,
            meetsLearnedPct = shouldUnlock,
            targetUnlockedLevel = if (shouldUnlock) nextLevel else currentLevel
        )

        @Suppress("UNUSED_VARIABLE")
        val derivedLevel = UserLevel.levelFromXp(progress.xpTotal + xpToGrant)
            .coerceAtMost(maxLevel)
    }

    /**
     * Builds a single question from [word]: picks a random form
     * (PAST_SIMPLE or PAST_PARTICIPLE after the Phase 7.4 cleanup),
     * resolves the correct answer and pairs it with two distractor
     * misspellings. The three options are shuffled so the correct
     * answer is not always in the same slot. The verb's
     * [WordTypeFilter] bucket and progression level are stamped on
     * the question so the gameplay loop can credit the right
     * category without re-querying Room.
     */
    private fun buildQuestion(word: WordEntity): WordMatchQuestion {
        val askType = WordMatchAskType.random()
        val correct = askType.correctAnswer(word)
        val distractors = DistractorGenerator.generate(correct, count = 2)
        val allOptions = (listOf(correct) + distractors).shuffled()
        return WordMatchQuestion(
            baseWord = word.word,
            askType = askType,
            correctAnswer = correct,
            options = allOptions,
            category = classifyWordSafely(word),
            wordLevel = word.level
        )
    }

    /**
     * Maps a [WordEntity] to its [WordTypeFilter] bucket. Falls back
     * to the closest tracked category when the word's type does not
     * match any of the eight grammar buckets (e.g. a future type
     * not yet wired into the progression system). Verbosity over
     * `WordTypeFilter.classify` which only handles tracked types.
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
        cancelTimer()
    }
}