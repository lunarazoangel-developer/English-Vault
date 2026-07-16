package com.example.englishvault.ui.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.englishvault.ui.components.ArcadeBottomBar
import com.example.englishvault.ui.games.GamesScreen
import com.example.englishvault.ui.games.lettersoup.LetterSoupGameScreen
import com.example.englishvault.ui.games.lettersoup.LetterSoupLevelScreen
import com.example.englishvault.ui.games.listening.ListeningGameScreen
import com.example.englishvault.ui.games.listening.ListeningLevelScreen
import com.example.englishvault.ui.games.wordmatchverbs.WordMatchVerbsGameScreen
import com.example.englishvault.ui.games.wordmatchverbs.WordMatchVerbsLevelScreen
import com.example.englishvault.ui.navigation.Destination
import com.example.englishvault.ui.progress.ProgressScreen
import com.example.englishvault.ui.progress.arcade.ArcadePalettes
import com.example.englishvault.ui.progress.arcade.LocalArcadePalette
import com.example.englishvault.ui.settings.SettingsEditNameScreen
import com.example.englishvault.ui.settings.SettingsScreen
import com.example.englishvault.ui.world.WorldScreen
import com.example.englishvault.ui.words.WordFormScreen
import com.example.englishvault.ui.words.WordListScreen
import com.example.englishvault.ui.words.viewmodel.WordListViewModel
import data.database.entities.UserProfileEntity

/**
 * Top-level scaffold hosting the bottom navigation bar and the
 * [NavHost] that connects all destinations.
 *
 * Phase 8.x: accepts the persisted [themeMode] and provides the
 * matching [com.example.englishvault.ui.progress.arcade.ArcadePalette]
 * to every arcade-aware child via
 * [LocalArcadePalette]. The bottom bar deliberately ignores the
 * CompositionLocal and uses [ArcadePalettes.Dark] directly so it
 * stays a fixed chrome across both theme variants.
 *
 * Phase 2.5: the Words screen and the Word form destination share
 * the same [WordListViewModel] (scoped to the navigation graph).
 *
 * Phase 6: the Word Match Verbs mini-game is reachable from
 * [GamesScreen] and walks the user through
 * `WordMatchVerbsLevel → WordMatchVerbsPlay → WordMatchVerbsEnd`.
 */
