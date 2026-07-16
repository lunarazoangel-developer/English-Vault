package com.example.englishvault.ui.games.lettersoup.model

/**
 * Visual role of a single cell on the Letter Soup board.
 *
 * The grid renders the same way for every variant (rounded square,
 * letter colour, optional border). The role drives the small
 * decorative differences:
 *  - [Soup] cells have no border and no special treatment; the
 *    default in a word search.
 *  - [InSelection] cells belong to the chain the player is currently
 *    underlining. They get a coloured border so the in-progress
 *    selection is visible before the player commits.
 *  - [WordFixed] cells belong to a placement the player has already
 *    found. They keep a green-tinted border for the rest of the run
 *    so the victory is visible at a glance.
 *
 * The old "wrong letter" / "correct letter" roles are gone — the
 * word search mechanic never reveals which cells belong to an
 * unfound word, so a cell is either soup, in the active selection,
 * or already part of a found word.
 */
enum class LetterSoupCell {
    /** Not part of any target word (or part of one the player has not yet found). */
    Soup,

    /** Cell is part of the chain the player is currently underlining. */
    InSelection,

    /** Cell belongs to a placement the player has already found. */
    WordFixed
}
