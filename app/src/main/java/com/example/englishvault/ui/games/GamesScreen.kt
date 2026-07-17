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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.englishvault.R
import com.example.englishvault.ui.progress.arcade.ArcadeFonts
import com.example.englishvault.ui.progress.arcade.LocalArcadePalette

/**
 * Games zone — a 2-column grid of mini-game cards, rendered in the
 * arcade style.
 *
 * Phase 6: the "Word Match Verbs" card is wired to the new
 * Word Match Verbs mini-game via [onOpenWordMatchVerbs].
 *
 * Phase 7.3: the "Letter Soup" card replaces the placeholder
 * "Memory Cards" entry and routes to the Letter Soup mini-game via
 * [onOpenLetterSoup].
 *
 * Phase 7.5: the "Listening" card becomes interactive and routes to
 * the Listening mini-game via [onOpenListening].
 *
 * Each card carries its own accent colour drawn from the arcade
 * category palette (see [ArcadeFonts] / `ArcadePalette.categoryColor`
 * for the source set) so every game reads as distinct at a glance.
 * The cards themselves use `palette.surface` so the colour pop comes
 * from the icon disc + the difficulty chip, not from a full-colour
 * background tile.
 */
@Composable
fun GamesScreen(
    onOpenWordMatchVerbs: () -> Unit,
    onOpenLetterSoup: () -> Unit,
    onOpenListening: () -> Unit,
    modifier: Modifier = Modifier
) {
    // region: Games catalogue — wire to real sources as Phase 3 lands
    val games = listOf(
        GameItem(
            name = "Word Match Verbs",
            description = "Match English words with their translations.",
            icon = Icons.Filled.Psychology,
            color = Color(0xFFFF007A),
            difficulty = DifficultyLevel.EASY,
            isInteractive = true
        ),
        GameItem(
            name = "Speed Quiz",
            description = "How fast can you pick the correct option?",
            icon = Icons.Filled.Speed,
            color = Color(0xFF9D4EDD),
            difficulty = DifficultyLevel.MEDIUM,
            isInteractive = false
        ),
        GameItem(
            name = "Letter Soup",
            description = "Swap letters to fix the broken words.",
            icon = Icons.Filled.Memory,
            color = Color(0xFFFFD700),
            difficulty = DifficultyLevel.MEDIUM,
            isInteractive = true
        ),
        GameItem(
            name = "Listening",
            description = "Hear a word and pick the correct spelling.",
            icon = Icons.Filled.Headphones,
            color = Color(0xFF5FB878),
            difficulty = DifficultyLevel.MEDIUM,
            isInteractive = true
        ),
        GameItem(
            name = "Fill the Blank",
            description = "Complete sentences with the right word.",
            icon = Icons.Filled.Bolt,
            color = Color(0xFFFF8C00),
            difficulty = DifficultyLevel.HARD,
            isInteractive = false
        ),
        GameItem(
            name = "Translation Race",
            description = "Translate as many words as you can in 60s.",
            icon = Icons.Filled.Translate,
            color = Color(0xFF00D4FF),
            difficulty = DifficultyLevel.HARD,
            isInteractive = false
        )
    )
    // endregion

    val palette = LocalArcadePalette.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = R.string.games_title),
            color = palette.textMain,
            fontFamily = ArcadeFonts.Display,
            fontWeight = ArcadeFonts.DisplayWeight,
            fontSize = 22.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(id = R.string.games_subtitle),
            color = palette.textDim,
            fontFamily = ArcadeFonts.Pixel,
            fontWeight = ArcadeFonts.PixelWeight,
            fontSize = 11.sp
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
                            "Listening" -> onOpenListening()
                        }
                    }
                )
            }
        }
    }
}

/**
 * Square tile that hosts a single game entry. The accent colour is
 * painted into a circular icon disc (full opacity over a 15% alpha
 * halo) and re-used on the difficulty chip, while the rest of the
 * card surface stays neutral (`palette.surface`) so the colour pop
 * does not fight with neighbouring tiles in the 2-column grid.
 */
@Composable
private fun GameCard(game: GameItem, onClick: () -> Unit) {
    val palette = LocalArcadePalette.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(palette.surface)
            .clickable(enabled = game.isInteractive, onClick = onClick)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
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
                    color = palette.textMain,
                    fontFamily = ArcadeFonts.Display,
                    fontWeight = ArcadeFonts.DisplayWeight,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = game.description,
                    color = palette.textDim,
                    fontFamily = ArcadeFonts.Pixel,
                    fontWeight = ArcadeFonts.PixelWeight,
                    fontSize = 10.sp,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(8.dp))
                DifficultyChip(level = game.difficulty, accent = game.color)
            }
        }
    }
}

/**
 * Compact difficulty tag rendered with the arcade pixel font. The
 * chip carries the game's own accent colour by default so the
 * difficulty read stays visually tied to the card. The "Hard" level
 * is the exception — it deliberately uses `palette.error` so the
 * "this is the tough one" cue survives even if a future game picks
 * a low-contrast accent like the cream `PREPOSITIONS` hue.
 */
@Composable
private fun DifficultyChip(level: DifficultyLevel, accent: Color) {
    val palette = LocalArcadePalette.current
    val labelRes = when (level) {
        DifficultyLevel.EASY -> R.string.games_difficulty_easy
        DifficultyLevel.MEDIUM -> R.string.games_difficulty_medium
        DifficultyLevel.HARD -> R.string.games_difficulty_hard
    }
    val chipColor = when (level) {
        DifficultyLevel.HARD -> palette.error
        DifficultyLevel.EASY,
        DifficultyLevel.MEDIUM -> accent
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(chipColor.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = labelRes),
            color = chipColor,
            fontFamily = ArcadeFonts.Pixel,
            fontWeight = ArcadeFonts.PixelWeight,
            fontSize = 9.sp
        )
    }
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