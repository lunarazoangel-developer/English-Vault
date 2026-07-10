package com.example.englishvault.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.englishvault.ui.navigation.BottomNavItem
import com.example.englishvault.ui.navigation.bottomNavItems

/**
 * Bottom navigation bar for the app.
 *
 * Highlights the active route by matching against the current back-stack
 * entry's hierarchy. Tapping a tab navigates using single-top + restore
 * state semantics so the back stack behaves like a typical bottom-bar app.
 *
 * @param navController Controller used to switch destinations.
 * @param modifier Optional [Modifier] for layout adjustments.
 * @param items Tab definitions. Defaults to [bottomNavItems].
 */
@Composable
fun AppBottomBar(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    items: List<BottomNavItem> = bottomNavItems
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(modifier = modifier) {
        items.forEach { item ->
            val selected = currentDestination?.hierarchy
                ?.any { it.route == item.destination.route } == true

            NavigationBarItem(
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
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = stringResource(id = item.labelRes)
                    )
                },
                label = { Text(text = stringResource(id = item.labelRes)) },
                colors = NavigationBarItemDefaults.colors()
            )
        }
    }
}