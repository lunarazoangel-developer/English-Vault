package com.example.englishvault.ui.words

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.englishvault.R
import com.example.englishvault.ui.words.components.WordCard
import com.example.englishvault.ui.words.viewmodel.WordListViewModel
import data.database.entities.WordEntity

/**
 * Words screen — list of vocabulary entries with add / edit / delete
 * actions.
 *
 * Phase 2.5: data flows from Room through [WordListViewModel] instead
 * of an in-memory mock. The screen exposes four tabs that filter the
 * list by grammatical type and source.
 *
 * Default / seeded entries (`source == "core"`) are read-only:
 *  - The screen never displays edit or delete controls for them.
 *  - The ViewModel also re-checks [WordEntity.isUserAdded] before
 *    deleting so a stale dialog cannot wipe protected rows.
 *
 * @param onAddWord Called when the FAB is tapped.
 * @param onEditWord Called with the word id when the user taps the pencil icon.
 * @param modifier Optional [Modifier] for layout adjustments.
 */
@Composable
fun WordListScreen(
    onAddWord: () -> Unit,
    onEditWord: (id: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WordListViewModel = hiltViewModel()
) {
    val words by viewModel.allWords.collectAsState()

    var selectedTab by remember { mutableStateOf(WordsTab.REGULAR) }
    var wordPendingDelete by remember { mutableStateOf<WordEntity?>(null) }

    val filteredWords = remember(words, selectedTab) {
        WordsTab.filter(selectedTab, words)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = stringResource(id = R.string.words_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.words_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                WordsTabBar(
                    selected = selectedTab,
                    onSelected = { selectedTab = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (filteredWords.isEmpty()) {
                item {
                    EmptyState(message = stringResource(id = selectedTab.emptyMessageRes))
                }
            } else {
                items(filteredWords, key = { it.id }) { word ->
                    WordCard(
                        entity = word,
                        onEdit = { onEditWord(word.id) },
                        onDelete = { wordPendingDelete = word }
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onAddWord,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            icon = {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(id = R.string.words_fab_add)
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.words_fab_add),
                    fontWeight = FontWeight.Bold
                )
            }
        )
    }

    wordPendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { wordPendingDelete = null },
            title = { Text(stringResource(id = R.string.words_delete_title)) },
            text = { Text(stringResource(id = R.string.words_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    // Delegate to the VM so the gate on user-owned rows
                    // is enforced at the data tier, not just in the UI.
                    viewModel.deleteWord(target)
                    wordPendingDelete = null
                }) {
                    Text(
                        text = stringResource(id = R.string.words_delete_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { wordPendingDelete = null }) {
                    Text(stringResource(id = R.string.words_delete_cancel))
                }
            }
        )
    }
}

// region: Tabs
/**
 * Available filters for the Words screen. Each tab maps to a logical
 * subset of the dictionary that the user can browse.
 *
 *  - [REGULAR] / [IRREGULAR] — verbs split by their conjugation behaviour.
 *  - [VOCABULARY] — non-verb entries (nouns, adjectives, interjections, …).
 *  - [MINE] — words added by the user through the form, regardless of
 *    grammatical type.
 */
private enum class WordsTab(val labelRes: Int, val emptyMessageRes: Int) {
    REGULAR(R.string.words_tab_regular, R.string.words_empty_regular),
    IRREGULAR(R.string.words_tab_irregular, R.string.words_empty_irregular),
    VOCABULARY(R.string.words_tab_vocabulary, R.string.words_empty_vocabulary),
    MINE(R.string.words_tab_mine, R.string.words_empty_mine);

    companion object {
        /**
         * Applies the given [tab] filter against the supplied [words].
         *
         * @param tab Active tab.
         * @param words Source list to filter (typically the Room-backed
         *   collection surfaced by [WordListViewModel]).
         * @return Filtered list preserving the original order.
         */
        fun filter(tab: WordsTab, words: List<WordEntity>): List<WordEntity> = when (tab) {
            REGULAR -> words.filter { it.type == "verb" && it.regular == true }
            IRREGULAR -> words.filter { it.type == "verb" && it.regular == false }
            VOCABULARY -> words.filter { it.type != "verb" }
            MINE -> words.filter { it.source == WordEntity.SOURCE_USER }
        }
    }
}

/**
 * Tab bar used at the top of the Words screen.
 *
 * @param selected Currently active tab.
 * @param onSelected Invoked when the user taps a tab.
 */
@Composable
private fun WordsTabBar(
    selected: WordsTab,
    onSelected: (WordsTab) -> Unit
) {
    TabRow(selectedTabIndex = selected.ordinal) {
        WordsTab.entries.forEach { tab ->
            Tab(
                selected = tab == selected,
                onClick = { onSelected(tab) },
                text = {
                    Text(
                        text = stringResource(id = tab.labelRes),
                        fontWeight = if (tab == selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }
    }
}

/** Renders the empty state shown when a tab has no matching entries. */
@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
// endregion

// region: Lifecycle helper
// Removed: lifecycle-aware collector wrapper. Phase 2.5 uses the simpler
// `androidx.compose.runtime.collectAsState` directly because we do not
// yet need the lifecycle-aware variant — the StateFlow is always kept
// warm by the ViewModel scope.
// endregion