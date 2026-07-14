package com.example.englishvault.ui.games.common

/**
 * Game-mode flag for mini-games that ship a `WORLD` variant.
 *
 * Lives in the `common` package so the Word Match Verbs mini-game
 * (which originally declared it inline next to its state machine)
 * and the new Listening mini-game can share the same enum without
 * depending on each other.
 *
 * The dev toggle button on each play screen flips the active mode
 * between [NORMAL] and [WORLD] without requiring navigation. Mini-games
 * freeze the chosen value into their per-run state so the UI can
 * branch on the mode without re-reading the ViewModel.
 */
enum class GameMode { NORMAL, WORLD }