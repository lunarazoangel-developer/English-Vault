package com.example.englishvault.ui.games.lettersoup.model

/**
 * One of the target words placed on the Letter Soup board.
 *
 * @property original The dictionary word the placement represents,
 *   upper-cased. Compared against the live cells to know whether the
 *   player's underlined selection matches a target word.
 * @property wordId The id of the underlying
 *   [data.database.entities.WordEntity] in `core_words`. Required by
 *   the auto-marking pipeline
 *   ([data.game.AutoStatusEvaluator] + [data.database.dao.WordDao.setConsecutiveCorrect])
 *   so the VM can persist `consecutiveCorrect` and (when applicable)
 *   promote the word's [data.database.entities.LearningStatus] every
 *   time the player fixes the placement. `0` when the id could not
 *   be resolved (defensive fallback for any future board that
 *   carries synthetic words).
 * @property row Anchor row of the placement (the first letter's row).
 * @property col Anchor column of the placement (the first letter's col).
 * @property direction Reading direction from the anchor. The full
 *   chain of cells is produced by stepping `length` times in this
 *   direction. With the eight supported directions, words can be
 *   horizontal, vertical, diagonal, and reversed — covering every
 *   layout a classic word-search supports.
 * @property fixed `true` after the player has underlined the entire
 *   word on the board. A fixed word stays on the board for visual
 *   continuity but no longer counts as an active target.
 * @property translation Spanish translation of the word, used as the
 *   hint when the player taps the English-hint button. `null` when
 *   the underlying dictionary entry had no translation.
 */
data class LetterSoupWord(
    val original: String,
    val wordId: Int = 0,
    val row: Int,
    val col: Int,
    val direction: Direction,
    val fixed: Boolean = false,
    val translation: String? = null
) {
    /** Convenience — `original.length`. */
    val length: Int get() = original.length

    /**
     * Returns the (row, col) coordinates of every cell this word
     * occupies, in reading order from the anchor. Drives the
     * selection verifier and the "found" highlight.
     */
    fun cells(): List<Pair<Int, Int>> = List(length) { i ->
        (row + direction.dRow * i) to (col + direction.dCol * i)
    }
}

/**
 * The eight reading directions supported by the word search.
 *
 * The `dRow` / `dCol` pair is the unit step taken for every
 * subsequent letter. The cardinal directions cover the classic
 * horizontal/vertical layouts; the four diagonals let the player
 * find words at any angle. Choosing a direction whose `dCol < 0` or
 * `dRow < 0` places the word *reversed* on the board (the anchor is
 * still the first cell, but the dictionary word now reads from the
 * end of the visual chain back to the anchor).
 */
enum class Direction(val dRow: Int, val dCol: Int) {
    E(0, 1),
    W(0, -1),
    S(1, 0),
    N(-1, 0),
    SE(1, 1),
    NW(-1, -1),
    NE(-1, 1),
    SW(1, -1);

    companion object {
        /** All eight directions — uniform random pick in the generator. */
        val ALL: List<Direction> = entries
    }
}
