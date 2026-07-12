package com.example.englishvault.ui.games.wordmatchverbs

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
import com.example.englishvault.ui.games.wordmatchverbs.model.WordMatchError
import com.example.englishvault.ui.games.wordmatchverbs.model.WordMatchGameState

/**
 * Results panel for a finished Word Match Verbs run. Rendered in
 * place inside [WordMatchVerbsGameScreen] when the VM transitions to
 * [WordMatchGameState.Finished], avoiding a separate navigation
 * hop and the cross-VM state sharing it would require.
 *
 * Shows the score ("X / Y correct"), the per-error breakdown so the
 * learner can see which words tripped them up, and a "Play again"
 * button that triggers a fresh run at the same level.
 *
 * @param state The finished game state straight from the VM.
 * @param onPlayAgain Invoked when the user wants to retry the same
 *   level. The screen drives this by calling
 *   [com.example.englishvault.ui.games.wordmatchverbs.viewmodel.WordMatchVerbsViewModel.startGame]
 *   with the level it already received.
 * @param onExit Invoked when the user wants to leave the game (the
 *   parent navigates back to the Games tab).
 */
@Composable
fun WordMatchVerbsEndContent(
    state: WordMatchGameState.Finished,
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
            text = stringResource(id = R.string.game_wordmatch_game_over),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        ScoreCard(correct = state.correctCount, total = state.totalQuestions)

        if (state.errors.isNotEmpty()) {
            Text(
                text = stringResource(id = R.string.game_wordmatch_errors_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            state.errors.forEach { error ->
                ErrorRow(error = error)
            }
        } else {
            Text(
                text = stringResource(id = R.string.game_wordmatch_perfect),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onPlayAgain,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(id = R.string.game_wordmatch_play_again),
                fontWeight = FontWeight.SemiBold
            )
        }
        OutlinedButton(
            onClick = onExit,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.game_wordmatch_back_games))
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ScoreCard(correct: Int, total: Int) {
    val fraction = if (total <= 0) 0f else correct.toFloat() / total
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
            Text(
                text = stringResource(
                    id = R.string.game_wordmatch_score_format,
                    correct,
                    total
                ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { fraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .padding(horizontal = 8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
            )
        }
    }
}

@Composable
private fun ErrorRow(error: WordMatchError) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = error.question.baseWord,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(id = error.question.askType.promptResId),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.75f)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(
                    id = R.string.game_wordmatch_error_user_format,
                    error.userPicked
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = stringResource(
                    id = R.string.game_wordmatch_error_correct_format,
                    error.question.correctAnswer
                ),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
