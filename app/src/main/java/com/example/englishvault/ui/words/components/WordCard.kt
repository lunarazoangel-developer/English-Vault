package com.example.englishvault.ui.words.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.englishvault.R
import com.example.englishvault.ui.progress.arcade.ArcadeFonts
import com.example.englishvault.ui.progress.arcade.LocalArcadePalette
import com.example.englishvault.ui.progress.arcade.components.ArcadeIconButton
import data.database.entities.Difficulty
import data.database.entities.LearningStatus
import data.database.entities.WordEntity
import data.database.entities.isUserAdded

/**
 * Single card representing a vocabulary word in the Words screen.
 *
 * Phase 4: receives a fully formed [WordEntity] from the parent
 * screen and renders the full dictionary payload:
 *  - Compact header (avatar, word, translation, action buttons)
 *  - Source badge (`Dictionary` for core, `Mine` for user-added)
 *  - Type and difficulty chips
 *  - Per-level chip whose colour cycles through the arcade palette so
 *    the player can tell levels apart at a glance
 *  - Expandable detail section with pronunciation, verb forms,
 *    examples, synonyms, antonyms, tags and category.
 *
 * Phase 7.x: the header now exposes two 🔊 buttons.
 *  - The first one always speaks the word through the device TTS.
 *  - The second one is rendered only for verbs (regular or irregular)
 *    whose [Forms] carry at least one past form. Tapping it queues
 *    the past simple and past participle in sequence so the learner
 *    can hear the irregulars side by side.
 *
 * Expand / collapse is controlled by the parent so the surrounding
 * screen can persist the state across configuration changes via
 * `rememberSaveable`. The expand control is now an [ArcadeIconButton]
 * (3D pill) instead of the legacy rotating chevron — same affordance,
 * on-brand with the rest of the arcade.
 *
 * Delete and edit are exposed only for user-owned rows (see
 * [isUserAdded]).
 *
 * @param entity Word backing this card.
 * @param expanded Whether the detail section is currently visible.
 * @param onToggle Called when the user taps the expand / collapse
 *   control.
 * @param onCycleStatus Called when the user picks a new
 *   [LearningStatus] from the status menu button. Available on every
 *   card, including dictionary entries.
 * @param onSpeak Called when the user taps any 🔊 button. The card
 *   uses this single callback for both the header "speak word"
 *   button and each per-form button inside the expanded Forms
 *   table — the form's text is passed in.
 * @param isSpeaking Returns `true` when the TTS engine is currently
 *   uttering the supplied text. The card uses this to tint the
 *   active 🔊 button green (both in the header and in the Forms
 *   table) so the user always knows which row is being spoken.
 * @param onEdit Called when the pencil icon is tapped. Ignored when
 *   the row is not user-owned.
 * @param onDelete Called when the trash icon is tapped. Ignored when
 *   the row is not user-owned.
 */
@Composable
fun WordCard(
    entity: WordEntity,
    expanded: Boolean,
    onToggle: () -> Unit,
    onCycleStatus: (LearningStatus) -> Unit,
    onSpeak: (String) -> Unit,
    isSpeaking: (String) -> Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    WordCard(
        word = entity.word,
        translation = entity.translation,
        type = entity.type,
        difficulty = entity.difficulty.toCardDifficulty(),
        isUserAdded = entity.isUserAdded(),
        status = entity.status,
        level = entity.level,
        pronunciation = entity.pronunciation?.ipa,
        forms = entity.forms,
        examples = entity.examples,
        synonyms = entity.synonyms,
        antonyms = entity.antonyms,
        tags = entity.tags,
        category = entity.category,
        expanded = expanded,
        onToggle = onToggle,
        onCycleStatus = onCycleStatus,
        onSpeak = onSpeak,
        isSpeaking = isSpeaking,
        onEdit = onEdit,
        onDelete = onDelete,
        modifier = modifier
    )
}

/**
 * Lower-level overload kept for previews and tests that already have
 * every field on hand. TTS callbacks default to no-ops so callers
 * without a [com.example.englishvault.ui.games.common.TtsPlayer] do
 * not need to thread the engine through.
 */
