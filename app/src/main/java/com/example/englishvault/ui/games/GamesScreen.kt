package com.example.englishvault.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.englishvault.R

/**
 * Games zone — a 2-column grid of mini-game cards.
 *
 * Phase 2 mockup. Tapping a card does nothing; Phase 3 will route each
 * entry to its corresponding game screen.
 *
 * Phase 6: the "Word Match Verbs" card is wired to the new
 * Word Match Verbs mini-game via [onOpenWordMatchVerbs].
 *
 * Phase 7.3: the "Letter Soup" card replaces the placeholder
 * "Memory Cards" entry and routes to the Letter Soup mini-game via
 * [onOpenLetterSoup].
 *
 * Each card shows a colored icon, the game name, a short description
 * and a difficulty chip.
 */
@Composable
fun GamesScreen(
    onOpenWordMatchVerbs: () -> Unit,
    onOpenLetterSoup: () -> Unit,
    modifier: Modifier = Modifier
) {
    // region: Mock games catalogue — wire to real sources in Phase 3
    val games = listOf(
        GameItem(
            name = "Word Match Verbs",
            description = "Match English words with their translations.",
            icon = Icons.Filled.Psychology,
            color = MaterialTheme.colorScheme.primary,
            difficulty = DifficultyLevel.EASY,
            isInteractive = true
        ),
        GameItem(
            name = "Speed Quiz",
            description = "How fast can you pick the correct option?",
            icon = Icons.Filled.Speed,
            color = MaterialTheme.colorScheme.secondary,
            difficulty = DifficultyLevel.MEDIUM,
            isInteractive = false
        ),
        GameItem(
            name = "Letter Soup",
            description = "Swap letters to fix the broken words.",
            icon = Icons.Filled.Memory,
            color = MaterialTheme.colorScheme.tertiary,
            difficulty = DifficultyLevel.MEDIUM,
            isInteractive = true
        ),
        GameItem(
            name = "Listening",
            description = "Hear a word and type what you understood.",
            icon = Icons.Filled.Headphones,
            color = MaterialTheme.colorScheme.primary,
            difficulty = DifficultyLevel.MEDIUM,
            isInteractive = false
        ),
        GameItem(
            name = "Fill the Blank",
            description = "Complete sentences with the right word.",
            icon = Icons.Filled.Bolt,
            color = MaterialTheme.colorScheme.secondary,
            difficulty = DifficultyLevel.HARD,
            isInteractive = false
        ),
        GameItem(
            name = "Translation Race",
            description = "Translate as many words as you can in 60s.",
            icon = Icons.Filled.Translate,
            color = MaterialTheme.colorScheme.tertiary,
            difficulty = DifficultyLevel.HARD,
            isInteractive = false
        )
    )
    // endregion

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = R.string.games_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(id = R.string.games_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(games, key = { it.name }) { game ->
                GameCard(
                    game = game,
                    onClick = {
                        if (!game.isInteractive) return@GameCard
                        when (game.name) {
                            "Word Match Verbs" -> onOpenWordMatchVerbs()
                            "Letter Soup" -> onOpenLetterSoup()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun GameCard(game: GameItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(enabled = game.isInteractive, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(game.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = game.icon,
                    contentDescription = null,
                    tint = game.color
                )
            }
            Column {
                Text(
                    text = game.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = game.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(8.dp))
                DifficultyChip(level = game.difficulty)
            }
        }
    }
}

@Composable
private fun DifficultyChip(level: DifficultyLevel) {
    val (labelRes, color) = when (level) {
        DifficultyLevel.EASY -> R.string.games_difficulty_easy to MaterialTheme.colorScheme.primary
        DifficultyLevel.MEDIUM -> R.string.games_difficulty_medium to MaterialTheme.colorScheme.secondary
        DifficultyLevel.HARD -> R.string.games_difficulty_hard to MaterialTheme.colorScheme.error
    }
    AssistChip(
        onClick = {},
        label = {
            Text(
                text = stringResource(id = labelRes),
                style = MaterialTheme.typography.labelSmall
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.12f),
            labelColor = color
        )
    )
}

// region: Local mock models (Phase 2 only)
private data class GameItem(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val difficulty: DifficultyLevel,
    /**
     * `true` when tapping the card navigates somewhere; `false`
     * when it is still a non-interactive placeholder (Phase 6 leaves
     * all but Word Match Verbs as placeholders).
     */
    val isInteractive: Boolean = false
)

private enum class DifficultyLevel { EASY, MEDIUM, HARD }
// endregion
