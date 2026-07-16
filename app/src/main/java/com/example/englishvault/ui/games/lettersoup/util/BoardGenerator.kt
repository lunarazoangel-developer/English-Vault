package com.example.englishvault.ui.games.lettersoup.util

import com.example.englishvault.ui.games.lettersoup.model.Direction
import com.example.englishvault.ui.games.lettersoup.model.LetterSoupBoard
import com.example.englishvault.ui.games.lettersoup.model.LetterSoupGameState
import com.example.englishvault.ui.games.lettersoup.model.LetterSoupWord
import kotlin.random.Random

/**
 * Pure generator for the Letter Soup board.
 *
 * Inputs: a pool of dictionary words (and their Spanish translations)
 * whose lengths all fit on the board. Callers filter by length via
 * `WordDao.getCoreWordsByLengthAndLevel(level, MIN, MAX)`.
 *
 * Output: a fully populated [LetterSoupBoard] with up to
 * [LetterSoupGameState.TARGET_PLACEMENTS] target words placed in any
 * of the eight supported directions (horizontal, vertical, two
 * diagonals, and each in reverse), allowing cells to be **shared**
 * between placements when the overlapping letter matches.
 *
 * The board side length is fixed at
 * [LetterSoupBoard.DEFAULT_BOARD_SIZE] (12) — the word-search layout
 * no longer auto-scales. Every remaining cell is filled with a
 * weighted random letter from [EnglishLetterFrequency] so the soup
 * feels like natural English.
 *
 * The function is deterministic given a [random] instance, which lets
 * tests and previews pin specific layouts. Production callers should
 * pass `Random.Default` (or a freshly seeded `Random` per run) so
 * playthroughs feel different.
 */
object BoardGenerator {

    /** Max placement attempts per word before giving up on it. */
    private const val MAX_PLACEMENT_ATTEMPTS: Int = 400

    /**
     * Builds a board from [pool]. Returns `null` when the pool is
     * empty (the caller should fall back to an empty / error state in
     * that case). Returns a board with fewer placements than
     * [LetterSoupGameState.TARGET_PLACEMENTS] when one of the
     * candidate words could not fit on the board after
     * [MAX_PLACEMENT_ATTEMPTS] tries — a partially full board is
     * still playable.
     *
     * @param pool Upper-cased words to choose from.
     * @param translations Lookup from upper-cased word to its Spanish
     *   translation. Missing entries fall back to `null`.
     * @param wordIds Lookup from upper-cased word to its
     *   [data.database.entities.WordEntity.id]. Required by the
     *   auto-marking pipeline so the VM can persist
     *   `consecutiveCorrect` on every fixed placement. Missing entries
     *   fall back to `0`, which makes the placement invisible to the
     *   auto pipeline (see [LetterSoupWord.wordId] and the `> 0`
     *   guard in `LetterSoupViewModel.applyAutoStatus`).
     * @param random RNG source — pass a seeded `Random` for tests.
     */
    fun generate(
        pool: List<String>,
        translations: Map<String, String?> = emptyMap(),
        wordIds: Map<String, Int> = emptyMap(),
        random: Random = Random.Default
    ): LetterSoupBoard? {
        if (pool.isEmpty()) return null

        val sanitized = pool
            .map { it.uppercase().trim() }
            .filter { word ->
                word.isNotEmpty() &&
                    word.all { it in 'A'..'Z' }
            }
            .distinct()
        if (sanitized.isEmpty()) return null

        val boardSize = LetterSoupBoard.DEFAULT_BOARD_SIZE
        val chosen = sanitized.shuffled(random).take(LetterSoupGameState.TARGET_PLACEMENTS)
        val cells = Array(boardSize) { arrayOfNulls<Char>(boardSize) }
        val placements = mutableListOf<LetterSoupWord>()

        for (word in chosen) {
            val placement = tryPlace(word, cells, boardSize, random) ?: continue
            for (i in word.indices) {
                val (r, c) = placement.cells()[i]
                cells[r][c] = word[i]
            }
            placements.add(
                placement.copy(
                    wordId = wordIds[word] ?: 0,
                    translation = translations[word]
                )
            )
        }

        for (r in 0 until boardSize) {
            for (c in 0 until boardSize) {
                if (cells[r][c] == null) {
                    cells[r][c] = EnglishLetterFrequency.pickSoup(random)
                }
            }
        }

        val grid: List<List<Char>> = cells.map { row ->
            row.map { it ?: EnglishLetterFrequency.pickSoup(random) }
        }
        return LetterSoupBoard(cells = grid, placements = placements, boardSize = boardSize)
    }

    /**
     * Attempts to find an anchor + direction that fits [word] on the
     * [cells] grid. Returns `null` if no position fits after
     * [MAX_PLACEMENT_ATTEMPTS] tries.
     *
     * Overlap is **allowed**: a cell already populated with the same
     * letter is treated as a free match. A cell populated with a
     * different letter blocks the placement, since the chain would
     * visually disagree with the target word.
     */
    private fun tryPlace(
        word: String,
        cells: Array<Array<Char?>>,
        boardSize: Int,
        random: Random
    ): LetterSoupWord? {
        if (word.length > boardSize) return null

        val direction = Direction.ALL.random(random)
        // Valid anchor range: every cell of the word must lie inside
        // the [0, boardSize) square. The anchor itself is the first
        // letter; the last letter sits at
        // (row + (length - 1) * dRow, col + (length - 1) * dCol).
        val rowMin = if (direction.dRow < 0) -(direction.dRow) * (word.length - 1) else 0
        val rowMax = if (direction.dRow > 0) boardSize - direction.dRow * (word.length - 1) - 1
            else boardSize - 1
        val colMin = if (direction.dCol < 0) -(direction.dCol) * (word.length - 1) else 0
        val colMax = if (direction.dCol > 0) boardSize - direction.dCol * (word.length - 1) - 1
            else boardSize - 1
        if (rowMin > rowMax || colMin > colMax) return null

        repeat(MAX_PLACEMENT_ATTEMPTS) {
            val row = random.nextInt(rowMin, rowMax + 1)
            val col = random.nextInt(colMin, colMax + 1)
            if (canPlace(word, row, col, direction, cells)) {
                return LetterSoupWord(
                    original = word,
                    row = row,
                    col = col,
                    direction = direction
                )
            }
        }
        return null
    }

    /**
     * `true` when the chain described by ([row], [col], [direction],
     * word length) is empty or only overlaps with matching letters.
     */
    private fun canPlace(
        word: String,
        row: Int,
        col: Int,
        direction: Direction,
        cells: Array<Array<Char?>>
    ): Boolean {
        for (i in word.indices) {
            val r = row + direction.dRow * i
            val c = col + direction.dCol * i
            val existing = cells[r][c]
            if (existing != null && existing != word[i]) return false
        }
        return true
    }
}
