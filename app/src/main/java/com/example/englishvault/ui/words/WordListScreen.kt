package com.example.englishvault.ui.words

import androidx.annotation.StringRes
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
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
import data.database.entities.isUserAdded

/**
 * Words screen â€” list of vocabulary entries with add / edit / delete
 * actions and rich detail on tap.
 *
 * Phase 4: data flows from Room through [WordListViewModel] and the
 * list is rendered from the unified `words_view` that joins
 * `core_words` and `user_words`. Each card is expandable to surface
 * pronunciation, verb forms, examples, synonyms, antonyms, tags and
 * category. The source badge (`Dictionary` vs `Mine`) communicates
 * which rows the user can edit.
 *
 * Phase 4.6: the top of the screen exposes two filter rows.
 *  - The first row is a horizontally-scrollable list of [FilterChip]s
 *    that group words by grammatical type (or origin for "Mine").
 *  - The second row chooses the sort order for the visible list
 *    (alphabetical Aâ†’Z / Zâ†’A or by progression level ascending /
 *    descending).
 *  - Both selections are screen-local state persisted across
 *    configuration changes via `rememberSaveable` using their ordinal
 *    (enum entries are stable as long as the file is not refactored
 *    mid-session).
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

    var selectedType by rememberSaveable { mutableStateOf(WordsTabFilter.ALL) }
    var sortOrder by rememberSaveable { mutableStateOf(SortOrder.ALPHABETICAL_ASC) }
    var wordPendingDelete by remember { mutableStateOf<WordEntity?>(null) }

    // Per-card expansion state. Keyed by `WordEntity.id` so core and
    // user rows track their own state. Persists across configuration
    // changes via `rememberSaveable` thanks to the custom Saver that
    // flattens the map into a Bundle-friendly list.
    val expandedIds = rememberSaveable(
        saver = ExpansionSaver
    ) {
        mutableStateMapOf<Int, Boolean>()
    }

    // Filter + sort in a single derivedStateOf so the UI only
    // recomposes when either the source data, the type filter or the
    // sort order changes.
    val displayedWords by remember(words, selectedType, sortOrder) {
        derivedStateOf {
            val filtered = words.filter { selectedType.type.matches(it) }
            sortOrder.apply(filtered)
        }
    }
    val typeCounts: Map<WordsTabFilter, Int> by remember(words) {
        derivedStateOf {
            WordsTabFilter.entries.associateWith { f -> words.count { f.type.matches(it) } }
        }
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
                TypeFilterRow(
                    selected = selectedType,
                    counts = typeCounts,
                    onSelect = { selectedType = it }
                )
            }

            item {
                SortRow(
                    selected = sortOrder,
                    onSelect = { sortOrder = it }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (displayedWords.isEmpty()) {
                item {
                    EmptyState(message = stringResource(id = selectedType.emptyMessageRes))
                }
            } else {
                items(displayedWords, key = { it.id }) { word ->
                    WordCard(
                        entity = word,
                        expanded = expandedIds[word.id] == true,
                        onToggle = {
                            expandedIds[word.id] = expandedIds[word.id] != true
                        },
                        onCycleStatus = { status ->
                            viewModel.setStatus(word.id, status)
                        },
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

// region: Type filter
/**
 * Filter state used by the Words screen. Mirrors the canonical
 * [WordTypeFilter] enum but carries an extra [emptyMessageRes] per
 * entry so each bucket can have its own copy when the list is empty.
 */
private enum class WordsTabFilter(
    val type: WordTypeFilter,
    @StringRes val labelRes: Int,
    @StringRes val emptyMessageRes: Int
) {
    ALL(WordTypeFilter.ALL, R.string.words_tab_all, R.string.words_empty_all),
    VERBS_REGULAR(WordTypeFilter.VERBS_REGULAR, R.string.words_tab_regular, R.string.words_empty_regular),
    VERBS_IRREGULAR(WordTypeFilter.VERBS_IRREGULAR, R.string.words_tab_irregular, R.string.words_empty_irregular),
    ADJECTIVES(WordTypeFilter.ADJECTIVES, R.string.words_tab_adjectives, R.string.words_empty_adjectives),
    ADVERBS(WordTypeFilter.ADVERBS, R.string.words_tab_adverbs, R.string.words_empty_adverbs),
    NOUNS(WordTypeFilter.NOUNS, R.string.words_tab_nouns, R.string.words_empty_nouns),
    CONJUNCTIONS(WordTypeFilter.CONJUNCTIONS, R.string.words_tab_conjunctions, R.string.words_empty_conjunctions),
    PREPOSITIONS(WordTypeFilter.PREPOSITIONS, R.string.words_tab_prepositions, R.string.words_empty_prepositions),
    INTERJECTIONS(WordTypeFilter.INTERJECTIONS, R.string.words_tab_interjections, R.string.words_empty_interjections),
    MINE(WordTypeFilter.MINE, R.string.words_tab_mine, R.string.words_empty_mine);
}

