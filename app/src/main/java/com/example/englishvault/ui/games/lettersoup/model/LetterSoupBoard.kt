package com.example.englishvault.ui.games.lettersoup.model

/**
 * A single Letter Soup board: the grid of letters plus the metadata
 * describing the target words on it.
 *
 * The board is **immutable** — every state change (selection update,
 * found flag flip) rebuilds a fresh instance with the updated cell
 * values. The `placements` list therefore always matches the on-screen
 * layout; the UI never has to reconcile two sources of truth.
 *
 * @property cells `[row][col]` array of letters currently displayed on
 *   the board. Always `boardSize × boardSize`.
 * @property placements Target words hidden in the grid. `fixed = true`
 *   after the player underlines the entire word.
 * @property boardSize Side length of the grid. Always
 *   [DEFAULT_BOARD_SIZE] (12) for the standard mini-game; the
 *   word-search layout no longer auto-scales.
 */
data class LetterSoupBoard(
    val cells: List<List<Char>>,
    val placements: List<LetterSoupWord>,
    val boardSize: Int
) {

    /** Letter at ([row], [col]). Throws on out-of-range access. */
    operator fun get(row: Int, col: Int): Char = cells[row][col]

    /**
     * Returns the [LetterSoupCell] role for the cell at ([row], [col]).
     *
     * The role is computed against the caller-supplied [selectedCells]
     * (so this composable function stays pure) and the placement list.
     * Out-of-range queries return [LetterSoupCell.Soup] so the UI can
     * render guards without try/catch noise.
     *
     * Order of checks:
     *  1. Active selection → [LetterSoupCell.InSelection].
     *  2. Found placement → [LetterSoupCell.WordFixed].
     *  3. Otherwise → [LetterSoupCell.Soup].
     */
    fun roleAt(
        row: Int,
        col: Int,
        selectedCells: List<Pair<Int, Int>> = emptyList()
    ): LetterSoupCell {
        if (row !in 0 until boardSize || col !in 0 until boardSize) {
            return LetterSoupCell.Soup
        }
        if (selectedCells.contains(row to col)) {
            return LetterSoupCell.InSelection
        }
        placements.forEach { word ->
            if (word.fixed && row to col in word.cells()) {
                return LetterSoupCell.WordFixed
            }
        }
        return LetterSoupCell.Soup
    }

    /** `true` while at least one placement is still active (not fixed). */
    fun hasActiveWords(): Boolean = placements.any { !it.fixed }

    /**
     * Returns the placements that the player has successfully found.
     */
    fun fixedPlacements(): List<LetterSoupWord> = placements.filter { it.fixed }

    /**
     * `true` when the cell at ([row], [col]) belongs to a placement
     * the player has already found. Used by the UI to render the
     * green "found" border independently of the role lookup.
     */
    fun isFoundAt(row: Int, col: Int): Boolean =
        placements.any { it.fixed && (row to col) in it.cells() }

    /**
     * Returns the first placement that is still hidden (not fixed).
     * `null` when every placement has been found.
     */
    fun firstUnfixedPlacement(): LetterSoupWord? =
        placements.firstOrNull { !it.fixed }

    companion object {
        /** Side length of the grid. The mini-game always uses 12×12. */
        const val DEFAULT_BOARD_SIZE: Int = 12
    }
}
