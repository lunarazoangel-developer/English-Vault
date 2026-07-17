package com.example.englishvault.ui.games.lettersoup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.englishvault.R
import com.example.englishvault.ui.progress.arcade.ArcadeFonts
import com.example.englishvault.ui.progress.arcade.LocalArcadePalette
import com.example.englishvault.ui.progress.arcade.components.ArcadeButton

/**
 * End-of-run panel for the Letter Soup mini-game. Rendered in place
 * inside [LetterSoupGameScreen] when the VM transitions to
 * [com.example.englishvault.ui.games.lettersoup.model.LetterSoupGameState.Finished],
 * avoiding a separate navigation hop and the cross-VM state sharing
 * it would require.
 *
 * Shows the win / lose headline, the score (`wordsFixed / 5`), a
 * progress bar so the player can see how close they got, and the
 * standard "Play again" + "Back to games" buttons (rendered with
 * the arcade 3D button + a thin outlined arcade button so the panel
 * stays on-brand with the rest of the arcade UI).
 */
@Composable
fun LetterSoupEndContent(
    level: Int,
    won: Boolean,
    wordsFixed: Int,
    wordsToWin: Int,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalArcadePalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(
                id = if (won) R.string.game_lettersoup_win else R.string.game_lettersoup_lose
            ),
            color = if (won) palette.textMain else palette.error,
            fontFamily = ArcadeFonts.Display,
            fontWeight = ArcadeFonts.DisplayWeight,
            fontSize = 24.sp
        )

        Text(
            text = stringResource(id = R.string.game_lettersoup_level_format, level),
            color = palette.textDim,
            fontFamily = ArcadeFonts.Pixel,
            fontWeight = ArcadeFonts.PixelWeight,
            fontSize = 13.sp
        )

        ScoreCard(wordsFixed = wordsFixed, wordsToWin = wordsToWin, won = won)

        Spacer(modifier = Modifier.height(8.dp))

        ArcadeButton(
            text = stringResource(id = R.string.game_lettersoup_play_again),
            onClick = onPlayAgain,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedArcadeButton(
            text = stringResource(id = R.string.game_lettersoup_back_games),
            onClick = onExit,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * End-of-run score card. The card body uses
 * `palette.primary.copy(alpha = 0.18f)` so the fill honours the
 * pink accent without competing with the win / lose headline above
 * it. The progress bar tints itself green (won) or red (lost) so
 * the player reads the outcome at a glance even before reading the
 * copy.
 */
@Composable
private fun ScoreCard(wordsFixed: Int, wordsToWin: Int, won: Boolean) {
    val palette = LocalArcadePalette.current
    val fraction = (wordsFixed.toFloat() / wordsToWin).coerceIn(0f, 1f)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = palette.primary.copy(alpha = 0.18f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(
                        id = R.string.game_lettersoup_words_format,
                        wordsFixed,
                        wordsToWin
                    ),
                    color = palette.textMain,
                    fontFamily = ArcadeFonts.Display,
                    fontWeight = ArcadeFonts.DisplayWeight,
                    fontSize = 24.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    id = if (won) R.string.game_lettersoup_win_subtitle else R.string.game_lettersoup_lose_subtitle,
                    wordsFixed,
                    wordsToWin
                ),
                color = palette.textMain.copy(alpha = 0.78f),
                fontFamily = ArcadeFonts.Pixel,
                fontWeight = ArcadeFonts.PixelWeight,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .padding(horizontal = 8.dp),
                color = if (won) palette.success else palette.error,
                trackColor = palette.textMain.copy(alpha = 0.18f)
            )
        }
    }
}

/**
 * Secondary arcade button: surface fill, single-pixel border in
 * `palette.border`, pixel-font label. Used for "Back to games"
 * where the primary action already carries the pink emphasis.
 *
 * Kept inline (not in `arcade/components`) because only the three
 * end screens need it; promoting it would be premature for a
 * single-screen helper.
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
        contentAlignment = Alignment.Center
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