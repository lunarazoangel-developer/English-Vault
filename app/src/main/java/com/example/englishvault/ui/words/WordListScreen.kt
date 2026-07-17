package com.example.englishvault.ui.words

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.englishvault.R
import com.example.englishvault.ui.progress.arcade.ArcadeFonts
import com.example.englishvault.ui.progress.arcade.LocalArcadePalette
import com.example.englishvault.ui.words.components.WordCard
import com.example.englishvault.ui.words.viewmodel.WordListViewModel
import data.database.entities.WordEntity
import data.database.entities.isUserAdded
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest

/**
 * Words screen — list of vocabulary entries with add / edit / delete
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
 *  - The first row is a horizontally-scrollable list of filter chips
 *    that group words by grammatical type (or origin for "Mine").
 *  - The second row chooses the sort order for the visible list
 *    (alphabetical A→Z / Z→A or by progression level ascending /
 *    descending).
 *  - Both selections are screen-local state persisted across
 *    configuration changes via `rememberSaveable` using their ordinal
 *    (enum entries are stable as long as the file is not refactored
 *    mid-session).
 *
 * Phase 7.x: the screen renders end-to-end against the arcade palette
 * (reads [LocalArcadePalette] at the root). The FAB, type / sort chips
 * and search field all share the same palette so the screen flips
 * between dark and light with the rest of the UI. The card's per-row
 * 🔊 button also drives a `speakingText` reflection so the row
 * currently being pronounced can tint its button green via the
 * [WordCard.speakingIsThisWord] flag.
 *
 * Pagination and search: the list is paged through [PAGE_SIZE] cards
 * at a time and grows by the same amount as the user scrolls near
 * the rendered end. A debounced [OutlinedTextField] below the sort
 * row filters by free-text match against the word, its translation,
 * its category and its tags. The page window resets whenever the
 * type filter, sort order or debounced search query changes so the
 * user always starts at the top of the freshly-narrowed list.
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
    val palette = LocalArcadePalette.current
    val words by viewModel.allWords.collectAsState()
    val speakingText by viewModel.speakingText.collectAsState()

    var selectedType by rememberSaveable { mutableStateOf(WordsTabFilter.ALL) }
    var sortOrder by rememberSaveable { mutableStateOf(SortOrder.LEVEL_ASC) }
    var wordPendingDelete by remember { mutableStateOf<WordEntity?>(null) }

    // Search input is captured immediately so the field stays
    // responsive, but the actual filter is applied against the
    // debounced value to avoid re-running the filter on every
    // keystroke.
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    LaunchedEffect(searchQuery) {
        delay(SEARCH_DEBOUNCE_MILLIS)
        debouncedQuery = searchQuery
    }

    // Pagination state. The list shows [PAGE_SIZE] cards at a time and
    // grows by [PAGE_SIZE] as the user scrolls near the end. Reset
    // back to [PAGE_SIZE] whenever the filter, sort order or search
    // query changes so the user always starts at the top of the
    // freshly-narrowed list.
    val listState = rememberLazyListState()
    var visibleCount by remember { mutableStateOf(PAGE_SIZE) }
    LaunchedEffect(selectedType, sortOrder, debouncedQuery) {
        visibleCount = PAGE_SIZE
        listState.scrollToItem(0)
    }

    // Per-card expansion state. Keyed by `WordEntity.id` so core and
    // user rows track their own state. Persists across configuration
    // changes via `rememberSaveable` thanks to the custom Saver that
    // flattens the map into a Bundle-friendly list.
    val expandedIds = rememberSaveable(
        saver = ExpansionSaver
    ) {
        mutableStateMapOf<Int, Boolean>()
    }

    // Filter + search + sort in a single derivedStateOf so the UI only
    // recomposes when the source data, the type filter, the debounced
    // search query or the sort order changes.
    val displayedWords by remember(words, selectedType, sortOrder, debouncedQuery) {
        derivedStateOf {
            val needle = debouncedQuery.trim().lowercase()
            val filtered = words.filter { word ->
                selectedType.type.matches(word) &&
                    (
                        needle.isEmpty() ||
                            word.word.lowercase().contains(needle) ||
                            word.translation.lowercase().contains(needle) ||
                            (word.category?.any { it.lowercase().contains(needle) } == true) ||
                            (word.tags?.any { it.lowercase().contains(needle) } == true)
                        )
            }
            sortOrder.apply(filtered)
        }
    }
    val typeCounts: Map<WordsTabFilter, Int> by remember(words) {
        derivedStateOf {
            WordsTabFilter.entries.associateWith { f -> words.count { f.type.matches(it) } }
        }
    }

    // Infinite-scroll trigger: when the last visible item gets within
    // [LOAD_AHEAD_THRESHOLD] cards of the rendered end, grow the
    // visible window by one page. We compare against the LazyColumn's
    // own `totalItemsCount` (headers + currently-rendered cards)
    // rather than the filtered word list because `visibleItemsInfo`
    // indexes the full LazyColumn — headers and cards alike — so a
    // direct comparison against `displayedWords.size` always misses
    // the threshold.
    //
    // Wrapped in [snapshotFlow] so the collector re-runs on every
    // scroll tick — a plain `LaunchedEffect` keyed on [listState]
    // would only fire when one of its captured values changes, and
    // none of them do while the user is just dragging.
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible to info.totalItemsCount
        }
            .distinctUntilChanged()
            .collectLatest { (lastVisible, totalRendered) ->
                val totalCards = displayedWords.size
                if (totalRendered > 0 &&
                    lastVisible >= totalRendered - LOAD_AHEAD_THRESHOLD &&
                    visibleCount < totalCards
                ) {
                    visibleCount = (visibleCount + PAGE_SIZE).coerceAtMost(totalCards)
                }
            }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
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
                    color = palette.textMain,
                    fontFamily = ArcadeFonts.Display,
                    fontWeight = ArcadeFonts.DisplayWeight,
                    fontSize = 22.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.words_subtitle),
                    color = palette.textDim,
                    fontFamily = ArcadeFonts.Pixel,
                    fontWeight = ArcadeFonts.PixelWeight,
                    fontSize = 11.sp
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

            item {
                SearchField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onClear = { searchQuery = "" }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (displayedWords.isEmpty()) {
                item {
                    EmptyState(message = stringResource(id = selectedType.emptyMessageRes))
                }
            } else {
                items(
                    items = displayedWords.take(visibleCount),
                    key = { it.id }
                ) { word ->
                    WordCard(
                        entity = word,
                        expanded = expandedIds[word.id] == true,
                        onToggle = {
                            expandedIds[word.id] = expandedIds[word.id] != true
                        },
                        onCycleStatus = { status ->
                            viewModel.setStatus(word.id, status)
                        },
                        onSpeak = { viewModel.speakWord(it) },
                        isSpeaking = { speakingText == it },
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
            containerColor = palette.highlight,
            contentColor = palette.ink,
            icon = {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(id = R.string.words_fab_add)
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.words_fab_add),
                    color = palette.ink,
                    fontFamily = ArcadeFonts.Pixel,
                    fontWeight = ArcadeFonts.PixelWeight,
                    fontSize = 12.sp
                )
            }
        )
    }

    wordPendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { wordPendingDelete = null },
            title = {
                Text(
                    text = stringResource(id = R.string.words_delete_title),
                    color = palette.textMain,
                    fontFamily = ArcadeFonts.Display,
                    fontWeight = ArcadeFonts.DisplayWeight,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.words_delete_message),
                    color = palette.textMain,
                    fontFamily = ArcadeFonts.Pixel,
                    fontWeight = ArcadeFonts.PixelWeight,
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    // Delegate to the VM so the gate on user-owned rows
                    // is enforced at the data tier, not just in the UI.
                    viewModel.deleteWord(target)
                    wordPendingDelete = null
                }) {
                    Text(
                        text = stringResource(id = R.string.words_delete_confirm),
                        color = palette.error,
                        fontFamily = ArcadeFonts.Pixel,
                        fontWeight = ArcadeFonts.PixelWeight,
                        fontSize = 12.sp
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { wordPendingDelete = null }) {
                    Text(
                        text = stringResource(id = R.string.words_delete_cancel),
                        color = palette.textMain,
                        fontFamily = ArcadeFonts.Pixel,
                        fontWeight = ArcadeFonts.PixelWeight,
                        fontSize = 12.sp
                    )
                }
            },
            containerColor = palette.surface
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
 * Horizontally-scrollable row of arcade chips, one per
 * [WordTypeFilter]. The chip label carries the live count of words
 * that match its filter so the user can see the dictionary split at a
 * glance.
 *
 * Each chip's tint comes from the arcade category palette so the
 * grammatical bucket reads as its own colour even at a glance.
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
            ArcadeFilterChip(
                label = "${stringResource(id = filter.labelRes)} ($count)",
                accent = accentForFilter(filter),
                selected = filter == selected,
                enabled = enabled,
                onClick = { if (enabled) onSelect(filter) }
            )
        }
    }
}

/**
 * Picks the arcade accent for a [WordsTabFilter].
 *
 * `categoryColor()` maps the pseudo-buckets `ALL` and `MINE` to the
 * generic `surfaceDark` so they look like "no category" — which works
 * fine for neutral surfaces but leaves the `ALL` chip looking dull
 * next to the vivid grammatical chips. We override those two here so
 * every chip carries its own identity:
 *  - **ALL**     → `palette.highlight` (gold) — the "show everything"
 *    entry deserves the most eye-catching accent so the player always
 *    knows where to reset the filter.
 *  - **MINE**    → `palette.success`  (green) — reads as "growth /
 *    personal library" and mirrors the per-row user-badge accent on
 *    the cards.
 *  - everything else falls through to the canonical category colour.
 */
