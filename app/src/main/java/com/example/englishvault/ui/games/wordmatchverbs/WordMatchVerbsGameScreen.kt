package com.example.englishvault.ui.games.wordmatchverbs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.englishvault.R
import com.example.englishvault.ui.games.wordmatchverbs.model.WordMatchGameState
import com.example.englishvault.ui.games.wordmatchverbs.viewmodel.WordMatchVerbsViewModel
import kotlinx.coroutines.delay

/**
 * Active gameplay screen for Word Match Verbs.
 *
 * Receives the chosen [level] as a navigation argument and seeds
 * the VM on first composition. Renders one of three bodies
 * depending on the [WordMatchGameState]:
 *  - [WordMatchGameState.Loading] / [WordMatchGameState.Empty] â€”
 *    a full-screen branded loading panel with the app's blue
 *    gradient so the user never sees a plain "Loadingâ€¦" flash.
 *  - [WordMatchGameState.InProgress] â€” the verb + question prompt
 *    and three option cards, with a transient âœ“ / âœ— overlay that
 *    auto-advances after a short delay.
 *  - [WordMatchGameState.Finished] â€” the results panel rendered in
 *    place (see [WordMatchVerbsEndContent]); "Play again" resets the VM
 *    and "Back to games" hands control back to the parent.
 */
@Composable
fun WordMatchVerbsGameScreen(
    level: Int,
    onBack: () -> Unit,
    onExitToGames: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WordMatchVerbsViewModel = hiltViewModel()
) {
    val state by viewModel.gameState.collectAsState()

    // Seed the VM exactly once per level. Re-runs of the same level
    // (via "Play again") call `viewModel.startGame(level)` directly
    // from the end content, which transitions the state through
    // Loading â†’ InProgress on its own.
    LaunchedEffect(level) {
        viewModel.startGame(level)
    }

    // Auto-advance after the player answers. The delay gives them
    // enough time to read the feedback overlay before the next
    // question slides in.
    LaunchedEffect(state) {
        val s = state
        if (s is WordMatchGameState.InProgress && s.lastAnswer != null) {
            delay(1500)
            viewModel.acknowledgeAnswer()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.game_wordmatch_back),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = stringResource(id = R.string.game_wordmatch_header, level),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
        }

        when (val s = state) {
            is WordMatchGameState.Loading -> BrandedLoadingPanel()
            is WordMatchGameState.Empty -> BrandedEmptyPanel()
            is WordMatchGameState.InProgress -> InProgressState(
                state = s,
                onPicked = viewModel::submitAnswer
            )
            is WordMatchGameState.Finished -> WordMatchVerbsEndContent(
                state = s,
                onPlayAgain = { viewModel.startGame(level) },
                onExit = onExitToGames
            )
        }
    }
}

/**
 * Full-screen branded loading panel with the app's blue gradient
 * background, the game title and a centred
 * [CircularProgressIndicator]. Used while the VM queries Room and
 * builds the question list.
 */
@Composable
private fun BrandedLoadingPanel() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandedGradient()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(id = R.string.game_wordmatch_loading_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.game_wordmatch_loading_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 4.dp,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

/**
 * Variant of [BrandedLoadingPanel] used when the chosen level has
 * no eligible verbs. Shares the gradient and headline layout; the
 * spinner is replaced with the no-words message.
 */
@Composable
private fun BrandedEmptyPanel() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandedGradient()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Text(
                text = stringResource(id = R.string.game_wordmatch_loading_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.game_wordmatch_no_words),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Vertical gradient from the primary color at the top to a slightly
 * faded primary at the bottom. Used by the loading and empty panels
 * to give the user a branded splash instead of a plain text label.
 */
@Composable
private fun BrandedGradient(): Brush = Brush.verticalGradient(
    colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
    )
)

/**
 * Body of the gameplay screen. Renders the active question and the
 * three option cards, plus a transient feedback overlay driven by
 * [WordMatchGameState.InProgress.lastAnswer].
 */
@Composable
private fun InProgressState(
    state: WordMatchGameState.InProgress,
    onPicked: (String) -> Unit
) {
    val question = state.currentQuestion ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(
                id = R.string.game_wordmatch_progress,
                state.currentIndex + 1,
                state.totalQuestions
            ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Verb + question prompt.
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
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = question.baseWord,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = question.askType.promptResId),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Three option cards.
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(question.options) { option ->
                OptionCard(
                    text = option,
                    feedback = feedbackFor(option, state),
                    enabled = state.lastAnswer == null,
                    onClick = { onPicked(option) }
                )
            }
        }
    }
}