@Composable
fun WordCard(
    word: String,
    translation: String,
    type: String,
    difficulty: WordCardDifficulty,
    isUserAdded: Boolean,
    status: LearningStatus,
    level: Int,
    pronunciation: String?,
    forms: data.database.entities.Forms?,
    examples: List<data.database.entities.Example>?,
    synonyms: List<String>?,
    antonyms: List<String>?,
    tags: List<String>?,
    category: List<String>?,
    expanded: Boolean,
    onToggle: () -> Unit,
    onCycleStatus: (LearningStatus) -> Unit,
    onSpeak: (String) -> Unit = {},
    isSpeaking: (String) -> Boolean = { false },
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalArcadePalette.current
    val toggleDescription = stringResource(
        if (expanded) R.string.words_action_collapse else R.string.words_action_expand
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 3D expand / collapse pill (replaces the legacy
                // rotating chevron). The accent flips between the
                // primary pink (closed) and the success lime (open).
                ArcadeIconButton(
                    onClick = onToggle,
                    color = if (expanded) palette.success else palette.primary,
                    content = {
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = toggleDescription,
                            tint = palette.ink,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(if (expanded) 90f else 0f)
                        )
                    }
                )
                Spacer(modifier = Modifier.size(8.dp))
                // Avatar bubble with the word's initial letter.
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(palette.primary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = word.firstOrNull()?.uppercase().orEmpty(),
                        color = palette.primary,
                        fontFamily = ArcadeFonts.Display,
                        fontWeight = ArcadeFonts.DisplayWeight,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.size(12.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = toggleDescription,
                            onClick = onToggle
                        )
                ) {
                    Text(
                        text = word,
                        color = palette.textMain,
                        fontFamily = ArcadeFonts.Display,
                        fontWeight = ArcadeFonts.DisplayWeight,
                        fontSize = 16.sp
                    )
                    Text(
                        text = translation,
                        color = palette.textDim,
                        fontFamily = ArcadeFonts.Pixel,
                        fontWeight = ArcadeFonts.PixelWeight,
                        fontSize = 11.sp
                    )
                }
                // TTS button — single header button for the base word.
                // The expanded Forms table renders its own per-row
                // 🔊 buttons so the header stays uncluttered.
                IconButton(onClick = { onSpeak(word) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = stringResource(id = R.string.words_action_speak),
                        tint = if (isSpeaking(word)) palette.success else palette.textMain
                    )
                }
                // Status menu button — available on every card.
                StatusMenuButton(
                    status = status,
                    onPick = onCycleStatus
                )
                if (isUserAdded) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(id = R.string.words_action_edit),
                            tint = palette.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(id = R.string.words_action_delete),
                            tint = palette.error
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isUserAdded) {
                    SourceBadge(
                        label = stringResource(id = R.string.words_badge_mine),
                        icon = Icons.Filled.Person,
                        containerColor = palette.highlight,
                        contentColor = palette.ink
                    )
                } else {
                    SourceBadge(
                        label = stringResource(id = R.string.words_badge_dictionary),
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        containerColor = palette.secondary,
                        contentColor = palette.ink
                    )
                }
                MetaChip(text = type)
                DifficultyChip(difficulty = difficulty)
                LevelChip(level = level)
            }

            // region: Expanded detail section
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pronunciation?.takeIf { it.isNotBlank() }?.let { ipa ->
                        DetailSection(title = stringResource(id = R.string.words_section_pronunciation)) {
                            Text(
                                text = ipa,
                                color = palette.textMain,
                                fontFamily = ArcadeFonts.Pixel,
                                fontWeight = ArcadeFonts.PixelWeight,
                                fontSize = 13.sp
                            )
                        }
                    }
                    forms?.let { f ->
                        DetailSection(title = stringResource(id = R.string.words_section_forms)) {
                            FormsTable(forms = f, onSpeak = onSpeak, isSpeaking = isSpeaking)
                        }
                    }
                    examples?.takeIf { it.isNotEmpty() }?.let { list ->
                        DetailSection(title = stringResource(id = R.string.words_section_examples)) {
                            ExamplesList(examples = list)
                        }
                    }
                    synonyms?.takeIf { it.isNotEmpty() }?.let { list ->
                        DetailSection(title = stringResource(id = R.string.words_section_synonyms)) {
                            ChipStrip(items = list)
                        }
                    }
                    antonyms?.takeIf { it.isNotEmpty() }?.let { list ->
                        DetailSection(title = stringResource(id = R.string.words_section_antonyms)) {
                            ChipStrip(items = list)
                        }
                    }
                    category?.takeIf { it.isNotEmpty() }?.let { list ->
                        DetailSection(title = stringResource(id = R.string.words_section_category)) {
                            ChipStrip(items = list)
                        }
                    }
                    tags?.takeIf { it.isNotEmpty() }?.let { list ->
                        DetailSection(title = stringResource(id = R.string.words_section_tags)) {
                            ChipStrip(items = list)
                        }
                    }
                }
            }
            // endregion
        }
    }
}

