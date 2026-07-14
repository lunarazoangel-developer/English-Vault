package com.example.englishvault.ui.games.listening.model

import com.example.englishvault.ui.words.WordTypeFilter

/**
 * One round of the Listening mini-game.
 *
 * The TTS engine pronounces [targetWord] and the player picks the
 * matching text from [options]. The four options are pre-shuffled
 * by the ViewModel so the correct answer is not always in the same
 * slot. [category] / [wordLevel] are stamped onto the question so
 * the gameplay loop can credit the right `category_progress` row
 * without re-querying Room.
 *
 * @property targetWord The English word the device pronounces. Also
 *   the source of truth for the canonical spelling — comparison is
 *   case-insensitive.
 * @property options The four candidate spellings shown to the player.
 *   Contains exactly one entry equal (case-insensitive) to
 *   [targetWord] / [correctAnswer].
 * @property correctAnswer The canonical answer string. Matches
 *   [targetWord] exactly; both are kept so the question type can
 *   be evolved without breaking existing call sites.
 * @property category The [WordTypeFilter] bucket the source word
 *   belongs to. Used by the ViewModel to accumulate XP into the
 *   correct `category_progress` row.
 * @property wordLevel The dictionary level the word was drawn from.
 *   Currently informational only — the gameplay loop does not branch
 *   on it, but it lets future analytics tools correlate errors
 *   with difficulty.
 */
data class ListeningQuestion(
    val targetWord: String,
    val options: List<String>,
    val correctAnswer: String,
    val category: WordTypeFilter,
    val wordLevel: Int
) {
    init {
        require(options.size == OPTIONS_COUNT) {
            "ListeningQuestion requires exactly $OPTIONS_COUNT options, was ${options.size}"
        }
        require(options.any { it.equals(correctAnswer, ignoreCase = true) }) {
            "ListeningQuestion.options must contain the correct answer"
        }
    }

    companion object {
        /** Number of options shown per question. */
        const val OPTIONS_COUNT: Int = 4
    }
}

/**
 * Records the player's answer on the current question. The last
 * answer is kept on the [com.example.englishvault.ui.games.listening.model.ListeningGameState.InProgress]
 * state machine so the UI can render a transient feedback overlay
 * before auto-advancing.
 */
data class ListeningAnswer(
    val picked: String,
    val isCorrect: Boolean
)

/**
 * Aggregates a single wrong answer so the end-of-run panel can
 * show "you picked X — correct was Y". One [ListeningError] per
 * wrong answer (including time-outs where `userPicked` is `""`).
 */
data class ListeningError(
    val question: ListeningQuestion,
    val userPicked: String
)