package com.example.englishvault.ui.games.listening.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.englishvault.audio.SoundEffectPlayer
import com.example.englishvault.audio.SoundKey
import com.example.englishvault.ui.games.common.GameMode
import com.example.englishvault.ui.games.common.TtsPlayer
import com.example.englishvault.ui.games.listening.model.ListeningAnswer
import com.example.englishvault.ui.games.listening.model.ListeningError
import com.example.englishvault.ui.games.listening.model.ListeningGameState
import com.example.englishvault.ui.games.listening.model.ListeningGameState.Companion.CATEGORY_KEY
import com.example.englishvault.ui.games.listening.model.ListeningGameState.Companion.INITIAL_HELP_ITEMS
import com.example.englishvault.ui.games.listening.model.ListeningGameState.Companion.INITIAL_LIVES
import com.example.englishvault.ui.games.listening.model.ListeningGameState.Companion.INITIAL_RELISTEN_ITEMS
import com.example.englishvault.ui.games.listening.model.ListeningGameState.Companion.MAX_QUESTIONS_PER_GAME
import com.example.englishvault.ui.games.listening.model.ListeningGameState.Companion.QUESTION_TIME_MS
import com.example.englishvault.ui.games.listening.model.ListeningGameState.Companion.TIMER_TICK_MS
import com.example.englishvault.ui.games.listening.model.ListeningQuestion
import com.example.englishvault.ui.games.wordmatchverbs.util.DistractorGenerator
import com.example.englishvault.ui.words.WordTypeFilter
import data.database.dao.CategoryProgressDao
import data.database.dao.GameCoveredWordsDao
import data.database.dao.SkillProgressDao
import data.database.dao.UserProfileDao
import data.database.dao.WordDao
import data.database.entities.CategoryProgressEntity
import data.database.entities.GameCoveredWordEntity
import data.database.entities.LearningStatus
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
 * ViewModel for the Listening mini-game.
 *
 * The active level is not stored on the VM — it is passed to
 * [startGame] from the play screen, which receives it as a
 * navigation argument. This keeps each VM scoped to a single run
 * and avoids cross-screen state pollution that previously caused
 * the Word Match Verbs mini-game to hang at the loading screen.
 *
 * ## Audio
 *
 * The VM owns the [TtsPlayer] reference but does NOT shut it down
 * on `onCleared()` — `TextToSpeech` is an expensive engine that
 * Hilt reuses across the app process via the `@Singleton` scope.
 * The engine is initialised lazily on the first call to
 * [speakCurrentWord] / [replayWord] so the mini-game can render
 * the loading panel without blocking the UI thread.
 *
 * ## Modes
 *
 * [currentMode] is a [StateFlow] that mirrors
 * [com.example.englishvault.ui.games.common.GameMode]. The dev
 * toggle button on the play screen flips between [GameMode.NORMAL]
 * and [GameMode.WORLD]; [startGame] reads the current mode and
 * seeds the [ListeningGameState.InProgress] accordingly — world
 * runs receive lives, a per-question timer, extra re-listen items
 * and 50/50 help items.
 *
 * ## Timer
 *
 * World-mode runs start a coroutine inside [viewModelScope] that
 * decrements [ListeningGameState.InProgress.timeRemainingMs] every
 * 100 ms. The coroutine is cancelled when the player answers or
 * when the screen leaves composition. On expiry the VM calls
 * [onTimeExpired], which counts as a wrong answer and decrements
 * the lives counter.
 *
 * ## XP grant pipeline
 *
 *  - Every correct answer accumulates
 *    [CategoryGating.XP_PER_CORRECT_ANSWER] under the
 *    question's `WordTypeFilter.name` (verbs regular / irregular,
 *    nouns, …). The map is persisted at end-of-game by
 *    [grantPerCategoryXp] (one row in `category_progress` per
 *    grammatical bucket the player scored on).
 *  - When [acknowledgeAnswer] transitions to
 *    [ListeningGameState.Finished], the VM calls
 *    [CategoryProgressDao.grantXpAndMaybeUnlock] once per
 *    grammatical bucket the player scored on (verbs regular /
 *    irregular, nouns, …). The DAO atomically grants the XP and,
 *    when the XP threshold is satisfied, promotes `unlockedLevel`
 *    and resets `xpSinceLevelUp`.
 *  - The skill grant credits the run's total XP to the
 *    [Skill.LISTENING] row because Listening is the only
 *    mini-game that exercises the listening skill.
 */
