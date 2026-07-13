package com.example.englishvault.ui.games.lettersoup.model

/**
 * A single Letter Soup board: the grid of letters plus the metadata
 * describing the two active target words on it.
 *
 * The board is **immutable** — every swap rebuilds a fresh instance
 * with the updated cell values. The `placements` list therefore
 * always matches the on-screen layout; the UI never has to reconcile
 * two sources of truth.
 *
 * @property cells `[row][col]` array of letters currently displayed on
 *   the board. Always `boardSize × boardSize`.
 * @property placements Up to two active words; `fixed = true` after
 *   the player swaps the wrong letter away.
 * @property boardSize Side length of the grid — `8` for the standard
 *   case, `10` when the longest word in the placement set has 9 or
 *   more characters. The UI uses this to switch between layouts.
 */
data class LetterSoupBoard(
    val cells: List<List<Char>>,
    val placements: List<LetterSoupWord>,
    val boardSize: Int
) {

    companion object {
        /** Side length of the default grid. */
        const val DEFAULT_BOARD_SIZE: Int = 8

        /** Side length used when words are too long for the default. */
        const val EXTENDED_BOARD_SIZE: Int = 10

        /** Word length threshold that triggers the extended board. */
        const val EXTENDED_THRESHOLD: Int = 9
    }

    /** Letter at ([row], [col]). Throws on out-of-range access. */
    operator fun get(row: Int, col: Int): Char = cells[row][col]

    /**
     * Returns the [LetterSoupCell] role for the cell at ([row], [col]).
     * Out-of-range queries return [LetterSoupCell.Soup] so the UI can
     * render guards without try/catch noise.
     */
    fun roleAt(row: Int, col: Int): LetterSoupCell {
        if (row !in 0 until boardSize || col !in 0 until boardSize) {
            return LetterSoupCell.Soup
        }
        placements.forEach { word ->
            val idx = word.cells().indexOf(row to col)
            if (idx >= 0) {
                return when {
                    word.fixed -> LetterSoupCell.WordFixed
                    idx == word.wrongIndex -> LetterSoupCell.WordWrong
                    else -> LetterSoupCell.WordCorrect
                }
            }
        }
        return LetterSoupCell.Soup
    }

    /** `true` while at least one placement is still active (not fixed). */
    fun hasActiveWords(): Boolean = placements.any { !it.fixed }

    /**
     * Returns the placements that the player has successfully fixed
     * (i.e. their wrong letter has been swapped away).
     */
    fun fixedPlacements(): List<LetterSoupWord> = placements.filter { it.fixed }

    /**
     * Returns the first active placement that still carries a wrong
     * letter. `null` when every active word is already correct.
     */
    fun firstActiveWithWrong(): LetterSoupWord? =
        placements.firstOrNull { !it.fixed && it.wrongIndex >= 0 }
}