// region: Helpers

/**
 * Status menu button rendered on every card.
 *
 * The icon reflects the current [LearningStatus]; tap opens a
 * [DropdownMenu] with the three options. Picking an item fires
 * [onPick] and dismisses the menu.
 */
@Composable
private fun StatusMenuButton(
    status: LearningStatus,
    onPick: (LearningStatus) -> Unit
) {
    val palette = LocalArcadePalette.current
    var expanded by remember { mutableStateOf(false) }
    val icon = when (status) {
        LearningStatus.NOT_LEARNED -> Icons.Filled.RadioButtonUnchecked
        LearningStatus.ALMOST -> Icons.Outlined.CompareArrows
        LearningStatus.LEARNED -> Icons.Filled.CheckCircle
    }
    val tint = when (status) {
        LearningStatus.NOT_LEARNED -> palette.textDim
        LearningStatus.ALMOST -> palette.highlight
        LearningStatus.LEARNED -> palette.success
    }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(id = R.string.words_status_change),
                tint = tint
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            StatusMenuEntry(
                status = LearningStatus.NOT_LEARNED,
                selected = status == LearningStatus.NOT_LEARNED,
                onClick = {
                    onPick(LearningStatus.NOT_LEARNED)
                    expanded = false
                }
            )
            StatusMenuEntry(
                status = LearningStatus.ALMOST,
                selected = status == LearningStatus.ALMOST,
                onClick = {
                    onPick(LearningStatus.ALMOST)
                    expanded = false
                }
            )
            StatusMenuEntry(
                status = LearningStatus.LEARNED,
                selected = status == LearningStatus.LEARNED,
                onClick = {
                    onPick(LearningStatus.LEARNED)
                    expanded = false
                }
            )
        }
    }
}

/**
 * Single row inside the status dropdown. Shows the status icon plus
 * the localised label and bolds the entry that matches the current
 * selection so the user can see what's already set.
 */
@Composable
private fun StatusMenuEntry(
    status: LearningStatus,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalArcadePalette.current
    val icon = when (status) {
        LearningStatus.NOT_LEARNED -> Icons.Filled.RadioButtonUnchecked
        LearningStatus.ALMOST -> Icons.Outlined.CompareArrows
        LearningStatus.LEARNED -> Icons.Filled.CheckCircle
    }
    val tint = when (status) {
        LearningStatus.NOT_LEARNED -> palette.textDim
        LearningStatus.ALMOST -> palette.highlight
        LearningStatus.LEARNED -> palette.success
    }
    val labelRes = when (status) {
        LearningStatus.NOT_LEARNED -> R.string.words_status_not_learned
        LearningStatus.ALMOST -> R.string.words_status_almost
        LearningStatus.LEARNED -> R.string.words_status_learned
    }
    DropdownMenuItem(
        text = {
            Text(
                text = stringResource(id = labelRes),
                color = palette.textMain,
                fontFamily = ArcadeFonts.Pixel,
                fontWeight = if (selected) ArcadeFonts.PixelWeight else ArcadeFonts.PixelWeight,
                fontSize = 12.sp
            )
        },
        leadingIcon = {
            Icon(imageVector = icon, contentDescription = null, tint = tint)
        },
        onClick = onClick
    )
}

/**
 * Per-level chip whose colour cycles through the arcade palette so
 * the player can tell levels apart at a glance. Levels beyond 8 wrap
 * with `mod 8`. The colour is also what the level header in the
 * game-level selectors uses, so seeing "L3 = pink" here lines up with
 * the visual cue the rest of the app already establishes.
 */
@Composable
private fun LevelChip(level: Int) {
    val palette = LocalArcadePalette.current
    val color = levelColor(level)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.20f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = "L$level",
            color = color,
            fontFamily = ArcadeFonts.Pixel,
            fontWeight = ArcadeFonts.PixelWeight,
            fontSize = 9.sp
        )
    }
}

/**
 * Rounded source badge used to communicate whether a row comes from
 * the bundled dictionary (`Dictionary`) or was added by the learner
 * (`Mine`).
 */
@Composable
private fun SourceBadge(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(containerColor.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = label,
                color = contentColor,
                fontFamily = ArcadeFonts.Pixel,
                fontWeight = ArcadeFonts.PixelWeight,
                fontSize = 9.sp
            )
        }
    }
}