@Composable
fun MainScaffold(
    themeMode: String,
    modifier: Modifier = Modifier
) {
    val palette = if (themeMode == UserProfileEntity.THEME_MODE_DARK) {
        ArcadePalettes.Dark
    } else {
        ArcadePalettes.Light
    }

    CompositionLocalProvider(LocalArcadePalette provides palette) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val showBottomBar = currentRoute in Destination.bottomBarDestinations.map { it.route }

        Scaffold(
            modifier = modifier,
            bottomBar = {
                if (showBottomBar) {
                    ArcadeBottomBar(navController = navController)
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Destination.Progress.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                // region: Bottom-bar destinations
                composable(Destination.Progress.route) {
                    ProgressScreen(
                        onOpenSettings = {
                            navController.navigate(Destination.Settings.route)
                        }
                    )
                }
                composable(Destination.Games.route) {
                    GamesScreen(
                        onOpenWordMatchVerbs = {
                            navController.navigate(Destination.WordMatchVerbsLevel.route)
                        },
                        onOpenLetterSoup = {
                            navController.navigate(Destination.LetterSoupLevel.route)
                        },
                        onOpenListening = {
                            navController.navigate(Destination.ListeningLevel.route)
                        }
                    )
                }
                composable(Destination.World.route) {
                    WorldScreen()
                }
                composable(Destination.Words.route) {
                    WordListScreen(
                        onAddWord = {
                            navController.navigate(Destination.WordForm.buildRoute())
                        },
                        onEditWord = { id ->
                            navController.navigate(Destination.WordForm.buildRoute(id))
                        }
                    )
                }
                // endregion

                // region: Settings (Phase 7.1)
                composable(Destination.Settings.route) {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onEditName = {
                            navController.navigate(Destination.SettingsEditName.route)
                        }
                    )
                }
                composable(Destination.SettingsEditName.route) {
                    SettingsEditNameScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                // endregion

                // region: Word CRUD
                composable(
                    route = Destination.WordForm.route,
                    arguments = listOf(
                        navArgument(Destination.WordForm.ARG_WORD_ID) {
                            type = NavType.IntType
                            defaultValue = -1
                        }
                    )
                ) { entry ->
                    val wordId = entry.arguments?.getInt(Destination.WordForm.ARG_WORD_ID)
                    val effectiveId = if (wordId == null || wordId < 0) null else wordId

                    // Shared ViewModel scoped to this back-stack entry so
                    // both the list and the form operate on the same data.
                    val viewModel: WordListViewModel = hiltViewModel()

                    WordFormScreen(
                        wordId = effectiveId,
                        onBack = { navController.popBackStack() },
                        onSave = { word ->
                            // Route to insert vs update based on whether the
                            // form was opened in edit mode. The form itself
                            // decides whether the id is preserved.
                            if (effectiveId != null) {
                                viewModel.updateUserWord(word)
                            } else {
                                viewModel.addUserWord(word)
                            }
                            navController.popBackStack()
                        }
                    )
                }
                // endregion

                // region: Word Match Verbs mini-game
                composable(Destination.WordMatchVerbsLevel.route) {
                    WordMatchVerbsLevelScreen(
                        onBack = { navController.popBackStack() },
                        onLevelChosen = { level ->
                            navController.navigate(Destination.WordMatchVerbsPlay.buildRoute(level))
                        }
                    )
                }
                composable(
                    route = Destination.WordMatchVerbsPlay.route,
                    arguments = listOf(
                        navArgument(Destination.WordMatchVerbsPlay.ARG_LEVEL) {
                            type = NavType.IntType
                            defaultValue = 1
                        }
                    )
                ) { backStackEntry ->
                    val level = backStackEntry.arguments
                        ?.getInt(Destination.WordMatchVerbsPlay.ARG_LEVEL) ?: 1
                    WordMatchVerbsGameScreen(
                        level = level,
                        onBack = { navController.popBackStack() },
                        onExitToGames = {
                            navController.popBackStack(
                                route = Destination.Games.route,
                                inclusive = false
                            )
                        }
                    )
                }
                // endregion

                // region: Letter Soup mini-game
                composable(Destination.LetterSoupLevel.route) {
                    LetterSoupLevelScreen(
                        onBack = { navController.popBackStack() },
                        onLevelChosen = { level ->
                            navController.navigate(Destination.LetterSoupPlay.buildRoute(level))
                        }
                    )
                }
                composable(
                    route = Destination.LetterSoupPlay.route,
                    arguments = listOf(
                        navArgument(Destination.LetterSoupPlay.ARG_LEVEL) {
                            type = NavType.IntType
                            defaultValue = 1
                        }
                    )
                ) { backStackEntry ->
                    val level = backStackEntry.arguments
                        ?.getInt(Destination.LetterSoupPlay.ARG_LEVEL) ?: 1
                    LetterSoupGameScreen(
                        level = level,
                        onBack = { navController.popBackStack() },
                        onExitToGames = {
                            navController.popBackStack(
                                route = Destination.Games.route,
                                inclusive = false
                            )
                        }
                    )
                }
                // endregion

                // region: Listening mini-game (Phase 7.5)
                composable(Destination.ListeningLevel.route) {
                    ListeningLevelScreen(
                        onBack = { navController.popBackStack() },
                        onLevelChosen = { level ->
                            navController.navigate(Destination.ListeningPlay.buildRoute(level))
                        }
                    )
                }
                composable(
                    route = Destination.ListeningPlay.route,
                    arguments = listOf(
                        navArgument(Destination.ListeningPlay.ARG_LEVEL) {
                            type = NavType.IntType
                            defaultValue = 1
                        }
                    )
                ) { backStackEntry ->
                    val level = backStackEntry.arguments
                        ?.getInt(Destination.ListeningPlay.ARG_LEVEL) ?: 1
                    ListeningGameScreen(
                        level = level,
                        onBack = { navController.popBackStack() },
                        onExitToGames = {
                            navController.popBackStack(
                                route = Destination.Games.route,
                                inclusive = false
                            )
                        }
                    )
                }
                // endregion
            }
        }
    }
}
