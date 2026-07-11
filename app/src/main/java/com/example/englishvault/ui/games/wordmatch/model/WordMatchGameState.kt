package com.example.englishvault.ui.games.wordmatch.model

/**
 * State machine for the Word Match Verbs mini-game.
 *
 *  - [Loading] — the level is being loaded from Room.
 *  - [Empty]  — the level has no eligible verbs (everything is
 *    already LEARNED or there is no data at that level yet).
 *  - [InProgress] — the player is answering questions. [lastAnswer]
 *    carries the most recent response so the UI can render a
 *    transient feedback overlay before auto-advancing.
 *  - [Finished] — every question has been answered. [errors] is
 *    populated with the wrong answers for the end screen.
 */
sealed class WordMatchGameState {

    object Loading : WordMatchGameState()

    object Empty : WordMatchGameState()

    data class InProgress(
        val questions: List<WordMatchQuestion>,
        val currentIndex: Int,
        val correctCount: Int,
        val errors: List<WordMatchError>,
        val lastAnswer: WordMatchAnswer? = null
    ) : WordMatchGameState() {
        val totalQuestions: Int get() = questions.size
        val currentQuestion: WordMatchQuestion? = questions.getOrNull(currentIndex)
        val isLastQuestion: Boolean get() = currentIndex >= questions.lastIndex
    }

    data class Finished(
        val totalQuestions: Int,
        val correctCount: Int,
        val errors: List<WordMatchError>
    ) : WordMatchGameState()
}