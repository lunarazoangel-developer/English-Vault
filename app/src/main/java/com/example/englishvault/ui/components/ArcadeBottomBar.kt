package com.example.englishvault.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.englishvault.ui.navigation.BottomNavItem
import com.example.englishvault.ui.navigation.bottomNavItems
import com.example.englishvault.ui.progress.arcade.ArcadeFonts
import com.example.englishvault.ui.progress.arcade.ArcadePalettes

/**
 * Bottom navigation bar for the app, rendered in the arcade style.
 *
 * Always uses the [ArcadePalettes.Dark] palette — the bar is a fixed
 * chrome that does not respond to the global light / dark theme
 * toggle. The rest of the app flips between dark and light arcade
 * via the [com.example.englishvault.ui.progress.arcade.LocalArcadePalette]
 * provided at the root of the Compose tree; this bar reads from
 * the static dark instance directly so the user always sees the
 * same anchor regardless of their theme choice.
 *
 * The active tab is rendered as a flat (no shadow) pink pill
 * with the label and icon in dark ink. Inactive tabs use the
 * surface-dark color with dim text. Tapping a tab navigates
 * using single-top + restore-state semantics so the back stack
 * behaves like a typical bottom-bar app.
 */
@Composable
fun ArcadeBottomBar(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    items: List<BottomNavItem> = bottomNavItems
) {
    val palette = ArcadePalettes.Dark
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(palette.surface)
            .border(width = 1.dp, color = palette.border, shape = RoundedCornerShape(0.dp)),
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
 * Single tab in [ArcadeBottomBar]. Vertical layout: 24 dp icon on
 * top, 2 dp spacer, 8 sp pixel-font label below. Inactive tabs
 * are flat (no fill); the active tab is a flat pink pill with
 * dark ink text and icon — no shadow per the design decision.
 */
@Composable
private fun ArcadeBottomBarItem(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = ArcadePalettes.Dark
    val container = if (selected) palette.primary else palette.surface
    val content = if (selected) palette.ink else palette.textDim

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .padding(horizontal = 4.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = stringResource(id = item.labelRes),
                tint = content,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.size(2.dp))
            Text(
                text = stringResource(id = item.labelRes),
                color = content,
                fontFamily = ArcadeFonts.Pixel,
                fontWeight = ArcadeFonts.PixelWeight,
                fontSize = 8.sp,
                letterSpacing = 1.sp
            )
        }
    }
}
