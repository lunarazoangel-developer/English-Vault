package com.example.englishvault.ui.games.wordmatchverbs.model

import com.example.englishvault.ui.words.WordTypeFilter
import data.database.entities.WordEntity

/**
 * Which form of the verb is being tested in a single
 * [WordMatchQuestion].
 *
 * Phase 7.4 dropped `THIRD_PERSON` from the rotation — the form was
 * deemed too predictable from the base verb. The game now alternates
 * between `PAST_SIMPLE` and `PAST_PARTICIPLE`.
 */
enum class WordMatchAskType(val promptResId: Int) {
    PAST_SIMPLE(com.example.englishvault.R.string.game_wordmatch_ask_past_simple),
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
 * @property wordId The id of the underlying [data.database.entities.WordEntity]
 *   in `core_words` (the only source Word Match Verbs draws from).
 *   Required by the auto-marking pipeline
 *   ([data.game.AutoStatusEvaluator] + [data.database.dao.WordDao.setConsecutiveCorrect])
 *   so the VM can persist `consecutiveCorrect` and (when applicable)
 *   promote the word's [data.database.entities.LearningStatus] on
 *   every correct answer without a redundant lookup by text.
 * @property askType Which conjugation the player must pick.
 * @property correctAnswer The expected answer string.
 * @property options Three strings shown as multiple-choice buttons.
 *   Always includes [correctAnswer]; the other two are misspellings
 *   produced by [com.example.englishvault.ui.games.wordmatchverbs.util.DistractorGenerator].
 * @property category The grammatical category bucket this verb
 *   belongs to (regular verbs / irregular verbs / …). Used by the
 *   ViewModel to credit XP to the right per-category progress row.
 * @property wordLevel The verb's progression level (1..N). Carried
 *   alongside the category so the gating evaluator can compute
 *   learned-percentage at the user's current level.
 */
data class WordMatchQuestion(
    val baseWord: String,
    val wordId: Int,
    val askType: WordMatchAskType,
    val correctAnswer: String,
    val options: List<String>,
    val category: WordTypeFilter,
    val wordLevel: Int
)

/**
 * The answer the player just submitted, plus whether it was right.
 * Used by the UI to render a transient âœ“ / âœ— overlay before
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
