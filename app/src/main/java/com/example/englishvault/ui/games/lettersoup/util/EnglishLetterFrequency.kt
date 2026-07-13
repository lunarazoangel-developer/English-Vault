package com.example.englishvault.ui.games.lettersoup.util

import kotlin.random.Random

/**
 * Weighted English letter pool used to fill the "soup" cells of the
 * Letter Soup board (i.e. every cell that is not part of an active
 * word) and to pick replacement letters for the one wrong slot per
 * word.
 *
 * Weights mirror the canonical English letter frequency table:
 *  E=13, T=9, A=8, O=8, I=7, N=7, S=6, H=6, R=6, D=4, L=4, C=3, U=3,
 *  M=3, W=2, F=2, G=2, Y=2, P=2, B=2, V=1, K=1, J=1, X=1, Q=1, Z=1.
 *
 * Implementation stores a flat `List<Char>` with each letter repeated
 * by its weight — `pick()` is then a single `random()` call. This is
 * O(1) per pick and avoids the off-by-one bugs of cumulative-weight
 * lookups while staying trivial to read.
 */
object EnglishLetterFrequency {

    /** Per-letter weights in English (must sum to ~100 to feel natural). */
    private val weights: Map<Char, Int> = mapOf(
        'E' to 13, 'T' to 9, 'A' to 8, 'O' to 8, 'I' to 7, 'N' to 7,
        'S' to 6, 'H' to 6, 'R' to 6, 'D' to 4, 'L' to 4, 'C' to 3,
        'U' to 3, 'M' to 3, 'W' to 2, 'F' to 2, 'G' to 2, 'Y' to 2,
        'P' to 2, 'B' to 2, 'V' to 1, 'K' to 1, 'J' to 1, 'X' to 1,
        'Q' to 1, 'Z' to 1
    )

    /** Flattened pool — each letter repeated by its weight. */
    private val pool: List<Char> = buildList {
        weights.forEach { (letter, weight) -> repeat(weight) { add(letter) } }
    }

    /** Picks a random letter from the weighted pool. */
    fun pickSoup(random: Random = Random.Default): Char = pool.random(random)

    /**
     * Picks a random letter from the weighted pool that is **not** the
     * same as [original]. Used to fabricate the single wrong letter
     * per active word so the player can search the soup for the
     * correct replacement.
     *
     * Falls back to any letter if the pool happened to be exhausted
     * (only possible if [original] was the only letter in the pool,
     * which cannot happen with the current weights — guarded by the
     * `?: original` for safety).
     */
    fun pickDifferentFrom(original: Char, random: Random = Random.Default): Char {
        val filtered = pool.filter { it != original.uppercaseChar() }
        return if (filtered.isNotEmpty()) filtered.random(random) else original
    }
}