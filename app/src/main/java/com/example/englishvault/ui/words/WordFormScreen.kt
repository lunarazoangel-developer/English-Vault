package com.example.englishvault.ui.words

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.englishvault.R
import com.example.englishvault.ui.components.PrimaryButton
import com.example.englishvault.ui.words.viewmodel.WordListViewModel
import data.database.entities.Difficulty
import data.database.entities.WordEntity

/**
 * Word form screen — used for both creating a new word and editing
 * an existing one.
 *
 * Phase 5.5:
 *  - The form injects [WordListViewModel] (shared with the Words
 *    list via Hilt's navigation-scoped graph) and loads the row
 *    referenced by [wordId] on first composition. The pre-fill
 *    replaces the legacy hard-coded mockup data.
 *  - When saving, the original id is preserved so the edit path
 *    updates the existing row via `OnConflictStrategy.REPLACE`
 *    instead of inserting a duplicate.
 *  - The form still surfaces every field that [WordEntity] persists,
 *    including the `level` integer used to bucket the word into a
 *    progression tier.
 *
 * @param wordId Optional id of the word being edited. `null` (or a
 *   negative value) means "new".
 * @param onBack Called when the user taps the back arrow.
 * @param onSave Called when the user taps Save. The parent typically
 *   routes to [WordListViewModel.addUserWord] or
 *   [WordListViewModel.updateUserWord] depending on [wordId].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordFormScreen(
    wordId: Int?,
    onBack: () -> Unit,
    onSave: (WordEntity) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WordListViewModel = hiltViewModel()
) {
    val isEdit = wordId != null && wordId >= 0

    // region: Load existing word when editing
    var loadedWord by remember { mutableStateOf<WordEntity?>(null) }
    var hasLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(wordId) {
        if (isEdit) {
            loadedWord = viewModel.loadWordForEdit(wordId!!)
        }
        hasLoaded = true
    }
    // endregion

    // region: Pre-fill state derived from the loaded word
    // Re-key the remembered state on `loadedWord` so a fresh edit
    // re-initialises every field when the user navigates between
    // different word ids.
    val initial = remember(loadedWord) {
        val loaded = loadedWord
        if (loaded != null) {
            WordFormState(
                word = loaded.word,
                translation = loaded.translation,
                type = loaded.type,
                difficulty = loaded.difficulty.toFormDifficulty(),
                level = loaded.level,
                notes = loaded.notes
            )
        } else {
            WordFormState()
        }
    }

    var word by remember(loadedWord) { mutableStateOf(initial.word) }
    var translation by remember(loadedWord) { mutableStateOf(initial.translation) }
    var type by remember(loadedWord) { mutableStateOf(initial.type) }
    var difficulty by remember(loadedWord) { mutableStateOf(initial.difficulty) }
    var levelText by remember(loadedWord) { mutableStateOf(initial.level.toString()) }
    var notes by remember(loadedWord) { mutableStateOf(initial.notes) }
    // endregion

    val titleRes = if (isEdit) R.string.form_title_edit else R.string.form_title_new

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = titleRes),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.form_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = word,
                onValueChange = { word = it },
                label = { Text(stringResource(id = R.string.form_field_word)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = translation,
                onValueChange = { translation = it },
                label = { Text(stringResource(id = R.string.form_field_translation)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = type,
                onValueChange = { type = it },
                label = { Text(stringResource(id = R.string.form_field_type)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(id = R.string.form_field_difficulty),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FormDifficulty.entries.forEach { level ->
                        val selected = level == difficulty
                        FilterChip(
                            selected = selected,
                            onClick = { difficulty = level },
                            label = {
                                Text(
                                    text = stringResource(id = level.labelRes),
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            OutlinedTextField(
                value = levelText,
                onValueChange = { input ->
                    // Allow only positive integers to keep the level sane.
                    if (input.isEmpty() || input.all { it.isDigit() }) {
                        levelText = input
                    }
                },
                label = { Text(stringResource(id = R.string.form_field_level)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(id = R.string.form_field_notes)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            PrimaryButton(
                text = stringResource(id = R.string.form_save),
                onClick = {
                    val parsedLevel = levelText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    val original = loadedWord
                    onSave(
                        WordEntity(
                            // Preserve the original id when editing so
                            // the insert call updates the same row via
                            // `OnConflictStrategy.REPLACE`. New words
                            // default to 0 which lets AUTOINCREMENT
                            // assign the next free value.
                            id = original?.id ?: 0,
                            word = word,
                            translation = translation,
                            type = type,
                            regular = original?.regular,
                            forms = original?.forms,
                            pronunciation = original?.pronunciation,
                            category = original?.category,
                            synonyms = original?.synonyms,
                            antonyms = original?.antonyms,
                            examples = original?.examples,
                            tags = original?.tags,
                            difficulty = difficulty.toEntityDifficulty(),
                            level = parsedLevel,
                            // Preserve the tri-state progress and any
                            // user-owned counters when editing; default
                            // to NOT_LEARNED + zero counters for new
                            // words.
                            status = original?.status
                                ?: data.database.entities.LearningStatus.NOT_LEARNED,
                            // Persist as user-added so the row shows up
                            // in the Mine tab and exposes edit/delete.
                            source = WordEntity.SOURCE_USER,
                            favorite = original?.favorite ?: false,
                            notes = notes,
                            reviewCount = original?.reviewCount ?: 0,
                            lastReview = original?.lastReview,
                            nextReview = original?.nextReview,
                            customDifficulty = original?.customDifficulty
                        )
                    )
                },
                enabled = hasLoaded
            )

            AssistChip(
                onClick = onBack,
                label = { Text(stringResource(id = R.string.form_cancel)) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurface
                )
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/** Local-only state for the word form. */
private data class WordFormState(
    val word: String = "",
    val translation: String = "",
    val type: String = "",
    val difficulty: FormDifficulty = FormDifficulty.EASY,
    val level: Int = 1,
    val notes: String = ""
)

/** Difficulty options inside the form. */
private enum class FormDifficulty(val labelRes: Int) {
    EASY(R.string.games_difficulty_easy),
    MEDIUM(R.string.games_difficulty_medium),
    HARD(R.string.games_difficulty_hard);

    /** Maps to the Room-backed [Difficulty] enum. */
    fun toEntityDifficulty(): Difficulty = when (this) {
        EASY -> Difficulty.EASY
        MEDIUM -> Difficulty.MEDIUM
        HARD -> Difficulty.HARD
    }
}

/** Maps the Room-backed [Difficulty] enum back into the form-facing [FormDifficulty]. */
private fun Difficulty.toFormDifficulty(): FormDifficulty = when (this) {
    Difficulty.EASY -> FormDifficulty.EASY
    Difficulty.MEDIUM -> FormDifficulty.MEDIUM
    Difficulty.HARD -> FormDifficulty.HARD
}