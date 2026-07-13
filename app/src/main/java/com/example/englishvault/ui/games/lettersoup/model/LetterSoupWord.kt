package com.example.englishvault.ui.games.lettersoup.model

/**
 * One of the two target words placed on the Letter Soup board.
 *
 * @property original The dictionary word the placement represents,
 *   upper-cased. Compared against the live cells to know whether a
 *   swap has just fixed the word.
 * @property row Top-left row of the placement on the board (inclusive).
 * @property col Top-left column of the placement on the board (inclusive).
 * @property horizontal `true` when the word runs left-to-right
 *   (`row` is constant, columns increment), `false` for top-to-bottom.
 * @property wrongIndex Index within the word (0..length-1) of the
 *   letter that was deliberately replaced by a random soup letter.
 *   `wrongIndex = -1` means the placement has not been resolved yet
 *   (used transiently by [com.example.englishvault.ui.games.lettersoup.util.BoardGenerator]).
 * @property fixed `true` after the player has successfully swapped the
 *   wrong letter away. A fixed word stays on the board for visual
 *   continuity but no longer counts as an active target.
 * @property translation Spanish translation of the word, used as the
 *   hint when the player taps the hint button. `null` when the
 *   underlying dictionary entry had no translation.
 */
data class LetterSoupWord(
    val original: String,
    val row: Int,
    val col: Int,
    val horizontal: Boolean,
    val wrongIndex: Int = -1,
    val fixed: Boolean = false,
    val translation: String? = null
) {
    /** Convenience — `original.length`. */
    val length: Int get() = original.length

    /**
     * Returns the (row, col) coordinates of every cell this word
     * occupies, in reading order. Useful for the swap evaluator and
     * for drawing the word border in the UI.
     */
    fun cells(): List<Pair<Int, Int>> = List(length) { i ->
        if (horizontal) row to (col + i) else (row + i) to col
    }
}