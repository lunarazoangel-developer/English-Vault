package com.example.englishvault.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.englishvault.R

/**
 * Static description of a single entry in the bottom navigation bar.
 *
 * Each tab is paired with its [Destination], a Material icon and a
 * localized label. Keeping this list in a single place guarantees the
 * bar order, the [NavHost] graph and the [Destination.bottomBarDestinations]
 * list stay aligned.
 */
data class BottomNavItem(
    val destination: Destination,
    @StringRes val labelRes: Int,
    val icon: ImageVector
)

/**
 * Order of the bottom navigation entries, left-to-right.
 *
 * Intentionally frozen so visual mockups match the implementation.
 */
val bottomNavItems: List<BottomNavItem> = listOf(
    BottomNavItem(
        destination = Destination.Progress,
        labelRes = R.string.nav_progress,
        icon = Icons.Filled.EmojiEvents
    ),
    BottomNavItem(
        destination = Destination.Games,
        labelRes = R.string.nav_games,
        icon = Icons.Filled.SportsEsports
    ),
    BottomNavItem(
        destination = Destination.Test,
        labelRes = R.string.nav_test,
        icon = Icons.Filled.Quiz
    ),
    BottomNavItem(
        destination = Destination.Words,
        labelRes = R.string.nav_words,
        icon = Icons.AutoMirrored.Filled.LibraryBooks
    )
)