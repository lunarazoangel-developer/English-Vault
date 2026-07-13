package com.example.englishvault.ui.games.lettersoup.model

/**
 * Visual role of a single cell on the Letter Soup board.
 *
 * The grid is rendered the same way for every variant (rounded
 * square, letter colour, optional border). The role drives the small
 * decorative differences:
 *  - [Soup] cells have no border.
 *  - [WordCorrect] cells (a letter that belongs to an active word and
 *    is not the wrong one) get a white border to signal "this is part
 *    of a target word".
 *  - [WordWrong] cells carry the wrong letter — white border **plus**
 *    a red ❌ badge and the pulse animation.
 *  - [WordFixed] cells belong to a word the player has already fixed
 *    — same border style but tinted green so the player can see the
 *    victory at a glance until the board regenerates.
 */
enum class LetterSoupCell {
    /** Not part of any target word. */
    Soup,

    /** Part of an active target word, correct letter. */
    WordCorrect,

    /** Part of an active target word, the one wrong letter. */
    WordWrong,

    /** Part of a word the player has already fixed. */
    WordFixed
}