package com.example.englishvault.ui.navigation

/**
 * Sealed hierarchy describing every navigation destination in the app.
 *
 * Phase 2 keeps navigation intentionally simple: four bottom-bar entries
 * plus a Word form destination for the Add/Edit flow. Routes are built
 * once and reused by [NavHost] and the bottom bar to stay in sync.
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