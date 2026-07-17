package com.example.englishvault.ui.games.listening

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
import com.example.englishvault.ui.games.listening.model.ListeningError
import com.example.englishvault.ui.games.listening.model.ListeningGameState
import com.example.englishvault.ui.progress.arcade.ArcadeFonts
import com.example.englishvault.ui.progress.arcade.LocalArcadePalette
import com.example.englishvault.ui.progress.arcade.components.ArcadeButton

/**
 * Results panel for a finished Listening run. Rendered in place
 * inside [ListeningGameScreen] when the VM transitions to
 * [ListeningGameState.Finished], avoiding a separate navigation
 * hop and the cross-VM state sharing it would require.
 *
 * Shows the score ("X / Y correct"), the per-error breakdown so the
 * learner can see which words tripped them up, and a "Play again"
 * button that triggers a fresh run at the same level.
 *
 * Uses the arcade palette end-to-end: primary "Play again" goes
 * through [ArcadeButton]; the secondary "Back to games" is the
 * pixel-bordered outlined variant declared at the bottom of this
 * file; the score card and per-error rows tint off the primary /
 * error accents so they stay legible in both themes.
 *
 * @param state The finished game state straight from the VM.
 * @param onPlayAgain Invoked when the user wants to retry the same
 *   level. The screen drives this by calling
 *   [com.example.englishvault.ui.games.listening.viewmodel.ListeningViewModel.startGame]
 *   with the level it already received.
 * @param onExit Invoked when the user wants to leave the game (the
 *   parent navigates back to the Games tab).
 */
@Composable
fun ListeningEndContent(
    state: ListeningGameState.Finished,
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
            text = stringResource(id = R.string.game_listening_game_over),
            color = palette.textMain,
            fontFamily = ArcadeFonts.Display,
            fontWeight = ArcadeFonts.DisplayWeight,
            fontSize = 24.sp
        )

        ScoreCard(correct = state.correctCount, total = state.totalQuestions)

        if (state.errors.isNotEmpty()) {
            Text(
                text = stringResource(id = R.string.game_listening_errors_title),
                color = palette.textMain,
                fontFamily = ArcadeFonts.Display,
                fontWeight = ArcadeFonts.DisplayWeight,
                fontSize = 16.sp
            )
            state.errors.forEach { error ->
                ErrorRow(error = error)
            }
        } else {
            Text(
                text = stringResource(id = R.string.game_listening_perfect),
                color = palette.success,
                fontFamily = ArcadeFonts.Pixel,
                fontWeight = ArcadeFonts.PixelWeight,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        ArcadeButton(
            text = stringResource(id = R.string.game_listening_play_again),
            onClick = onPlayAgain,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedArcadeButton(
            text = stringResource(id = R.string.game_listening_back_games),
            onClick = onExit,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * End-of-run score card. The fill is a tinted primary so it pops
 * against the screen background without competing with the headline
 * above; the progress bar uses the success accent so the run reads
 * as "you did well" without extra copy.
 */
@Composable
private fun ScoreCard(correct: Int, total: Int) {
    val palette = LocalArcadePalette.current
    val fraction = if (total <= 0) 0f else correct.toFloat() / total
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
            Text(
                text = stringResource(
                    id = R.string.game_listening_score_format,
                    correct,
                    total
                ),
                color = palette.textMain,
                fontFamily = ArcadeFonts.Display,
                fontWeight = ArcadeFonts.DisplayWeight,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { fraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .padding(horizontal = 8.dp),
                color = palette.success,
                trackColor = palette.textMain.copy(alpha = 0.18f)
            )
        }
    }
}

@Composable
private fun ErrorRow(error: ListeningError) {
    val palette = LocalArcadePalette.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = palette.error.copy(alpha = 0.18f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(
                    id = R.string.game_listening_error_correct_format,
                    error.question.correctAnswer
                ),
                color = palette.error,
                fontFamily = ArcadeFonts.Display,
                fontWeight = ArcadeFonts.DisplayWeight,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            val picked = error.userPicked.ifBlank { "—" }
            Text(
                text = stringResource(
                    id = R.string.game_listening_error_user_format,
                    picked
                ),
                color = palette.textMain,
                fontFamily = ArcadeFonts.Pixel,
                fontWeight = ArcadeFonts.PixelWeight,
                fontSize = 11.sp
            )
        }
    }
}

/**
 * Secondary arcade button (matching the one in
 * [com.example.englishvault.ui.games.lettersoup.LetterSoupEndScreen]):
 * surface fill, 2 dp border in `palette.border`, pixel-font label.
 * Kept inline because only the three end screens consume it.
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