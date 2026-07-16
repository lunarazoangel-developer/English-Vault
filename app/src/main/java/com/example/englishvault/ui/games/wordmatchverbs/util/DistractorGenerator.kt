package com.example.englishvault.ui.games.wordmatchverbs.util

/**
 * Produces plausible misspellings — or, deliberately, plausible-looking
 * **invented verb forms** — for the Word Match Verbs mini-game.
 *
 * Three families of strategies are mixed at random for every attempt so
 * the four options presented to the player feel varied and resist
 * pattern-matching:
 *
 *  1. **Single-character substitution** (typos): one character is
 *     swapped for a phonetically or visually similar neighbour
 *     (`a`↔`e`↔`i`↔`o`↔`u`, `b`↔`p`, `d`↔`t`, `g`↔`c`↔`k`,
 *     `f`↔`v`, `s`↔`z`, `m`↔`n`, `l`↔`r`).
 *  2. **Forced regularisation**: the [correct] form is re-cast as a
 *     regular past / past-participle by appending `-ed` (or `-d` if
 *     the input already ends in `e`) or `-en`. This catches irregular
 *     verbs whose correct past form is unique
 *     (`be`→`beed`, `go`→`goed`, `run`→`runed`, `take`→`taken`).
 *  3. **Forced irregularisation**: regular verbs get an irregular-style
 *     suffix (`-ought`, `-ain`, `-ept`, `-oke`, `-ung`, `-own`) and
 *     sometimes a vowel mutation, mimicking the shape of true
 *     irregulars without producing any real word
 *     (`ask`→`askought`, `ask`→`askain`, `walk`→`wolk`, `help`→`holpe`).
 *
 * A double-mutation pass applies strategy 1 on top of strategy 2 to
 * produce compounds like `beed`→`baed`, `goed`→`gaed`.
 *
 * The function retries until it has produced [count] unique
 * candidates that survive the filter (see below). A safety cap on the
 * number of attempts avoids infinite loops on tiny inputs.
 *
 * ## Filters
 *
 * Every candidate is dropped (case-insensitively) when it matches:
 *  - the [correct] form itself,
 *  - the [baseWord] (the verb root shown on the prompt card), so the
 *    player never sees the infinitive masquerading as a distractor,
 *  - the [otherValidForm] (e.g. the real past participle when the
 *    question asks for the past simple, or vice-versa), so the
 *    "wrong" options are not the other correct conjugation of the
 *    same verb.
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
     * Suffixes borrowed from real English irregulars — used here to
     * dress up regular verbs as invented irregulars. Kept short and
     * familiar so the player pauses before answering.
     */
    private val IRREGULAR_SUFFIXES: List<String> = listOf(
        "ought", "ain", "ept", "oke", "ung", "own"
    )

    /**
     * Returns up to [count] distinct distractor strings for [correct].
     *
     * @param correct The correct answer the player must pick.
     * @param count Maximum number of distractors to produce. The
     *   returned list is shorter only when the input is too short to
     *   mutate in [count] unique ways before the safety cap is hit.
     * @param baseWord The verb in its dictionary root form. Distractors
     *   equal to this are filtered out so the prompt verb never
     *   appears as an option.
     * @param otherValidForm The other valid conjugation of [baseWord]
     *   (e.g. the past participle when [correct] is the past simple).
     *   Distractors equal to this are filtered out so the player does
     *   not have to choose between two real correct answers. Pass
     *   `null` when the verb only has one recorded form, when [correct]
     *   falls back to the base word, or when the other form is
     *   unknown.
     */
    fun generate(
        correct: String,
        count: Int,
        baseWord: String? = null,
        otherValidForm: String? = null
    ): List<String> {
        if (correct.isEmpty() || count <= 0) return emptyList()
        val correctLc = correct.lowercase()
        val baseLc = baseWord?.lowercase()
        val otherLc = otherValidForm?.lowercase()

        val results = LinkedHashSet<String>()
        val maxAttempts = count * 10
        var attempts = 0
        val strategies = STRATEGIES
        while (results.size < count && attempts < maxAttempts) {
            attempts++
            val strategy = strategies.random()
            val candidate = strategy(correct) ?: continue
            if (candidate.isBlank()) continue
            val lc = candidate.lowercase()
            if (lc == correctLc) continue
            if (baseLc != null && lc == baseLc) continue
            if (otherLc != null && lc == otherLc) continue
            results.add(candidate)
        }
        return results.toList()
    }

    /**
     * Strategy list. Held as a `List<(String) -> String?>` so the main
     * loop can pick a random strategy per attempt and future
     * strategies can be added without touching the loop body.
     */
    private val STRATEGIES: List<(String) -> String?> = listOf(
        ::substituteChar,
        ::regularize,
        ::regularize,
        ::regularizeEn,
        ::irregularize,
        ::irregularizeWithVowelShift,
        ::doubleMutation
    )

    /**
     * Replaces a single character with a phonetically or visually
     * similar neighbour. Returns `null` when no position has a
     * substitution mapping (e.g. all-symbol input).
     */
    private fun substituteChar(word: String): String? {
        val positions = word.indices.toMutableList().apply { shuffle() }
        for (pos in positions) {
            val original = word[pos].lowercaseChar()
            val replacement: Char? = when {
                original in VOWELS -> VOWELS.filter { it != original }.randomOrNull()
                else -> SIMILAR_CONSONANTS[original]?.randomOrNull()
            }
            if (replacement != null) {
                val mapped = if (word[pos].isUpperCase()) {
                    replacement.uppercaseChar()
                } else {
                    replacement
                }
                return word.replaceRange(pos, pos + 1, mapped.toString())
            }
        }
        return null
    }

    /**
     * Re-casts the word as a regular past / past-participle by
     * appending `-ed` (or `-d` if it already ends in `e`).
     */
    private fun regularize(word: String): String? {
        if (word.length < 2) return null
        val suffix = if (word.last().lowercaseChar() == 'e') "d" else "ed"
        return word + suffix
    }

    /**
     * Past-participle flavour of [regularize]: appends `-en`. The
     * resulting forms are not real verbs but read as plausible
     * irregulars (e.g. `go`→`goen`).
     */
    private fun regularizeEn(word: String): String? {
        if (word.length < 2) return null
        return word + "en"
    }

    /**
     * Appends one of the recorded irregular suffixes to dress the
     * word up as an invented irregular past / past-participle.
     */
    private fun irregularize(word: String): String? {
        if (word.length < 2) return null
        val suffix = IRREGULAR_SUFFIXES.random()
        return word + suffix
    }

    /**
     * Shifts the last vowel to a different vowel, then appends an
     * irregular suffix. Mimics the shape of true vowel-changing
     * irregulars (`ask`→`eskought`, `help`→`holpe`).
     */
    private fun irregularizeWithVowelShift(word: String): String? {
        if (word.length < 2) return null
        val lastVowelIdx = word.indexOfLast { it.lowercaseChar() in VOWELS }
        if (lastVowelIdx < 0) return null
        val original = word[lastVowelIdx].lowercaseChar()
        val replacement = VOWELS.filter { it != original }.randomOrNull() ?: return null
        val mapped = if (word[lastVowelIdx].isUpperCase()) {
            replacement.uppercaseChar()
        } else {
            replacement
        }
        val shifted = word.replaceRange(lastVowelIdx, lastVowelIdx + 1, mapped.toString())
        val suffix = IRREGULAR_SUFFIXES.random()
        return shifted + suffix
    }

    /**
     * Applies [regularize] first and then runs [substituteChar] on the
     * result, producing forms like `beed`→`baed`, `goed`→`goad`.
     * The double pass is what makes the distractors feel like typos
     * the player might actually fall for.
     */
    private fun doubleMutation(word: String): String? {
        val first = regularize(word) ?: return null
        return substituteChar(first)
    }
}
