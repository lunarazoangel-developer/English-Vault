package com.example.englishvault.ui.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.englishvault.ui.components.AppBottomBar
import com.example.englishvault.ui.games.GamesScreen
import com.example.englishvault.ui.navigation.Destination
import com.example.englishvault.ui.progress.ProgressScreen
import com.example.englishvault.ui.test.TestScreen
import com.example.englishvault.ui.words.WordFormScreen
import com.example.englishvault.ui.words.WordListScreen
import com.example.englishvault.ui.words.viewmodel.WordListViewModel

/**
 * Top-level scaffold hosting the bottom navigation bar and the
 * [NavHost] that connects all destinations.
 *
 * Phase 2.5: the Words screen and the Word form destination share the
 * same [WordListViewModel] (scoped to the navigation graph) so that
 *  - the list reflects the Room-backed data set, and
 *  - the form's Save callback persists the new word into Room through
 *    the same VM instance.
 */
@Composable
fun MainScaffold(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Show the bottom bar only on top-level destinations.
    val showBottomBar = currentRoute in Destination.bottomBarDestinations.map { it.route }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(navController = navController)
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
                ProgressScreen()
            }
            composable(Destination.Games.route) {
                GamesScreen()
            }
            composable(Destination.Test.route) {
                TestScreen()
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
                        viewModel.addUserWord(word)
                        navController.popBackStack()
                    }
                )
            }
            // endregion
        }
    }
}