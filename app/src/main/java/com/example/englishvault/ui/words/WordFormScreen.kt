package com.example.englishvault.ui.words

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.englishvault.R
import com.example.englishvault.ui.progress.arcade.ArcadeFonts
import com.example.englishvault.ui.progress.arcade.LocalArcadePalette
import com.example.englishvault.ui.progress.arcade.components.ArcadeButton
import com.example.englishvault.ui.words.viewmodel.WordListViewModel
import data.database.entities.Difficulty
import data.database.entities.LearningStatus
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
 * Phase 7.x: rendered end-to-end against the arcade palette (reads
 * [LocalArcadePalette] at the root). The Save button is now an
 * [ArcadeButton] (3D pink pill) and the Cancel control is the
 * outlined arcade variant defined at the bottom of this file.
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
    val palette = LocalArcadePalette.current
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
        containerColor = palette.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = titleRes),
                        color = palette.textMain,
                        fontFamily = ArcadeFonts.Display,
                        fontWeight = ArcadeFonts.DisplayWeight,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.form_back),
                            tint = palette.textMain
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = palette.background,
                    titleContentColor = palette.textMain
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(palette.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = word,
                onValueChange = { word = it },
                label = {
                    Text(
                        text = stringResource(id = R.string.form_field_word),
                        color = palette.textDim,
                        fontFamily = ArcadeFonts.Pixel,
                        fontWeight = ArcadeFonts.PixelWeight,
                        fontSize = 11.sp
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = palette.textMain,
                    fontFamily = ArcadeFonts.Pixel,
                    fontWeight = ArcadeFonts.PixelWeight,
                    fontSize = 14.sp
                )
            )

            OutlinedTextField(
                value = translation,
                onValueChange = { translation = it },
                label = {
                    Text(
                        text = stringResource(id = R.string.form_field_translation),
                        color = palette.textDim,
                        fontFamily = ArcadeFonts.Pixel,
                        fontWeight = ArcadeFonts.PixelWeight,
                        fontSize = 11.sp
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = palette.textMain,
                    fontFamily = ArcadeFonts.Pixel,
                    fontWeight = ArcadeFonts.PixelWeight,
                    fontSize = 14.sp
                )
            )

            OutlinedTextField(
                value = type,
                onValueChange = { type = it },
                label = {
                    Text(
                        text = stringResource(id = R.string.form_field_type),
                        color = palette.textDim,
                        fontFamily = ArcadeFonts.Pixel,
                        fontWeight = ArcadeFonts.PixelWeight,
                        fontSize = 11.sp
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = palette.textMain,
                    fontFamily = ArcadeFonts.Pixel,
                    fontWeight = ArcadeFonts.PixelWeight,
                    fontSize = 14.sp
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(id = R.string.form_field_difficulty),
                    color = palette.textMain,
                    fontFamily = ArcadeFonts.Pixel,
                    fontWeight = ArcadeFonts.PixelWeight,
                    fontSize = 11.sp
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FormDifficulty.entries.forEach { level ->
                        FormFilterChip(
                            label = stringResource(id = level.labelRes),
                            accent = when (level) {
                                FormDifficulty.EASY -> palette.primary
                                FormDifficulty.MEDIUM -> palette.secondary
                                FormDifficulty.HARD -> palette.error
                            },
                            selected = level == difficulty,
                            onClick = { difficulty = level }
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
                label = {
                    Text(
                        text = stringResource(id = R.string.form_field_level),
                        color = palette.textDim,
                        fontFamily = ArcadeFonts.Pixel,
                        fontWeight = ArcadeFonts.PixelWeight,
                        fontSize = 11.sp
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = palette.textMain,
                    fontFamily = ArcadeFonts.Pixel,
                    fontWeight = ArcadeFonts.PixelWeight,
                    fontSize = 14.sp
                )
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = {
                    Text(
                        text = stringResource(id = R.string.form_field_notes),
                        color = palette.textDim,
                        fontFamily = ArcadeFonts.Pixel,
                        fontWeight = ArcadeFonts.PixelWeight,
                        fontSize = 11.sp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(12.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = palette.textMain,
                    fontFamily = ArcadeFonts.Pixel,
                    fontWeight = ArcadeFonts.PixelWeight,
                    fontSize = 14.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            ArcadeButton(
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
                            status = original?.status ?: LearningStatus.NOT_LEARNED,
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
                enabled = hasLoaded,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedArcadeButton(
                text = stringResource(id = R.string.form_cancel),
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Arcade-style filter chip used by the form's difficulty selector.
 * Matches the look of the chips in `WordListScreen` so the form feels
 * like part of the same vocabulary flow.
 */
@Composable
private fun FormFilterChip(
    label: String,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalArcadePalette.current
    val containerColor = if (selected) accent else palette.surface
    val contentColor = if (selected) palette.ink else accent
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = contentColor,
            fontFamily = ArcadeFonts.Pixel,
            fontWeight = ArcadeFonts.PixelWeight,
            fontSize = 11.sp
        )
    }
}

/**
 * Outlined arcade button — matches the helper used by the three
 * mini-game end screens so the form's secondary "Cancel" action
 * reads as part of the same brand language.
 */
@Composable
private fun OutlinedArcadeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalArcadePalette.current
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(palette.surface)
            .border(width = 2.dp, color = palette.border, shape = RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text = text,
            color = palette.textMain,
            fontFamily = ArcadeFonts.Pixel,
            fontWeight = ArcadeFonts.PixelWeight,
            fontSize = 12.sp
        )
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