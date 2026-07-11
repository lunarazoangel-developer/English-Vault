package com.example.englishvault.ui.games.wordmatch.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.englishvault.ui.games.wordmatch.model.WordMatchAnswer
import com.example.englishvault.ui.games.wordmatch.model.WordMatchAskType
import com.example.englishvault.ui.games.wordmatch.model.WordMatchError
import com.example.englishvault.ui.games.wordmatch.model.WordMatchGameState
import com.example.englishvault.ui.games.wordmatch.model.WordMatchQuestion
import com.example.englishvault.ui.games.wordmatch.util.DistractorGenerator
import data.database.dao.WordDao
import data.database.entities.WordEntity
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
 * One VM instance is scoped to the [WordMatchGameScreen]
 * destination. When the run finishes, the screen renders the
 * results UI in place (see [WordMatchGameState.Finished]); calling
 * [startGame] again starts a fresh run at the same level.
 */
@HiltViewModel
class WordMatchVerbsViewModel @Inject constructor(
    private val wordDao: WordDao
) : ViewModel() {

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
     * Loads the questions for [level] and flips the game state into
     * [WordMatchGameState.InProgress]. Idempotent: re-calling it
     * resets the run (zero errors, fresh score).
     */
    fun startGame(level: Int) {
        viewModelScope.launch {
            _gameState.value = WordMatchGameState.Loading
            val words = wordDao.getCoreWordsForGame(level)
            if (words.isEmpty()) {
                _gameState.value = WordMatchGameState.Empty
                return@launch
            }
            val questions = words.map(::buildQuestion)
            _gameState.value = WordMatchGameState.InProgress(
                questions = questions,
                currentIndex = 0,
                correctCount = 0,
                errors = emptyList(),
                lastAnswer = null
            )
        }
    }

    /**
     * Records [picked] as the player's answer for the current
     * question, mutating the [WordMatchGameState.InProgress] in
     * place. Wrong answers are appended to the running errors list
     * so the end UI can surface them.
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
        _gameState.value = state.copy(
            correctCount = newCorrect,
            errors = newErrors,
            lastAnswer = WordMatchAnswer(picked = picked, isCorrect = isCorrect)
        )
    }

    /**
     * Clears [WordMatchGameState.InProgress.lastAnswer] and either
     * advances to the next question or transitions to
     * [WordMatchGameState.Finished] when the run is complete.
     *
     * The screen drives this from a delayed `LaunchedEffect` once the
     * ✓ / ✗ overlay has been on screen long enough to read.
     */
    fun acknowledgeAnswer() {
        val state = _gameState.value as? WordMatchGameState.InProgress ?: return
        if (state.lastAnswer == null) return
        val nextIndex = state.currentIndex + 1
        if (nextIndex >= state.questions.size) {
            _gameState.value = WordMatchGameState.Finished(
                totalQuestions = state.questions.size,
                correctCount = state.correctCount,
                errors = state.errors
            )
        } else {
            _gameState.value = state.copy(
                currentIndex = nextIndex,
                lastAnswer = null
            )
        }
    }

    /**
     * Builds a single question from [word]: picks a random form,
     * resolves the correct answer and pairs it with two distractor
     * misspellings. The three options are shuffled so the correct
     * answer is not always in the same slot.
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
            options = allOptions
        )
    }
}