/**
 * Visual feedback for a single option card. Returned as a sealed
 * type so the [OptionCard] composable can pick the right tint and
 * icon without doing the comparison itself.
 *
 * Four states keep the colour and icon unambiguous when the user
 * answers wrong: a red X marks the option they picked; a blue check
 * highlights the correct answer they missed so the learner can see
 * at a glance which one was right. Distractors that were neither
 * picked nor correct stay neutral.
 */
private sealed class OptionFeedback {
    /** No answer submitted yet (or transition between questions). */
    object Neutral : OptionFeedback()

    /** This option is correct and the user picked it. Green check. */
    object CorrectPicked : OptionFeedback()

    /**
     * This option is the right answer but the user picked a different
     * one. Revealed in blue with a check so the learner can see what
     * would have been correct without confusing it with the wrong
     * pick (which carries a red X).
     */
    object RevealedCorrect : OptionFeedback()

    /** This option is what the user picked and it is wrong. Red X. */
    object WrongPicked : OptionFeedback()
}

@Composable
private fun feedbackFor(option: String, state: WordMatchGameState.InProgress): OptionFeedback {
    val answer = state.lastAnswer ?: return OptionFeedback.Neutral
    val correctAnswer = state.currentQuestion?.correctAnswer ?: ""
    val isThisCorrect = option.equals(correctAnswer, ignoreCase = true)
    val isThisPicked = option.equals(answer.picked, ignoreCase = true)

    return when {
        // User picked the right answer — celebrate in green.
        isThisCorrect && isThisPicked -> OptionFeedback.CorrectPicked
        // User missed the right answer — reveal it in blue.
        isThisCorrect && !isThisPicked -> OptionFeedback.RevealedCorrect
        // User picked a wrong distractor — mark it red.
        !isThisCorrect && isThisPicked && !answer.isCorrect -> OptionFeedback.WrongPicked
        // Distractor the user did not interact with — leave neutral.
        else -> OptionFeedback.Neutral
    }
}

private fun OptionFeedback.tint(container: Color): Color = when (this) {
    is OptionFeedback.CorrectPicked -> Color(0xFF34C759)    // green
    is OptionFeedback.RevealedCorrect -> Color(0xFF1E88E5)  // blue
    is OptionFeedback.WrongPicked -> Color(0xFFFF3B30)     // red
    OptionFeedback.Neutral -> container
}

@Composable
private fun OptionCard(
    text: String,
    feedback: OptionFeedback,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val baseContainer = MaterialTheme.colorScheme.surface
    val containerColor = feedback.tint(baseContainer)
        .copy(alpha = if (feedback is OptionFeedback.Neutral) 1f else 0.18f)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            // Green check — user got it right.
            AnimatedVisibility(
                visible = feedback is OptionFeedback.CorrectPicked,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = stringResource(id = R.string.game_wordmatch_correct),
                    tint = Color(0xFF34C759)
                )
            }
            // Blue check — the option that was actually correct (revealed
            // because the user picked something else).
            AnimatedVisibility(
                visible = feedback is OptionFeedback.RevealedCorrect,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = stringResource(id = R.string.game_wordmatch_correct),
                    tint = Color(0xFF1E88E5)
                )
            }
            // Red X — what the user picked and that was wrong.
            AnimatedVisibility(
                visible = feedback is OptionFeedback.WrongPicked,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Icon(
                    imageVector = Icons.Filled.Cancel,
                    contentDescription = stringResource(id = R.string.game_wordmatch_wrong_short),
                    tint = Color(0xFFFF3B30)
                )
            }
        }
    }
}
