package com.example.englishvault.ui.navigation

/**
 * Sealed hierarchy describing every navigation destination in the app.
 *
 * Phase 6 keeps navigation intentionally simple: four bottom-bar
 * entries plus the Word CRUD flow and the Word Match Verbs
 * mini-game flow.
 */
sealed class Destination(val route: String) {

    // region: Bottom-bar tabs
    data object Progress : Destination("progress")
    data object Games : Destination("games")
    data object Test : Destination("test")
    data object Words : Destination("words")
    // endregion

    // region: Word CRUD
    data object WordForm : Destination("words/form?wordId={wordId}") {
        const val ARG_WORD_ID: String = "wordId"
        fun buildRoute(wordId: Int? = null): String =
            if (wordId == null) "words/form?wordId=-1"
            else "words/form?wordId=$wordId"
    }
    // endregion

    // region: Word Match Verbs mini-game
    /** Level selector. */
    data object WordMatchLevel : Destination("games/wordmatch/level")

    /** Active game. Carries the chosen level in the route. The
     *  finished state is rendered in place inside this same
     *  destination, so there is no separate "end" route. */
    data object WordMatchPlay : Destination("games/wordmatch/play?level={level}") {
        const val ARG_LEVEL: String = "level"
        fun buildRoute(level: Int): String = "games/wordmatch/play?level=$level"
    }
    // endregion

    companion object {
        /** Top-level routes rendered as tabs in the bottom bar. */
        val bottomBarDestinations: List<Destination> = listOf(
            Progress,
            Games,
            Test,
            Words
        )
    }
}