@Composable
private fun accentForFilter(filter: WordsTabFilter): androidx.compose.ui.graphics.Color {
    val palette = LocalArcadePalette.current
    return when (filter) {
        WordsTabFilter.ALL -> palette.highlight
        WordsTabFilter.MINE -> palette.success
        else -> palette.categoryColor(filter.type)
    }
}

/**
 * Arcade-style filter chip. Tint comes from the live arcade palette
 * so the chip flips with the rest of the UI; the [accent] colour lets
 * each filter carry its own hue (driven by [ArcadePalette.categoryColor]).
 *
 * Selected chips flip to their accent colour as the background; idle
 * chips stay on `palette.surface` with a coloured text + border so the
 * accent still reads at a glance.
 */
@Composable
private fun ArcadeFilterChip(
    label: String,
    accent: androidx.compose.ui.graphics.Color,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalArcadePalette.current
    val containerColor = if (selected) accent else palette.surface
    val contentColor = if (selected) palette.ink else accent
    val borderColor = if (enabled) accent else palette.border
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (enabled) containerColor else palette.surfaceDark)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (enabled) contentColor else palette.textDim,
            fontFamily = ArcadeFonts.Pixel,
            fontWeight = ArcadeFonts.PixelWeight,
            fontSize = 10.sp
        )
    }
}

