package com.example.englishvault.ui.games.wordmatchverbs.util

/**
 * Produces plausible misspelt alternatives for the Word Match Verbs
 * mini-game.
 *
 * The algorithm picks one character position at random and replaces
 * it with a phonetically or visually similar substitute:
 *  - Vowels swap with another vowel (`a` â†” `e` â†” `i` â†” `o` â†” `u`).
 *  - Consonants swap with a phonetically close sibling
 *    (`b` â†” `p`, `d` â†” `t`, `g` â†” `c` â†” `k`, `f` â†” `v`,
 *    `s` â†” `z`, `m` â†” `n`, `l` â†” `r`).
 *
 * The function retries until it has produced [count] unique
 * strings that differ from the [correct] input. A safety cap on the
 * number of attempts avoids infinite loops on tiny inputs.
 */
object DistractorGenerator {

    private val VOWELS: Set<Char> = setOf('a', 'e', 'i', 'o', 'u')

    private val SIMILAR_CONSONANTS: Map<Char, List<Char>> = mapOf(
        'b' to listOf('p', 'v'),
        'p' to listOf('b'),
        'd' to listOf('t'),
        't' to listOf('d'),
        'g' to listOf('c', 'k'),
        'c' to listOf('g', 'k', 's'),
        'k' to listOf('c', 'g'),
        'f' to listOf('v'),
        'v' to listOf('f'),
        's' to listOf('z'),
        'z' to listOf('s'),
        'm' to listOf('n'),
        'n' to listOf('m'),
        'l' to listOf('r'),
        'r' to listOf('l')
    )

    /**
     * Returns up to [count] distinct misspellings of [correct].
     * Returns fewer entries if the input is too short to mutate in
     * [count] unique ways; callers should treat the list as
     * best-effort and pad if absolutely necessary.
     */
    fun generate(correct: String, count: Int): List<String> {
        if (correct.isEmpty() || count <= 0) return emptyList()
        val results = LinkedHashSet<String>()
        val maxAttempts = count * 10
        var attempts = 0
        while (results.size < count && attempts < maxAttempts) {
            attempts++
            val candidate = mutate(correct)
            if (candidate != null && candidate != correct) {
                results.add(candidate)
            }
        }
        return results.toList()
    }

    /**
     * Performs a single random single-character substitution on
     * [word]. Returns `null` if no mutation is possible (e.g. the
     * word contains only characters that have no mapping).
     */
    private fun mutate(word: String): String? {
        val positions = word.indices.toMutableList().apply { shuffle() }
        for (pos in positions) {
            val original = word[pos].lowercaseChar()
            val replacement: Char? = when {
                original in VOWELS -> VOWELS.filter { it != original }.randomOrNull()
                else -> SIMILAR_CONSONANTS[original]?.randomOrNull()
            }
            if (replacement != null) {
                return word.replaceRange(pos, pos + 1, replacement.toString())
            }
        }
        return null
    }
}
