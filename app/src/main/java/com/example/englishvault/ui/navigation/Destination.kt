package com.example.englishvault.ui.navigation

/**
 * Sealed hierarchy describing every navigation destination in the app.
 *
 * Phase 7: the "Test" tab has been replaced by a beta "World" map
 * (Duolingo-style path selector reimagined as a Super Mario Bros
 * level map). The remaining bottom-bar entries stay aligned with
 * [com.example.englishvault.ui.navigation.bottomNavItems].
 */
sealed class Destination(val route: String) {

    // region: Bottom-bar tabs
    data object Progress : Destination("progress")
    data object World : Destination("world")
    data object Games : Destination("games")
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
    data object WordMatchVerbsLevel : Destination("games/wordmatchverbs/level")

    /** Active game. Carries the chosen level in the route. The
     *  finished state is rendered in place inside this same
     *  destination, so there is no separate "end" route. */
    data object WordMatchVerbsPlay : Destination("games/wordmatchverbs/play?level={level}") {
        const val ARG_LEVEL: String = "level"
        fun buildRoute(level: Int): String = "games/wordmatchverbs/play?level=$level"
    }
    // endregion

    // region: Settings (Phase 7.1)
    /** Settings hub — profile + sound. Reachable from the Progress
     *  screen via the greeting button. */
    data object Settings : Destination("settings")

    /** Sub-screen that lets the user rename their profile. */
    data object SettingsEditName : Destination("settings/edit-name")
    // endregion

    companion object {
        /** Top-level routes rendered as tabs in the bottom bar. */
        val bottomBarDestinations: List<Destination> = listOf(
            Progress,
            World,
            Games,
            Words
        )
    }
}