/**
 * Horizontally-scrollable row of [FilterChip]s, one per
 * [WordTypeFilter]. The chip label carries the live count of words
 * that match its filter so the user can see the dictionary split at a
 * glance.
 *
 * @param selected Currently active filter.
 * @param counts Per-filter entry totals, computed in a single pass
 *   over the source list.
 * @param onSelect Invoked when the user taps a chip.
 */
@Composable
private fun TypeFilterRow(
    selected: WordsTabFilter,
    counts: Map<WordsTabFilter, Int>,
    onSelect: (WordsTabFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(WordsTabFilter.entries, key = { it.name }) { filter ->
            val count = counts[filter] ?: 0
            // Disable empty buckets (except ALL) so the user is not
            // tempted to tap into a guaranteed-empty list.
            val enabled = count > 0 || filter == WordsTabFilter.ALL
            FilterChip(
                selected = filter == selected,
                onClick = { if (enabled) onSelect(filter) },
                enabled = enabled,
                label = {
                    Text(
                        text = "${stringResource(id = filter.labelRes)} ($count)",
                        maxLines = 1
                    )
                },
                colors = FilterChipDefaults.filterChipColors()
            )
        }
    }
}

/** Renders the empty state shown when the current filter has no matches. */
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

// region: Sort
/**
 * Order in which the filtered list is rendered. Ties (same level, or
 * equal words when sorted by level) are always broken alphabetically
 * so the visible list is stable across taps.
 */
private enum class SortOrder(val labelRes: Int) {
    ALPHABETICAL_ASC(R.string.words_sort_alpha_asc),
    ALPHABETICAL_DESC(R.string.words_sort_alpha_desc),
    LEVEL_ASC(R.string.words_sort_level_asc),
    LEVEL_DESC(R.string.words_sort_level_desc);

    /**
     * Returns a new list sorted according to this order. Does not
     * mutate [words].
     */
    fun apply(words: List<WordEntity>): List<WordEntity> = when (this) {
        ALPHABETICAL_ASC -> words.sortedBy { it.word.lowercase() }
        ALPHABETICAL_DESC -> words.sortedByDescending { it.word.lowercase() }
        LEVEL_ASC -> words.sortedWith(
            compareBy<WordEntity> { it.level }.thenBy { it.word.lowercase() }
        )
        LEVEL_DESC -> words.sortedWith(
            compareByDescending<WordEntity> { it.level }.thenBy { it.word.lowercase() }
        )
    }
}

/**
 * Row of [FilterChip]s that selects the [SortOrder]. Placed
 * immediately under the type-filter row so it is always reachable
 * without an extra tap.
 *
 * @param selected Currently active sort order.
 * @param onSelect Invoked when the user taps a sort chip.
 */
@Composable
private fun SortRow(
    selected: SortOrder,
    onSelect: (SortOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.words_sort_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 8.dp)
        ) {
            items(SortOrder.entries, key = { it.name }) { order ->
                FilterChip(
                    selected = order == selected,
                    onClick = { onSelect(order) },
                    label = {
                        Text(
                            text = stringResource(id = order.labelRes),
                            maxLines = 1
                        )
                    }
                )
            }
        }
    }
}
// endregion

// region: ExpansionSaver
/**
 * Custom Saver that flattens a `SnapshotStateMap<Int, Boolean>` into
 * a `List<Int>` so it can survive configuration changes via
 * `rememberSaveable`. The list alternates `[id, value(0|1), id, value,
 * â€¦]` so we can reconstruct the map without an extra delimiter.
 */
private val ExpansionSaver: Saver<SnapshotStateMap<Int, Boolean>, Any> =
    Saver(
        save = { stateMap ->
            // Bundle-friendly flat list: [id1, value1, id2, value2, â€¦]
            stateMap.flatMap { (id, value) -> listOf(id, if (value) 1 else 0) }
        },
        restore = { saved ->
            @Suppress("UNCHECKED_CAST")
            val list = saved as List<Int>
            val map = mutableStateMapOf<Int, Boolean>()
            list.chunked(2).forEach { chunk ->
                if (chunk.size == 2) {
                    map[chunk[0]] = chunk[1] == 1
                }
            }
            map
        }
    )
// endregion
