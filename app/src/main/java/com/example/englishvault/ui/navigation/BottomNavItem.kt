package com.example.englishvault.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.englishvault.R

/**
 * Static description of a single entry in the bottom navigation bar.
 *
 * Each tab is paired with its [Destination], a Material icon, a
 * localized label and an [arcadeAccent] colour drawn from the
 * arcade category palette (`ArcadePalette.categoryColor`). The bar
 * uses [arcadeAccent] as the active pill colour and to tint the
 * icon of an inactive tab so every entry keeps its own visual
 * identity even when not selected.
 *
 * Keeping this list in a single place guarantees the bar order,
 * the [NavHost] graph and the [Destination.bottomBarDestinations]
 * list stay aligned.
 *
 * @property arcadeAccent Solid colour that identifies this tab. Pick
 *   from the eight grammatical category hues in
 *   [com.example.englishvault.ui.progress.arcade.ArcadePalette.categoryColor]
 *   so the bottom bar matches the dashboard and the Games grid.
 */
data class BottomNavItem(
    val destination: Destination,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val arcadeAccent: Color
)

/**
 * Order of the bottom navigation entries, left-to-right.
 *
 * Phase 7: the "Test" tab was replaced by the beta "World" map, which
 * sits between Progress and Games.
 *
 * Accent mapping (chosen so the four tabs read as a coherent arcade
 * set without any two of them sharing a hue):
 *  - **Progress** → gold (ADJECTIVES hue) — trophy / reward.
 *  - **World**    → pink (VERBS hue) — playful world map.
 *  - **Games**    → cyan (NOUNS hue) — energy / arcade cabinet.
 *  - **Words**    → green (INTERJECTIONS hue) — growth / library.
 */
val bottomNavItems: List<BottomNavItem> = listOf(
    BottomNavItem(
        destination = Destination.Progress,
        labelRes = R.string.nav_progress,
        icon = Icons.Filled.EmojiEvents,
        arcadeAccent = Color(0xFFFFD700)
    ),
    BottomNavItem(
        destination = Destination.World,
        labelRes = R.string.nav_world,
        icon = Icons.Filled.Public,
        arcadeAccent = Color(0xFFFF007A)
    ),
    BottomNavItem(
        destination = Destination.Games,
        labelRes = R.string.nav_games,
        icon = Icons.Filled.SportsEsports,
        arcadeAccent = Color(0xFF00D4FF)
    ),
    BottomNavItem(
        destination = Destination.Words,
        labelRes = R.string.nav_words,
        icon = Icons.AutoMirrored.Filled.LibraryBooks,
        arcadeAccent = Color(0xFF5FB878)
    )
)