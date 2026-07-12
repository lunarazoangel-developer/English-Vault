package com.example.englishvault.ui.games.wordmatchverbs.model

/**
 * State machine for the Word Match Verbs mini-game.
 *
 *  - [Loading] â€” the level is being loaded from Room.
 *  - [Empty]  â€” the level has no eligible verbs (everything is
 *    already LEARNED or there is no data at that level yet).
 *  - [InProgress] â€” the player is answering questions. [lastAnswer]
 *    carries the most recent response so the UI can render a
 *    transient feedback overlay before auto-advancing.
 *  - [Finished] â€” every question has been answered. [errors] is
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
        val correctXpByCategory: Map<String, Int> = emptyMap()
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
