package com.example.englishvault.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.englishvault.ui.navigation.BottomNavItem
import com.example.englishvault.ui.navigation.bottomNavItems
import com.example.englishvault.ui.progress.arcade.LocalArcadePalette

/**
 * Bottom navigation bar for the app, rendered in the arcade style.
 *
 * Reads the active palette from [LocalArcadePalette] so the bar
 * follows the user's light / dark theme choice. The previous version
 * hardcoded [com.example.englishvault.ui.progress.arcade.ArcadePalettes.Dark]
 * to act as "fixed chrome" — that contract was dropped so the bar
 * stays consistent with the rest of the UI after the palette rework.
 *
 * The bar carries no visible border. Round displays (Wear OS or any
 * device with a circular bezel) used to clip the rectangular outline
 * and leave a visible seam along the curve; dropping the border
 * removes that artefact and the bar reads as a clean coloured strip
 * against the screen background on every shape.
 *
 * Each tab is a single centred icon (no text label). TalkBack still
 * announces the tab name via the icon's `contentDescription`, which
 * is sourced from the same [BottomNavItem.labelRes] string the old
 * layout rendered.
 *
 * Tapping a tab navigates using single-top + restore-state semantics
 * so the back stack behaves like a typical bottom-bar app.
 */
@Composable
fun ArcadeBottomBar(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    items: List<BottomNavItem> = bottomNavItems
) {
    val palette = LocalArcadePalette.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(palette.surface),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items.forEach { item ->
            val selected = currentDestination?.hierarchy
                ?.any { it.route == item.destination.route } == true
            ArcadeBottomBarItem(
                item = item,
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(item.destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}

/**
 * Single tab in [ArcadeBottomBar]. The pill is bigger now that there
 * is no label to share the space with: 28 dp icon, 10 dp vertical
 * padding inside the rounded container.
 *
 * Each tab keeps its own visual identity even when inactive: the
 * pill flips to the tab's [BottomNavItem.arcadeAccent] when selected
 * (with dark ink text), and the icon of an inactive tab is tinted
 * with a 50/50 blend between the accent and the palette's
 * `textDim` so every entry still hints at its own colour while
 * reading as "off".
 */
@Composable
private fun ArcadeBottomBarItem(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalArcadePalette.current
    val container = if (selected) item.arcadeAccent else palette.surface
    val content = if (selected) {
        palette.ink
    } else {
        lerp(item.arcadeAccent, palette.textDim, 0.5f)
    }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = stringResource(id = item.labelRes),
            tint = content,
            modifier = Modifier.size(28.dp)
        )
    }
}