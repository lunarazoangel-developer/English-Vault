package com.example.englishvault.ui.games.lettersoup.util

import com.example.englishvault.ui.games.lettersoup.model.LetterSoupBoard
import com.example.englishvault.ui.games.lettersoup.model.LetterSoupBoard.Companion.DEFAULT_BOARD_SIZE
import com.example.englishvault.ui.games.lettersoup.model.LetterSoupBoard.Companion.EXTENDED_BOARD_SIZE
import com.example.englishvault.ui.games.lettersoup.model.LetterSoupBoard.Companion.EXTENDED_THRESHOLD
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
 * Output: a fully populated [LetterSoupBoard] with up to two target
 * words placed horizontally or vertically, one random letter per word
 * replaced by a soup letter so the player can hunt for the correct
 * replacement, and every remaining cell filled with a weighted random
 * letter. The board side length auto-scales to 10 when any candidate
 * word has 9+ characters.
 *
 * The function is deterministic given a [random] instance, which lets
 * tests and previews pin specific layouts. Production callers should
 * pass `Random.Default` (or a freshly seeded `Random` per run) so
 * playthroughs feel different.
 */
object BoardGenerator {

    /** Probability of placing a word horizontally (vs vertically). */
    private const val HORIZONTAL_PROBABILITY: Double = 0.75

    /** Max placement attempts per word before giving up on it. */
    private const val MAX_PLACEMENT_ATTEMPTS: Int = 100

    /**
     * Builds a board from [pool]. Returns `null` when the pool is
     * empty (the caller should fall back to an empty / error state in
     * that case). Returns a board with **one** placement if the second
     * candidate could not fit on the board after [MAX_PLACEMENT_ATTEMPTS]
     * tries — a partially empty board is still playable.
     *
     * @param pool Upper-cased words to choose from.
     * @param translations Lookup from upper-cased word to its Spanish
     *   translation. Missing entries fall back to `null`.
     * @param random RNG source — pass a seeded `Random` for tests.
     */
    fun generate(
        pool: List<String>,
        translations: Map<String, String?> = emptyMap(),
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

        val chosen = sanitized.shuffled(random).take(LetterSoupGameState.TARGET_PLACEMENTS)
        val boardSize = computeBoardSize(chosen)

        val cells = Array(boardSize) { arrayOfNulls<Char>(boardSize) }
        val placements = mutableListOf<LetterSoupWord>()

        for (word in chosen) {
            val placement = tryPlace(word, cells, boardSize, random) ?: continue
            val wrongIndex = (0 until word.length).random(random)
            for (i in word.indices) {
                val letter = if (i == wrongIndex) {
                    EnglishLetterFrequency.pickDifferentFrom(word[i], random)
                } else {
                    word[i]
                }
                if (placement.horizontal) {
                    cells[placement.row][placement.col + i] = letter
                } else {
                    cells[placement.row + i][placement.col] = letter
                }
            }
            placements.add(
                placement.copy(
                    wrongIndex = wrongIndex,
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
     * Picks the right side length for the chosen words. Anything up to
     * 8 chars uses the default 8×8 grid; 9+ chars forces the 10×10
     * extended layout so the longest word always fits.
     */
    private fun computeBoardSize(words: List<String>): Int {
        val longest = words.maxOfOrNull { it.length } ?: 0
        return if (longest >= EXTENDED_THRESHOLD) EXTENDED_BOARD_SIZE else DEFAULT_BOARD_SIZE
    }

    /**
     * Attempts to find an empty rectangle of size `word.length` on
     * the [cells] grid, respecting the [HORIZONTAL_PROBABILITY] axis
     * bias. Returns `null` if no position fits after
     * [MAX_PLACEMENT_ATTEMPTS] tries.
     */
    private fun tryPlace(
        word: String,
        cells: Array<Array<Char?>>,
        boardSize: Int,
        random: Random
    ): LetterSoupWord? {
        if (word.length > boardSize) return null

        repeat(MAX_PLACEMENT_ATTEMPTS) {
            val horizontal = random.nextDouble() < HORIZONTAL_PROBABILITY
            val row: Int
            val col: Int
            if (horizontal) {
                row = random.nextInt(boardSize)
                col = random.nextInt(boardSize - word.length + 1)
            } else {
                row = random.nextInt(boardSize - word.length + 1)
                col = random.nextInt(boardSize)
            }
            if (canPlace(word, row, col, horizontal, cells)) {
                return LetterSoupWord(
                    original = word,
                    row = row,
                    col = col,
                    horizontal = horizontal
                )
            }
        }
        return null
    }

    /** `true` when the rectangle described by ([row], [col], [horizontal], word length) is empty. */
    private fun canPlace(
        word: String,
        row: Int,
        col: Int,
        horizontal: Boolean,
        cells: Array<Array<Char?>>
    ): Boolean {
        for (i in word.indices) {
            val r = if (horizontal) row else row + i
            val c = if (horizontal) col + i else col
            if (cells[r][c] != null) return false
        }
        return true
    }
}