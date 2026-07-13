package com.example.englishvault.ui.games.lettersoup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.englishvault.R

/**
 * End-of-run panel for the Letter Soup mini-game. Rendered in place
 * inside [LetterSoupGameScreen] when the VM transitions to
 * [com.example.englishvault.ui.games.lettersoup.model.LetterSoupGameState.Finished],
 * avoiding a separate navigation hop and the cross-VM state sharing
 * it would require.
 *
 * Shows the win / lose headline, the score (`wordsFixed / 5`), a
 * progress bar so the player can see how close they got, and the
 * standard "Play again" + "Back to games" buttons.
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(
                id = if (won) R.string.game_lettersoup_win else R.string.game_lettersoup_lose
            ),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = if (won) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )

        Text(
            text = stringResource(id = R.string.game_lettersoup_level_format, level),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ScoreCard(wordsFixed = wordsFixed, wordsToWin = wordsToWin, won = won)

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onPlayAgain,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(id = R.string.game_lettersoup_play_again),
                fontWeight = FontWeight.SemiBold
            )
        }
        OutlinedButton(
            onClick = onExit,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.game_lettersoup_back_games))
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ScoreCard(wordsFixed: Int, wordsToWin: Int, won: Boolean) {
    val fraction = (wordsFixed.toFloat() / wordsToWin).coerceIn(0f, 1f)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
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
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    id = if (won) R.string.game_lettersoup_win_subtitle else R.string.game_lettersoup_lose_subtitle,
                    wordsFixed,
                    wordsToWin
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .padding(horizontal = 8.dp),
                color = if (won) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
            )
        }
    }
}