/** Generic pill for non-source metadata (type, etc.). */
@Composable
private fun MetaChip(text: String) {
    val palette = LocalArcadePalette.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(palette.surfaceDark)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = palette.textMain,
            fontFamily = ArcadeFonts.Pixel,
            fontWeight = ArcadeFonts.PixelWeight,
            fontSize = 9.sp
        )
    }
}

/** Difficulty chip reused by the card header. */
@Composable
private fun DifficultyChip(difficulty: WordCardDifficulty) {
    val color = difficulty.color()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = stringResource(id = difficulty.labelRes),
            color = color,
            fontFamily = ArcadeFonts.Pixel,
            fontWeight = ArcadeFonts.PixelWeight,
            fontSize = 9.sp
        )
    }
}

/** Section wrapper used by every detail block. */
@Composable
private fun DetailSection(title: String, content: @Composable () -> Unit) {
    val palette = LocalArcadePalette.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            color = palette.textDim,
            fontFamily = ArcadeFonts.Pixel,
            fontWeight = ArcadeFonts.PixelWeight,
            fontSize = 10.sp
        )
        content()
    }
}

/**
 * Two-column key/value table that renders every verb form.
 *
 * Each row carries its own inline 🔊 button so the learner can hear
 * the base, third-person, present participle, past simple and past
 * participle in isolation — no need to navigate back to the header
 * for a "speak past" shortcut.
 *
 * All five buttons share the **same** idle colour (`palette.textMain`,
 * a neutral white-on-navy / navy-on-cream depending on the active
 * theme) so the row reads as a uniform block of "speak this form"
 * controls. Only the currently-active utterance tints to
 * `palette.success` (green) so the user can always tell which row is
 * being spoken at a glance — no row stays green after the TTS
 * callback finishes.
 *
 * The button is disabled when the cell is empty (`"—"`) so the
 * layout stays aligned and the player can see at a glance which
 * forms the dictionary knows about for this verb.
 */
@Composable
private fun FormsTable(
    forms: data.database.entities.Forms,
    onSpeak: (String) -> Unit,
    isSpeaking: (String) -> Boolean
) {
    val rows = listOf(
        FormsRowSpec(
            labelRes = R.string.words_forms_base,
            value = forms.base
        ),
        FormsRowSpec(
            labelRes = R.string.words_forms_third,
            value = forms.thirdPerson
        ),
        FormsRowSpec(
            labelRes = R.string.words_forms_participle,
            value = forms.presentParticiple
        ),
        FormsRowSpec(
            labelRes = R.string.words_forms_past,
            value = forms.pastSimple
        ),
        FormsRowSpec(
            labelRes = R.string.words_forms_past_participle,
            value = forms.pastParticiple
        )
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEach { row ->
            FormsRow(
                label = stringResource(id = row.labelRes),
                value = row.value,
                onSpeak = onSpeak,
                isSpeaking = isSpeaking
            )
        }
    }
}

/**
 * Single key/value row inside [FormsTable]. Carries an inline 🔊
 * button on the trailing edge that speaks [value] when tapped. The
 * button auto-disables on empty values so the table keeps its
 * rectangular shape.
 *
 * Tint logic:
 *  - empty value → faded grey (signals "nothing to speak")
 *  - actively speaking this form → `palette.success` (green)
 *  - otherwise → `palette.textMain` (neutral, identical for every row)
 */
@Composable
private fun FormsRow(
    label: String,
    value: String?,
    onSpeak: (String) -> Unit,
    isSpeaking: (String) -> Boolean
) {
    val palette = LocalArcadePalette.current
    val hasValue = !value.isNullOrBlank()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = palette.textDim,
            fontFamily = ArcadeFonts.Pixel,
            fontWeight = ArcadeFonts.PixelWeight,
            fontSize = 10.sp,
            modifier = Modifier.weight(0.40f)
        )
        Text(
            text = value?.takeIf { it.isNotBlank() } ?: "—",
            color = palette.textMain,
            fontFamily = ArcadeFonts.Pixel,
            fontWeight = ArcadeFonts.PixelWeight,
            fontSize = 11.sp,
            modifier = Modifier.weight(0.45f)
        )
        IconButton(
            onClick = { value?.takeIf { it.isNotBlank() }?.let(onSpeak) },
            enabled = hasValue,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = stringResource(
                    id = R.string.words_action_speak_form
                ),
                tint = when {
                    !hasValue -> palette.textDim.copy(alpha = 0.35f)
                    isSpeaking(value.orEmpty()) -> palette.success
                    else -> palette.textMain
                }
            )
        }
    }
}

