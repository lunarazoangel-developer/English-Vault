package com.example.englishvault.ui.words.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.englishvault.R
import data.database.entities.Difficulty
import data.database.entities.WordEntity
import data.database.entities.isUserAdded

/**
 * Single card representing a vocabulary word in the Words screen.
 *
 * Phase 2.5: receives a fully formed [WordEntity] from the parent
 * screen so callers do not have to map fields manually.
 *
 * Delete is gated by the entity's [isUserAdded] flag: default / seeded
 * entries are completely read-only. Only entries the user added through
 * the form expose destructive actions, identified by a "Mine" badge.
 *
 * @param entity Word backing this card.
 * @param onEdit Called when the pencil icon is tapped. Ignored when the
 *   row is not user-owned.
 * @param onDelete Called when the trash icon is tapped. Ignored when
 *   the row is not user-owned.
 */
@Composable
fun WordCard(
    entity: WordEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUserAdded = entity.isUserAdded()
    WordCard(
        word = entity.word,
        translation = entity.translation,
        type = entity.type,
        difficulty = entity.difficulty.toCardDifficulty(),
        isUserAdded = isUserAdded,
        onEdit = onEdit,
        onDelete = onDelete,
        modifier = modifier
    )
}

/**
 * Lower-level overload kept for callers that already have the card
 * fields on hand (tests, future preview tooling, …).
 */
@Composable
fun WordCard(
    word: String,
    translation: String,
    type: String,
    difficulty: WordCardDifficulty,
    isUserAdded: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                Column(modifier = Modifier.weight(1f)) {
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
                // Default / seeded words render no action buttons; the
                // absence of controls visually communicates that the row
                // is read-only.
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isUserAdded) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = stringResource(id = R.string.words_badge_mine),
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                            labelColor = MaterialTheme.colorScheme.tertiary
                        )
                    )
                }
                AssistChip(
                    onClick = {},
                    label = {
                        Text(text = type, style = MaterialTheme.typography.labelSmall)
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
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
        }
    }
}

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