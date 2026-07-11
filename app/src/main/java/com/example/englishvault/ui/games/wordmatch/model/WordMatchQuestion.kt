package com.example.englishvault.ui.games.wordmatch.model

import data.database.entities.WordEntity

/**
 * Which form of the verb is being tested in a single
 * [WordMatchQuestion].
 */
enum class WordMatchAskType(val promptResId: Int) {
    PAST_SIMPLE(com.example.englishvault.R.string.game_wordmatch_ask_past_simple),
    THIRD_PERSON(com.example.englishvault.R.string.game_wordmatch_ask_third_person),
    PAST_PARTICIPLE(com.example.englishvault.R.string.game_wordmatch_ask_past_participle);

    /**
     * Resolves the correct answer for [type] using the [forms]
     * table on a [WordEntity]. Falls back to the base form when the
     * specific conjugation is missing.
     */
    fun correctAnswer(word: WordEntity): String {
        val forms = word.forms ?: return word.word
        return when (this) {
            PAST_SIMPLE -> forms.pastSimple?.takeIf { it.isNotBlank() } ?: word.word
            THIRD_PERSON -> forms.thirdPerson?.takeIf { it.isNotBlank() } ?: word.word
            PAST_PARTICIPLE -> forms.pastParticiple?.takeIf { it.isNotBlank() } ?: word.word
        }
    }

    companion object {
        /** Picks a random form to ask about for the current question. */
        fun random(): WordMatchAskType = entries.random()
    }
}

/**
 * One round of the Word Match Verbs game.
 *
 * @property baseWord The verb in its dictionary base form.
 * @property askType Which conjugation the player must pick.
 * @property correctAnswer The expected answer string.
 * @property options Three strings shown as multiple-choice buttons.
 *   Always includes [correctAnswer]; the other two are misspellings
 *   produced by [com.example.englishvault.ui.games.wordmatch.util.DistractorGenerator].
 */
data class WordMatchQuestion(
    val baseWord: String,
    val askType: WordMatchAskType,
    val correctAnswer: String,
    val options: List<String>
)

/**
 * The answer the player just submitted, plus whether it was right.
 * Used by the UI to render a transient ✓ / ✗ overlay before
 * auto-advancing to the next question.
 */
data class WordMatchAnswer(
    val picked: String,
    val isCorrect: Boolean
)

/**
 * A wrong answer collected during the run. The end screen renders
 * these so the learner can see which words tripped them up.
 */
data class WordMatchError(
    val question: WordMatchQuestion,
    val userPicked: String
)