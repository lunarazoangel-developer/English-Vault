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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.englishvault.R
import com.example.englishvault.ui.components.PrimaryButton
import data.database.entities.Difficulty
import data.database.entities.WordEntity

/**
 * Word form screen — used for both creating a new word and editing an
 * existing one.
 *
 * Phase 2.5:
 *  - The form is still largely visual: typing into the fields updates
 *    local state only.
 *  - The Save callback now receives a fully built [WordEntity] (with
 *    id=0 by default and default user-owned flags) so the parent can
 *    persist it via [com.example.englishvault.ui.words.viewmodel.WordListViewModel].
 *  - When [wordId] is non-null the form pre-fills from the existing row.
 *    Edit is currently visual (no persistence), as agreed for Phase 2.5.
 *
 * @param wordId Optional id of the word being edited. `null` means "new".
 * @param onBack Called when the user taps the back arrow.
 * @param onSave Called when the user taps Save. Receives the assembled
 *   [WordEntity] ready to be inserted by the caller.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordFormScreen(
    wordId: Int?,
    onBack: () -> Unit,
    onSave: (WordEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    // region: Pre-filled mock values when editing — Phase 3 will source them from Room
    val initial = remember(wordId) {
        if (wordId == null || wordId < 0) {
            WordFormState()
        } else {
            when (wordId) {
                1 -> WordFormState("Hello", "Hola", "interjection", FormDifficulty.EASY, "")
                2 -> WordFormState("Practice", "Practicar", "verb", FormDifficulty.MEDIUM, "Use it daily.")
                else -> WordFormState()
            }
        }
    }
    // endregion

    var word by remember { mutableStateOf(initial.word) }
    var translation by remember { mutableStateOf(initial.translation) }
    var type by remember { mutableStateOf(initial.type) }
    var difficulty by remember { mutableStateOf(initial.difficulty) }
    var notes by remember { mutableStateOf(initial.notes) }

    val titleRes = if (wordId == null || wordId < 0) R.string.form_title_new else R.string.form_title_edit

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
                    onSave(
                        WordEntity(
                            // id = 0 so the ViewModel's `toUserEntity`
                            // conversion hands a fresh AUTOINCREMENT slot
                            // to SQLite when persisting the row.
                            id = 0,
                            word = word,
                            translation = translation,
                            type = type,
                            regular = null,
                            forms = null,
                            pronunciation = null,
                            category = null,
                            synonyms = null,
                            antonyms = null,
                            examples = null,
                            tags = null,
                            difficulty = difficulty.toEntityDifficulty(),
                            // Persist as user-added so the row shows up
                            // in the Mine tab and exposes edit/delete.
                            source = WordEntity.SOURCE_USER,
                            favorite = false,
                            learned = false,
                            notes = notes,
                            reviewCount = 0,
                            lastReview = null,
                            nextReview = null,
                            customDifficulty = null
                        )
                    )
                }
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

/** Local-only state for the word form mockup. */
private data class WordFormState(
    val word: String = "",
    val translation: String = "",
    val type: String = "",
    val difficulty: FormDifficulty = FormDifficulty.EASY,
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