/**
 * Declarative description of a single row inside [FormsTable]. Kept
 * outside the composable so the table composable stays declarative
 * and the row layout can be shared.
 */
private data class FormsRowSpec(
    val labelRes: Int,
    val value: String?
)

/** Bilingual example list with CEFR badges. */
@Composable
private fun ExamplesList(examples: List<data.database.entities.Example>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        examples.forEach { ex ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LevelBadge(level = ex.level)
                    Text(
                        text = ex.english,
                        color = LocalArcadePalette.current.textMain,
                        fontFamily = ArcadeFonts.Pixel,
                        fontWeight = ArcadeFonts.PixelWeight,
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = ex.spanish,
                    color = LocalArcadePalette.current.textDim,
                    fontFamily = ArcadeFonts.Pixel,
                    fontWeight = ArcadeFonts.PixelWeight,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(start = 40.dp)
                )
            }
        }
    }
}

/**
 * Small colored badge that surfaces the CEFR level of an example.
 * Reads from the active arcade palette so the badge stays consistent
 * with the rest of the level-coloured chips.
 */
@Composable
private fun LevelBadge(level: String) {
    val palette = LocalArcadePalette.current
    val containerColor = when (level.uppercase()) {
        "A1", "A2" -> palette.primary
        "B1", "B2" -> palette.secondary
        else -> palette.highlight
    }
    Box(
        modifier = Modifier
            .size(width = 32.dp, height = 20.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(containerColor.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = level,
            color = containerColor,
            fontFamily = ArcadeFonts.Pixel,
            fontWeight = ArcadeFonts.PixelWeight,
            fontSize = 9.sp
        )
    }
}

/** Wrapping row of pills for string lists. */
@Composable
private fun ChipStrip(items: List<String>) {
    val palette = LocalArcadePalette.current
    // The screen uses regular FlowRow via Row + spacedBy because FlowRow
    // is part of compose.foundation and is API-stable in the BOM we use.
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(palette.surfaceDark)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = item,
                    color = palette.textMain,
                    fontFamily = ArcadeFonts.Pixel,
                    fontWeight = ArcadeFonts.PixelWeight,
                    fontSize = 9.sp
                )
            }
        }
    }
}
// endregion

/**
 * Difficulty levels surfaced by [WordCard]. The colour tokens are
 * resolved against the live [LocalArcadePalette] so the pill stays
 * consistent with the rest of the arcade in both themes.
 */
enum class WordCardDifficulty(val labelRes: Int) {
    EASY(R.string.games_difficulty_easy),
    MEDIUM(R.string.games_difficulty_medium),
    HARD(R.string.games_difficulty_hard);

    @Composable
    fun color(): Color = when (this) {
        EASY -> LocalArcadePalette.current.primary
        MEDIUM -> LocalArcadePalette.current.secondary
        HARD -> LocalArcadePalette.current.error
    }
}

// region: Mapping helpers

/**
 * Resolves the arcade colour for a dictionary [level]. The eight hues
 * cycle in the same order the dashboard uses for its category accents
 * so the level chip reads as "this is part of the same brand language".
 *
 * Levels outside `1..8` wrap with `mod 8` so a hypothetical level 12
 * still renders something distinct.
 *
 * Reads the live [LocalArcadePalette] so the chip flips between dark
 * and light with the rest of the UI, hence the `@Composable` annotation.
 */
@Composable
private fun levelColor(level: Int): Color {
    val palette = LocalArcadePalette.current
    val colors = listOf(
        palette.highlight,   // L1 — gold (ADJECTIVES)
        palette.secondary,   // L2 — cyan (NOUNS)
        palette.primary,     // L3 — pink (VERBS)
        palette.success,     // L4 — green (INTERJECTIONS)
        palette.textDim,     // L5 — muted blue (ADVERBS, no own hue)
        palette.error,       // L6 — red (CONJUNCTIONS stand-in)
        palette.border,      // L7 — blue-grey (PREPOSITIONS stand-in)
        palette.primary      // L8 — pink again (wrap)
    )
    val index = ((level - 1).coerceAtLeast(0)) % colors.size
    return colors[index]
}

/** Converts the Room-backed [Difficulty] enum to the UI-facing [WordCardDifficulty]. */
private fun Difficulty.toCardDifficulty(): WordCardDifficulty = when (this) {
    Difficulty.EASY -> WordCardDifficulty.EASY
    Difficulty.MEDIUM -> WordCardDifficulty.MEDIUM
    Difficulty.HARD -> WordCardDifficulty.HARD
}
// endregion