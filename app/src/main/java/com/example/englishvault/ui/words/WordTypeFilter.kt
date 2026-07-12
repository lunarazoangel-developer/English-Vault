package com.example.englishvault.ui.words

import androidx.annotation.StringRes
import com.example.englishvault.R
import data.database.entities.WordEntity
import data.database.entities.isUserAdded

/**
 * Canonical set of word buckets used across the app.
 *
 * Each entry maps a [WordEntity] to a single category via [matches].
 * The enum is the single source of truth for:
 *  - The filter chips on the Words screen (`WordListScreen`).
 *  - The per-category progress bars on the Progress screen.
 *  - The keys persisted in `category_progress.categoryKey` (see
 *    [data.database.entities.CategoryProgressEntity]). The string
 *    identifier stored in SQLite is [name], so renaming an entry here
 *    is a breaking change for existing installs.
 *
 * `ALL` and `MINE` are **non-trackable** buckets: they are shown in
 * the Words screen for browsing convenience, but they do not appear in
 * the per-category progression system. The trackable subset is
 * [TRACKED].
 *
 * @property type The literal value of `WordEntity.type` for this
 *   bucket. `null` for the synthetic `ALL` and `MINE` buckets.
 * @property regular The literal value of `WordEntity.regular` for
 *   verb buckets (`true` for regular, `false` for irregular). `null`
 *   for non-verb buckets.
 */
enum class WordTypeFilter(
    @StringRes val labelRes: Int,
    val type: String?,
    val regular: Boolean?,
    val matches: (WordEntity) -> Boolean
) {
    ALL(R.string.words_tab_all, null, null, { true }),

    VERBS_REGULAR(
        R.string.words_tab_regular,
        "verb", true,
        { word -> word.type == "verb" && word.regular == true }
    ),
    VERBS_IRREGULAR(
        R.string.words_tab_irregular,
        "verb", false,
        { word -> word.type == "verb" && word.regular == false }
    ),

    ADJECTIVES(
        R.string.words_tab_adjectives,
        "adjective", null,
        { word -> word.type == "adjective" }
    ),
    ADVERBS(
        R.string.words_tab_adverbs,
        "adverb", null,
        { word -> word.type == "adverb" }
    ),
    NOUNS(
        R.string.words_tab_nouns,
        "noun", null,
        { word -> word.type == "noun" }
    ),
    CONJUNCTIONS(
        R.string.words_tab_conjunctions,
        "conjunction", null,
        { word -> word.type == "conjunction" }
    ),
    PREPOSITIONS(
        R.string.words_tab_prepositions,
        "preposition", null,
        { word -> word.type == "preposition" }
    ),
    INTERJECTIONS(
        R.string.words_tab_interjections,
        "interjection", null,
        { word -> word.type == "interjection" }
    ),

    MINE(R.string.words_tab_mine, null, null, { word -> word.isUserAdded() });

    /**
     * Returns the [WordTypeFilter] that matches [word], or `null` when
     * the word belongs to a type that has no specific bucket
     * (currently impossible — `ALL` matches everything). Useful when
     * you need the category key from a row instead of doing the
     * iteration yourself.
     */
    fun classify(word: WordEntity): WordTypeFilter =
        entries.first { it != ALL && it != MINE && it.matches(word) }

    companion object {
        /**
         * Categories eligible for the per-category progression
         * system. Mirrors the rows seeded into `category_progress`
         * by `Migrations.MIGRATION_6_7`.
         */
        val TRACKED: List<WordTypeFilter> = listOf(
            VERBS_REGULAR,
            VERBS_IRREGULAR,
            ADJECTIVES,
            ADVERBS,
            NOUNS,
            CONJUNCTIONS,
            PREPOSITIONS,
            INTERJECTIONS
        )
    }
}