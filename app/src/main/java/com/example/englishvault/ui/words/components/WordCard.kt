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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.englishvault.R
import data.database.entities.Difficulty
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
 *  - Expandable detail section with pronunciation, verb forms,
 *    examples, synonyms, antonyms, tags and category.
 *
 * Expand / collapse is controlled by the parent so the surrounding
 * screen can persist the state across configuration changes via
 * `rememberSaveable`.
 *
 * Delete and edit are exposed only for user-owned rows
 * (see [isUserAdded]).
 *
 * @param entity Word backing this card.
 * @param expanded Whether the detail section is currently visible.
 * @param onToggle Called when the user taps the card body to expand
 *   or collapse the detail section.
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
        pronunciation = entity.pronunciation?.ipa,
        forms = entity.forms,
        examples = entity.examples,
        synonyms = entity.synonyms,
        antonyms = entity.antonyms,
        tags = entity.tags,
        category = entity.category,
        expanded = expanded,
        onToggle = onToggle,
        onEdit = onEdit,
        onDelete = onDelete,
        modifier = modifier
    )
}

/**
 * Lower-level overload kept for previews and tests that already have
 * every field on hand.
 */
@Composable
fun WordCard(
    word: String,
    translation: String,
    type: String,
    difficulty: WordCardDifficulty,
    isUserAdded: Boolean,
    pronunciation: String?,
    forms: data.database.entities.Forms?,
    examples: List<data.database.entities.Example>?,
    synonyms: List<String>?,
    antonyms: List<String>?,
    tags: List<String>?,
    category: List<String>?,
    expanded: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isUserAdded) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surface
    }

    val toggleDescription = stringResource(
        if (expanded) R.string.words_card_collapse else R.string.words_card_expand
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Chevron rotates to communicate expand / collapse.
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = toggleDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(if (expanded) 90f else 0f)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = word.firstOrNull()?.uppercase().orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = translation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isUserAdded) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(id = R.string.words_action_edit),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(id = R.string.words_action_delete),
                            tint = MaterialTheme.colorScheme.error
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
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    )
                } else {
                    SourceBadge(
                        label = stringResource(id = R.string.words_badge_dictionary),
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                }
                MetaChip(text = type)
                DifficultyChip(difficulty = difficulty)
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
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    forms?.let { f ->
                        DetailSection(title = stringResource(id = R.string.words_section_forms)) {
                            FormsTable(forms = f)
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
    AssistChip(
        onClick = {},
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = contentColor
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = containerColor.copy(alpha = 0.18f),
            labelColor = contentColor
        )
    )
}

/** Generic pill for non-source metadata (type, etc.). */
@Composable
private fun MetaChip(text: String) {
    AssistChip(
        onClick = {},
        label = {
            Text(text = text, style = MaterialTheme.typography.labelSmall)
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

/** Difficulty chip reused by the card header. */
@Composable
private fun DifficultyChip(difficulty: WordCardDifficulty) {
    AssistChip(
        onClick = {},
        label = {
            Text(
                text = stringResource(id = difficulty.labelRes),
                style = MaterialTheme.typography.labelSmall
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = difficulty.color().copy(alpha = 0.15f),
            labelColor = difficulty.color()
        )
    )
}

/** Section wrapper used by every detail block. */
@Composable
private fun DetailSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        content()
    }
}

/** Two-column key/value table that renders every verb form. */
@Composable
private fun FormsTable(forms: data.database.entities.Forms) {
    val rows = listOf(
        stringResource(id = R.string.words_forms_base) to forms.base,
        stringResource(id = R.string.words_forms_third) to forms.thirdPerson,
        stringResource(id = R.string.words_forms_participle) to forms.presentParticiple,
        stringResource(id = R.string.words_forms_past) to forms.pastSimple,
        stringResource(id = R.string.words_forms_past_participle) to forms.pastParticiple
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEach { (label, value) ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(0.45f)
                )
                Text(
                    text = value?.takeIf { it.isNotBlank() } ?: "—",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(0.55f)
                )
            }
        }
    }
}

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
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = ex.spanish,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 40.dp)
                )
            }
        }
    }
}

/** Small colored badge that surfaces the CEFR level of an example. */
@Composable
private fun LevelBadge(level: String) {
    val containerColor = when (level.uppercase()) {
        "A1", "A2" -> MaterialTheme.colorScheme.primary
        "B1", "B2" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.tertiary
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
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = containerColor
        )
    }
}

/** Wrapping row of pills for string lists. */
@Composable
private fun ChipStrip(items: List<String>) {
    // The screen uses regular FlowRow via Row + spacedBy because FlowRow
    // is part of compose.foundation and is API-stable in the BOM we use.
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            AssistChip(
                onClick = {},
                label = {
                    Text(text = item, style = MaterialTheme.typography.labelSmall)
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
// endregion

/** Difficulty levels surfaced by [WordCard]. */
enum class WordCardDifficulty(val labelRes: Int) {
    EASY(R.string.games_difficulty_easy),
    MEDIUM(R.string.games_difficulty_medium),
    HARD(R.string.games_difficulty_hard);

    /**
     * Color used by the difficulty chip. Reads from the active Material
     * color scheme so it respects light/dark themes.
     */
    @Composable
    fun color(): Color = when (this) {
        EASY -> MaterialTheme.colorScheme.primary
        MEDIUM -> MaterialTheme.colorScheme.secondary
        HARD -> MaterialTheme.colorScheme.error
    }
}

// region: Mapping helpers
/** Converts the Room-backed [Difficulty] enum to the UI-facing [WordCardDifficulty]. */
private fun Difficulty.toCardDifficulty(): WordCardDifficulty = when (this) {
    Difficulty.EASY -> WordCardDifficulty.EASY
    Difficulty.MEDIUM -> WordCardDifficulty.MEDIUM
    Difficulty.HARD -> WordCardDifficulty.HARD
}
// endregion