/** Renders the empty state shown when the current filter has no matches. */
@Composable
private fun EmptyState(message: String) {
    val palette = LocalArcadePalette.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = palette.textDim,
            fontFamily = ArcadeFonts.Pixel,
            fontWeight = ArcadeFonts.PixelWeight,
            fontSize = 12.sp
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
 * Row of arcade chips that selects the [SortOrder]. Placed
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
    val palette = LocalArcadePalette.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.words_sort_label),
            color = palette.textDim,
            fontFamily = ArcadeFonts.Pixel,
            fontWeight = ArcadeFonts.PixelWeight,
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 8.dp)
        ) {
            items(SortOrder.entries, key = { it.name }) { order ->
                ArcadeFilterChip(
                    label = stringResource(id = order.labelRes),
                    accent = palette.secondary,
                    selected = order == selected,
                    enabled = true,
                    onClick = { onSelect(order) }
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
 * …]` so we can reconstruct the map without an extra delimiter.
 */
private val ExpansionSaver: Saver<SnapshotStateMap<Int, Boolean>, Any> =
    Saver(
        save = { stateMap ->
            // Bundle-friendly flat list: [id1, value1, id2, value2, …]
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

// region: Search field
/**
 * Single-line text input that filters the word list. Captures every
 * keystroke into [onQueryChange] so the field stays responsive; the
 * parent is responsible for debouncing before applying the filter to
 * the underlying data set.
 *
 * Renders a leading magnifying-glass icon and a trailing clear button
 * (only when [query] is non-empty) so the user can wipe the search in
 * one tap without backspacing the whole query. Colours come from the
 * arcade palette so the field stays on-brand in both themes.
 */
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalArcadePalette.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = {
            Text(
                text = stringResource(id = R.string.words_search_hint),
                color = palette.textDim,
                fontFamily = ArcadeFonts.Pixel,
                fontWeight = ArcadeFonts.PixelWeight,
                fontSize = 12.sp
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = palette.textDim
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(id = R.string.words_search_clear_cd),
                        tint = palette.textDim
                    )
                }
            }
        }
    )
}
// endregion

// region: Constants
/**
 * Initial and incremental page size for the paged word list. Every
 * time the user nears the rendered end of the list, the visible
 * window grows by [PAGE_SIZE] cards.
 */
private const val PAGE_SIZE: Int = 20

/**
 * Time the search input is debounced before its value reaches the
 * filter pipeline. Keeps the list from recomputing on every
 * keystroke.
 */
private const val SEARCH_DEBOUNCE_MILLIS: Long = 300

/**
 * When the last visible item index is within this distance of the
 * rendered end, the visible window grows by one page. Slightly
 * larger than zero so the next page starts loading before the user
 * actually reaches the bottom.
 */
private const val LOAD_AHEAD_THRESHOLD: Int = 4
// endregion