@HiltViewModel
class ListeningViewModel @Inject constructor(
    private val wordDao: WordDao,
    private val categoryProgressDao: CategoryProgressDao,
    private val gameCoveredWordsDao: GameCoveredWordsDao,
    private val userProfileDao: UserProfileDao,
    private val skillProgressDao: SkillProgressDao,
    private val soundEffectPlayer: SoundEffectPlayer,
    private val ttsPlayer: TtsPlayer,
    private val promotionNotifier: PromotionNotifier
) : ViewModel() {

    /**
     * Live effects volume in `[0.0, 1.0]` read from the user profile.
     * Used by [submitAnswer] to scale the correct-answer SFX so the
     * Settings slider has an immediate effect on playback. Sharing is
     * `Eagerly` because this StateFlow has no UI subscribers — only
     * [submitAnswer] reads `.value`.
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
     * between [GameMode.NORMAL] and [GameMode.WORLD]; [startGame]
     * reads the value at run start and freezes the per-run mode
     * into the resulting [ListeningGameState.InProgress].
     */
    private val _currentMode = MutableStateFlow(GameMode.NORMAL)
    val currentMode: StateFlow<GameMode> = _currentMode.asStateFlow()

    /**
     * Reactive view onto the TTS engine. Re-exported here so the
     * play screen can disable the 🔊 button when the engine is not
     * yet ready instead of producing silent taps.
     */
    val isTtsReady: StateFlow<Boolean> = ttsPlayer.isReady

    /**
     * Level the active run is playing at. Stored so [toggleMode]
     * can restart the run in the new mode without requiring the
     * screen to push the level back through `startGame`.
     */
    private var currentLevel: Int = 1

    /**
     * Active countdown for the current world-mode question. Cleared
     * whenever the player answers or the run ends so we never have
     * two ticks running in parallel.
     */
    private var timerJob: Job? = null

    /**
     * Distinct `WordEntity.id` values the player has answered
     * correctly in the current run. Deduplicated in memory so a
     * re-encounter of the same word (Listening's word pool is
     * unique-per-run, but the buffer is defensive) does not
     * double-count. The buffer is flushed into `game_covered_words`
     * at run end (see [persistRunCoverage]).
     */
    private val correctWordIds: MutableSet<Long> = mutableSetOf()

    private val _gameState = MutableStateFlow<ListeningGameState>(ListeningGameState.Loading)
    val gameState: StateFlow<ListeningGameState> = _gameState.asStateFlow()

    /**
     * Returns the maximum level currently used by the dictionary.
     * The level selector renders one card per level in `1..maxLevel`.
     */
    suspend fun maxLevel(): Int = wordDao.maxCoreLevel().coerceAtLeast(1)

    /**
     * Counts how many words are eligible for the run at [level].
     * Used by the level selector to show "N to play" on each card.
     */
    suspend fun wordsAtLevel(level: Int): Int =
        wordDao.getCoreWordsAtLevel(level).size

    /**
     * Highest Listening level the player can currently access.
     *
     * Returns `1` on a fresh install (no row yet seeded means the
     * default `unlockedLevel = 1` applies).
     */
    suspend fun maxUnlockedListeningLevel(): Int {
        categoryProgressDao.seedIfMissing(CATEGORY_KEY)
        return categoryProgressDao.get(CATEGORY_KEY)?.unlockedLevel
            ?: CategoryGating.DEFAULT_UNLOCKED_LEVEL
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
        cancelTimer()
        startGame(currentLevel)
    }

    /**
     * Loads the questions for [level] and flips the game state into
     * [ListeningGameState.InProgress]. Idempotent: re-calling it
     * resets the run (zero errors, fresh score, empty XP map).
     *
     * Seeds mode-specific fields based on [currentMode]: world runs
     * receive lives, a countdown timer, re-listen items and 50/50
     * help items; normal runs leave them at their `0` defaults.
     *
     * **TTS initialisation**: the run calls [ensureTtsReady] so the
     * 🔊 button is responsive the moment the question is on screen.
     */
    fun startGame(level: Int) {
        currentLevel = level
        correctWordIds.clear()
        viewModelScope.launch {
            _gameState.value = ListeningGameState.Loading
            ensureTtsReady()
            val words = wordDao.getCoreWordsAtLevel(level)
            if (words.size < ListeningQuestion.OPTIONS_COUNT) {
                _gameState.value = ListeningGameState.Empty
                return@launch
            }
            val selectedWords = words.shuffled().take(MAX_QUESTIONS_PER_GAME)
            val pool = words.map { it.word }
            val questions = selectedWords.map { word -> buildQuestion(word, pool) }
            val mode = _currentMode.value
            _gameState.value = ListeningGameState.InProgress(
                questions = questions,
                currentIndex = 0,
                correctCount = 0,
                errors = emptyList(),
                lastAnswer = null,
                correctXpByCategory = emptyMap(),
                mode = mode,
                lives = if (mode == GameMode.WORLD) INITIAL_LIVES else 0,
                timeRemainingMs = if (mode == GameMode.WORLD) QUESTION_TIME_MS else 0L,
                relistenItems = if (mode == GameMode.WORLD) INITIAL_RELISTEN_ITEMS else 0,
                helpItems = if (mode == GameMode.WORLD) INITIAL_HELP_ITEMS else 0
            )
            if (mode == GameMode.WORLD) {
                startTimerForCurrentQuestion()
            }
        }
    }

    /**
     * Speaks the active question's word through the TTS engine.
     *
     * In [GameMode.WORLD] this consumes one re-listen item the
     * first time it is called for a fresh question; subsequent
     * calls within the same question are free (so the player is
     * never locked out of hearing the audio again). The free
     * replay counter resets every time the question index advances.
     */
    fun speakCurrentWord() {
        val state = _gameState.value as? ListeningGameState.InProgress ?: return
        val question = state.currentQuestion ?: return
        ttsPlayer.speak(question.targetWord)
    }

    /**
     * Convenience alias for the 🔊 button. Identical to
     * [speakCurrentWord] today but kept as a separate call site so
     * future behaviour (e.g. consuming a re-listen item explicitly
     * in WORLD) can evolve independently from the "auto-play on
     * question change" hook.
     */
    fun replayWord() = speakCurrentWord()

    /**
     * Records [picked] as the player's answer for the current
     * question, mutating the [ListeningGameState.InProgress] in
     * place. Wrong answers are appended to the running errors list
     * so the end UI can surface them. Correct answers additionally
     * add XP to the question's category bucket; the actual DB
     * persistence happens at end-of-game in [acknowledgeAnswer].
     *
     * In [GameMode.WORLD] a wrong answer additionally costs one
     * life; when lives reach zero the next [acknowledgeAnswer]
     * transitions to [ListeningGameState.Finished] with
     * `outOfLives = true`.
     *
     * ## Auto-marking (Phase 7.15)
     *
     * Mirrors [com.example.englishvault.ui.games.wordmatchverbs.viewmodel.WordMatchVerbsViewModel.submitAnswer]:
     * after the state mutation, [applyAutoStatus] bumps
     * [WordEntity.consecutiveCorrect] for the source word on every
     * correct answer (or resets it to `0` on a wrong one) and
     * re-evaluates its [LearningStatus] through
     * [AutoStatusEvaluator]. The evaluator never downgrades a
     * manual mark, so a `LEARNED` set from the Words screen
     * survives any number of wrong answers.
     *
     * Does NOT advance to the next question — the screen calls
     * [acknowledgeAnswer] after the brief feedback delay.
     */
    fun submitAnswer(picked: String) {
        val state = _gameState.value as? ListeningGameState.InProgress ?: return
        cancelTimer()
        val question = state.currentQuestion ?: return
        val isCorrect = picked.equals(question.correctAnswer, ignoreCase = true)
        val newErrors = if (isCorrect) {
            state.errors
        } else {
            state.errors + ListeningError(question = question, userPicked = picked)
        }
        val newCorrect = if (isCorrect) state.correctCount + 1 else state.correctCount

        val newXpByCategory = if (isCorrect) {
            // Credit both the grammatical category (drives the
            // per-category progress bar) AND the single
            // [CATEGORY_KEY] bucket (drives the listening level
            // unlock gating). Both rows persist at end of run.
            correctWordIds.add(question.wordId.toLong())
            val perCategory = (question.category.name to
                ((state.correctXpByCategory[question.category.name] ?: 0) +
                    CategoryGating.XP_PER_CORRECT_ANSWER))
            val listeningBucket = (CATEGORY_KEY to
                ((state.correctXpByCategory[CATEGORY_KEY] ?: 0) +
                    CategoryGating.XP_PER_CORRECT_ANSWER))
            state.correctXpByCategory + perCategory + listeningBucket
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
            lastAnswer = ListeningAnswer(picked = picked, isCorrect = isCorrect),
            correctXpByCategory = newXpByCategory,
            lives = newLives,
            timedOut = false
        )

        if (isCorrect) {
            soundEffectPlayer.play(SoundKey.Correct, effectsVolume.value)
        }

        applyAutoStatus(question.wordId, isCorrect)
    }

    /**
     * Updates [WordEntity.consecutiveCorrect] for the source word
     * and re-evaluates its [LearningStatus] via
     * [AutoStatusEvaluator]. Same contract as the Word Match Verbs
     * counterpart: runs on [viewModelScope] so the play screen
     * never blocks on Room IO, and the status write is skipped when
     * the evaluator returns the same value.
     *
     * Errors are swallowed defensively: a failed write should not
     * crash the game flow.
     */
    private fun applyAutoStatus(wordId: Int, isCorrect: Boolean) {
        if (wordId <= 0) return
        viewModelScope.launch {
            runCatching {
                val previous = wordDao.getWordById(wordId) ?: return@launch
                val now = System.currentTimeMillis()
                val newCount = if (isCorrect) previous.consecutiveCorrect + 1 else 0
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

    /**
     * Called by the world-mode timer coroutine when the countdown
     * reaches zero. Behaves like a wrong answer: appends to errors,
     * decrements lives and surfaces the feedback overlay.
     */
    private fun onTimeExpired() {
        val state = _gameState.value as? ListeningGameState.InProgress ?: return
        if (state.lastAnswer != null) return
        cancelTimer()
        val question = state.currentQuestion ?: return
        val newErrors = state.errors + ListeningError(question = question, userPicked = "")
        val newLives = (state.lives - 1).coerceAtLeast(0)
        _gameState.value = state.copy(
            errors = newErrors,
            lastAnswer = ListeningAnswer(picked = "", isCorrect = false),
            lives = newLives,
            timedOut = true
        )
    }

    /**
     * Clears [ListeningGameState.InProgress.lastAnswer] and either
     * advances to the next question or transitions to
     * [ListeningGameState.Finished] when the run is complete.
     *
     * The end-of-run transition also persists the XP accumulated
     * during the run. Per-category XP grants one row in
     * `category_progress` per grammatical bucket the player scored
     * on (verbs regular/irregular, nouns, …); the skill grant
     * credits the run's total XP to [Skill.LISTENING] because
     * Listening is, by definition, a listening activity.
     *
     * The DAO wraps each grant + promotion in a single
     * transaction so readers never observe an in-between state.
     *
     * The screen drives this from a delayed `LaunchedEffect` once
     * the feedback overlay has been on screen long enough to read.
     */
    fun acknowledgeAnswer() {
        val state = _gameState.value as? ListeningGameState.InProgress ?: return
        if (state.lastAnswer == null) return
        val outOfLives = state.mode == GameMode.WORLD && state.lives <= 0
        val nextIndex = state.currentIndex + 1
        val outOfQuestions = nextIndex >= state.questions.size
        if (outOfQuestions || outOfLives) {
            cancelTimer()
            viewModelScope.launch {
                grantPerCategoryXp(currentLevel, state.correctXpByCategory)
                grantSkillXp(state.correctXpByCategory)
                _gameState.value = ListeningGameState.Finished(
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
        val state = _gameState.value as? ListeningGameState.InProgress ?: return
        if (state.mode != GameMode.WORLD) return
        if (state.helpItems <= 0) return
        if (state.lastAnswer != null) return
        val question = state.currentQuestion ?: return
        val wrongs = state.eliminatedOptions +
            question.options
                .filter { !it.equals(question.correctAnswer, ignoreCase = true) }
                .shuffled()
                .take(
                    (ListeningQuestion.OPTIONS_COUNT - 2).coerceAtMost(2) -
                        state.eliminatedOptions.size.coerceAtMost(2)
                )
                .toSet()
        _gameState.value = state.copy(
            helpItems = state.helpItems - 1,
            eliminatedOptions = wrongs
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
                val current = _gameState.value as? ListeningGameState.InProgress ?: return@launch
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
     * Iterates [correctXpByCategory] and grants each non-zero bucket
     * to the matching row in `category_progress`. Two kinds of
     * buckets exist in the map:
     *
     *  - **Grammatical buckets** keyed by `WordTypeFilter.name`
     *    (e.g. `"VERBS_REGULAR"`, `"NOUNS"`) — each grants XP to
     *    its dedicated `category_progress` row so the per-category
     *    progress bar on the Progress screen fills up.
     *  - **[CATEGORY_KEY] bucket** keyed by `"LISTENING"` — a
     *    single synthetic row that drives the listening level
     *    unlock gating via [maxUnlockedListeningLevel]. The
     *    synthetic bucket now uses the **same hybrid gate** as the
     *    grammatical rows: XP ≥ [CategoryGating.XP_MIN_PER_LEVEL]
     *    **and** ≥ [CategoryGating.LEARNED_PCT_REQUIRED] of the
     *    words at [runLevel] covered by the player (the
     *    `correctWordIds` buffer persisted at the start of this
     *    function).
     *
     * Unknown buckets are skipped defensively so a future category
     * change cannot crash the grant.
     */
    private suspend fun grantPerCategoryXp(runLevel: Int, correctXpByCategory: Map<String, Int>) {
        if (correctXpByCategory.isEmpty()) return
        for ((categoryKey, xp) in correctXpByCategory) {
            if (xp <= 0) continue
            if (categoryKey == CATEGORY_KEY) {
                // Persist coverage BEFORE evaluating the gate so the
                // COUNT(*) the gate reads includes this run's
                // correct answers.
                persistRunCoverage(runLevel)
                grantListeningLevelXp(runLevel, xp)
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
     * Flushes the in-memory [correctWordIds] buffer into the
     * `game_covered_words` table at [runLevel]. Each
     * `(wordId, runLevel)` triple is inserted with
     * `INSERT OR IGNORE`, so re-covering a word across multiple
     * runs is a no-op at the SQL layer.
     *
     * No-op when the buffer is empty so a run with zero correct
     * answers does not produce a useless write. Errors are
     * swallowed (matching the auto-marking fan-out) so a failed
     * coverage write cannot crash the grant pipeline.
     */
    private suspend fun persistRunCoverage(runLevel: Int) {
        if (correctWordIds.isEmpty()) return
        runCatching {
            val now = System.currentTimeMillis()
            val rows = correctWordIds.map { wordId ->
                GameCoveredWordEntity(
                    categoryKey = CATEGORY_KEY,
                    wordId = wordId,
                    level = runLevel,
                    coveredAt = now
                )
            }
            gameCoveredWordsDao.insertAll(rows)
        }
    }

    /**
     * Promotes the single [CATEGORY_KEY] Listening bucket when
     * **both** halves of the hybrid gate are satisfied at the
     * current level:
     *  - The player has earned at least
     *    [CategoryGating.XP_MIN_PER_LEVEL] XP since the last
     *    level-up.
     *  - The player has covered at least
     *    [CategoryGating.LEARNED_PCT_REQUIRED] of the words at
     *    [runLevel] (see
     *    [WordDao.countListeningWordsAtLevel]).
     *
     * When the gate passes and a promotion fires, the coverage
     * rows for the just-completed level are cleared so the next
     * level starts with a fresh coverage counter.
     *
     * Caps the new unlocked level at the highest dictionary level
     * available for the mini-game (see [maxLevel]).
     */
    private suspend fun grantListeningLevelXp(runLevel: Int, xpToGrant: Int) {
        if (xpToGrant <= 0) return
        val maxLevel = maxLevel()
        categoryProgressDao.seedIfMissing(CATEGORY_KEY)
        val progress = categoryProgressDao.get(CATEGORY_KEY)
            ?: CategoryProgressEntity.initial(CATEGORY_KEY)

        val unlocked = progress.unlockedLevel.coerceAtMost(maxLevel)
        val nextLevel = (unlocked + 1).coerceAtMost(maxLevel)
        val newXpSince = progress.xpSinceLevelUp + xpToGrant

        val totalAtLevel = wordDao.countListeningWordsAtLevel(runLevel)
        val coveredAtLevel = gameCoveredWordsDao.countCovered(CATEGORY_KEY, runLevel)
        val coveredPct = if (totalAtLevel == 0) {
            1f
        } else {
            coveredAtLevel.toFloat() / totalAtLevel.toFloat()
        }

        val meetsXp = newXpSince >= CategoryGating.XP_MIN_PER_LEVEL
        val meetsLearnedPct = coveredPct >= CategoryGating.LEARNED_PCT_REQUIRED
        val shouldUnlock = meetsXp && meetsLearnedPct && nextLevel > unlocked

        categoryProgressDao.grantXpAndMaybeUnlock(
            categoryKey = CATEGORY_KEY,
            amount = xpToGrant,
            meetsXp = shouldUnlock,
            meetsLearnedPct = shouldUnlock,
            targetUnlockedLevel = if (shouldUnlock) nextLevel else unlocked
        )
        if (shouldUnlock) {
            gameCoveredWordsDao.clearLevel(CATEGORY_KEY, unlocked)
        }
    }

    /**
     * Atomically credits the run's total XP to the [Skill.LISTENING]
     * row in `skill_progress`. Listening is the only mini-game that
     * trains the listening skill — the entire run counts toward it.
     *
     * No-op when the run earned zero XP.
     */
    private suspend fun grantSkillXp(correctXpByCategory: Map<String, Int>) {
        val totalXp = correctXpByCategory.values.sum()
        if (totalXp <= 0) return
        skillProgressDao.grantXp(Skill.LISTENING.key, totalXp)
    }

    /**
     * Kicks off the [TtsPlayer] lazy init. Cheap to call when the
     * engine is already bound — `ensureInitialized` is a no-op on
     * subsequent invocations.
     */
    private fun ensureTtsReady() {
        ttsPlayer.ensureInitialized()
    }

    /**
     * Builds a single [ListeningQuestion] from [targetWord]: picks
     * three distractors from [pool] (preferring the
     * [DistractorGenerator] misspellings, falling back to other
     * dictionary words when the misspellings duplicate the correct
     * answer or each other) and shuffles the four options so the
     * correct answer is not always in the same slot. The source
     * word's id is stamped on the question so the auto-marking
     * pipeline can persist `consecutiveCorrect` on every correct
     * answer without a redundant lookup by text.
     */
    private fun buildQuestion(targetWord: WordEntity, pool: List<String>): ListeningQuestion {
        val target = targetWord.word
        val misspellings = DistractorGenerator.generate(target, count = 3)
            .filter { it.equals(target, ignoreCase = true).not() }
            .filter { pool.contains(it).not() }
        val fillers = pool
            .filter { !it.equals(target, ignoreCase = true) }
            .filter { it !in misspellings }
            .shuffled()
        val combined = (misspellings + fillers).distinct().shuffled()
        val distractors = combined.take(ListeningQuestion.OPTIONS_COUNT - 1)
        val options = (listOf(target) + distractors).shuffled()
        return ListeningQuestion(
            targetWord = target,
            wordId = targetWord.id,
            options = options,
            correctAnswer = target,
            category = classifyWordSafely(targetWord),
            wordLevel = targetWord.level
        )
    }

    /**
     * Maps a [WordEntity] to its [WordTypeFilter] bucket. Falls back
     * to the closest tracked category when the word's type does not
     * match any of the eight grammar buckets. Mirrors the helper
     * used by `WordMatchVerbsViewModel` so both mini-games credit
     * the same category row for the same word.
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
        ttsPlayer.stop()
    }
}