package com.example.englishvault.ui.games.wordmatchverbs.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.englishvault.ui.games.wordmatchverbs.model.WordMatchAnswer
import com.example.englishvault.ui.games.wordmatchverbs.model.WordMatchAskType
import com.example.englishvault.ui.games.wordmatchverbs.model.WordMatchError
import com.example.englishvault.ui.games.wordmatchverbs.model.WordMatchGameState
import com.example.englishvault.ui.games.wordmatchverbs.model.WordMatchQuestion
import com.example.englishvault.ui.games.wordmatchverbs.util.DistractorGenerator
import com.example.englishvault.ui.words.WordTypeFilter
import data.database.dao.CategoryProgressDao
import data.database.dao.WordDao
import data.database.entities.CategoryProgressEntity
import data.database.entities.WordEntity
import data.game.CategoryGating
import data.database.UserLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
 * One VM instance is scoped to the [WordMatchVerbsGameScreen]
 * destination. When the run finishes, the screen renders the
 * results UI in place (see [WordMatchGameState.Finished]); calling
 * [startGame] again starts a fresh run at the same level.
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
    private val categoryProgressDao: CategoryProgressDao
) : ViewModel() {

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
     * Loads the questions for [level] and flips the game state into
     * [WordMatchGameState.InProgress]. Idempotent: re-calling it
     * resets the run (zero errors, fresh score, empty XP map).
     */
    fun startGame(level: Int) {
        viewModelScope.launch {
            _gameState.value = WordMatchGameState.Loading
            val words = wordDao.getCoreWordsForGame(level)
            if (words.isEmpty()) {
                _gameState.value = WordMatchGameState.Empty
                return@launch
            }
            // Pick a random sample of up to MAX_QUESTIONS_PER_GAME
            // verbs so the player does not see the whole pool in one
            // run. Re-runs of the same level get a fresh shuffle.
            val selectedWords = words.shuffled().take(MAX_QUESTIONS_PER_GAME)
            val questions = selectedWords.map(::buildQuestion)
            _gameState.value = WordMatchGameState.InProgress(
                questions = questions,
                currentIndex = 0,
                correctCount = 0,
                errors = emptyList(),
                lastAnswer = null,
                correctXpByCategory = emptyMap()
            )
        }
    }

    /**
     * Records [picked] as the player's answer for the current
     * question, mutating the [WordMatchGameState.InProgress] in
     * place. Wrong answers are appended to the running errors list
     * so the end UI can surface them. Correct answers additionally
     * add XP to the question's category bucket; the actual DB
     * persistence happens at end-of-game in [acknowledgeAnswer].
     *
     * Does NOT advance to the next question — the screen calls
     * [acknowledgeAnswer] after the brief feedback delay.
     */
    fun submitAnswer(picked: String) {
        val state = _gameState.value as? WordMatchGameState.InProgress ?: return
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

        _gameState.value = state.copy(
            correctCount = newCorrect,
            errors = newErrors,
            lastAnswer = WordMatchAnswer(picked = picked, isCorrect = isCorrect),
            correctXpByCategory = newXpByCategory
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
        val nextIndex = state.currentIndex + 1
        if (nextIndex >= state.questions.size) {
            // End of run — grant accumulated XP per category before
            // transitioning to the Finished state so the Progress
            // screen reflects the gains the moment the user
            // navigates back to it.
            viewModelScope.launch {
                for ((categoryKey, xp) in state.correctXpByCategory) {
                    tryUnlockCategory(categoryKey, xp)
                }
                _gameState.value = WordMatchGameState.Finished(
                    totalQuestions = state.questions.size,
                    correctCount = state.correctCount,
                    errors = state.errors
                )
            }
        } else {
            _gameState.value = state.copy(
                currentIndex = nextIndex,
                lastAnswer = null
            )
        }
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

        // Surface currentLevel (derived from xpTotal) for callers
        // that need it. Currently unused outside this VM but kept
        // here as a hook for future logging or telemetry.
        @Suppress("UNUSED_VARIABLE")
        val derivedLevel = UserLevel.levelFromXp(progress.xpTotal + xpToGrant)
            .coerceAtMost(maxLevel)
    }

    /**
     * Builds a single question from [word]: picks a random form,
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
        // Verb-ish word whose `regular` flag is unexpected: best
        // effort bucket assignment.
        return if (word.type == "verb") {
            if (word.regular == false) WordTypeFilter.VERBS_IRREGULAR
            else WordTypeFilter.VERBS_REGULAR
        } else {
            WordTypeFilter.ADJECTIVES
        }
    }
}