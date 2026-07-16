package com.example.englishvault.ui.games.wordmatchverbs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.englishvault.R
import com.example.englishvault.ui.games.wordmatchverbs.model.GameMode
import com.example.englishvault.ui.games.wordmatchverbs.model.WordMatchGameState
import com.example.englishvault.ui.games.wordmatchverbs.model.WordMatchGameState.Companion.QUESTION_TIME_MS
import com.example.englishvault.ui.games.wordmatchverbs.viewmodel.WordMatchVerbsViewModel
import kotlinx.coroutines.delay

/**
 * Master switch for the dev toggle button. Flip to `false` to hide
 * the icon and force every run into [GameMode.NORMAL]; flip to
 * `true` to bring the toggle back for manual QA / play-testing.
 *
 * Designed so the world-mode wiring can stay merged before the
 * full world-mode UX (inventory, lives persistence, etc.) ships —
 * flipping this constant hides the toggle without deleting code.
 */
private const val DEV_MODE_TOGGLE_ENABLED: Boolean = true

/**
 * Active gameplay screen for Word Match Verbs.
 *
 * Receives the chosen [level] as a navigation argument and seeds
 * the VM on first composition. Renders one of three bodies
 * depending on the [WordMatchGameState]:
 *  - [WordMatchGameState.Loading] / [WordMatchGameState.Empty] —
 *    a full-screen branded loading panel with the app's blue
 *    gradient so the user never sees a plain "Loading…" flash.
 *  - [WordMatchGameState.InProgress] — the verb + question prompt
 *    and four option cards, with a transient ✗ / ✓ overlay that
 *    auto-advances after a short delay. World-mode runs additionally
 *    show lives, a countdown timer, and the help / boost item
 *    buttons.
 *  - [WordMatchGameState.Finished] — the results panel rendered in
 *    place (see [WordMatchVerbsEndContent]); "Play again" resets the
 *    VM and "Back to games" hands control back to the parent.
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
    val mode by viewModel.currentMode.collectAsState()

    // Seed the VM exactly once per level. Re-runs of the same level
    // (via "Play again") call `viewModel.startGame(level)` directly
    // from the end content, which transitions the state through
    // Loading → InProgress on its own.
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
            if (DEV_MODE_TOGGLE_ENABLED) {
                ModeToggleButton(
                    mode = mode,
                    onClick = viewModel::toggleMode
                )
            }
        }

        when (val s = state) {
            is WordMatchGameState.Loading -> BrandedLoadingPanel()
            is WordMatchGameState.Empty -> BrandedEmptyPanel()
            is WordMatchGameState.InProgress -> InProgressState(
                state = s,
                onPicked = viewModel::submitAnswer,
                onUseHelp = viewModel::useHelpItem,
                onUseTimeBoost = viewModel::useTimeBoostItem
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
 * Dev-only icon that swaps [GameMode.NORMAL] ↔ [GameMode.WORLD].
 * Hidden when [DEV_MODE_TOGGLE_ENABLED] is `false`; remove the
 * call site in [WordMatchVerbsGameScreen] to delete the feature
 * entirely.
 */
@Composable
private fun ModeToggleButton(mode: GameMode, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = if (mode == GameMode.WORLD) {
                Icons.Filled.Public
            } else {
                Icons.Filled.SportsEsports
            },
            contentDescription = stringResource(
                id = R.string.game_wordmatch_world_dev_toggle_cd
            ),
            tint = if (mode == GameMode.WORLD) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onBackground
            }
        )
    }
}

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
            androidx.compose.material3.CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 4.dp,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

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

@Composable
private fun BrandedGradient(): Brush = Brush.verticalGradient(
    colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
    )
)

@Composable
private fun InProgressState(
    state: WordMatchGameState.InProgress,
    onPicked: (String) -> Unit,
    onUseHelp: () -> Unit,
    onUseTimeBoost: () -> Unit
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

        if (state.mode == GameMode.WORLD) {
            WorldModeHud(
                state = state,
                onUseHelp = onUseHelp,
                onUseTimeBoost = onUseTimeBoost
            )
        }

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
                if (question.translation.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = question.translation,
                        style = MaterialTheme.typography.titleMedium,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                            .copy(alpha = 0.75f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = question.askType.promptResId),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (state.timedOut) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(id = R.string.game_wordmatch_world_timeout),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFFF3B30),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Four option cards.
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(question.options) { option ->
                OptionCard(
                    text = option,
                    feedback = feedbackFor(option, state),
                    enabled = state.lastAnswer == null && option !in state.eliminatedOptions,
                    eliminated = option in state.eliminatedOptions,
                    onClick = { onPicked(option) }
                )
            }
        }
    }
}

/**
 * World-mode HUD: lives chip, countdown bar, 50/50 button, +5s
 * button. Sits between the progress label and the verb card so
 * the player always sees both the timer and the lives count
 * without scrolling.
 */
@Composable
private fun WorldModeHud(
    state: WordMatchGameState.InProgress,
    onUseHelp: () -> Unit,
    onUseTimeBoost: () -> Unit
) {
    val fraction = if (QUESTION_TIME_MS <= 0L) 0f
        else state.timeRemainingMs.toFloat() / QUESTION_TIME_MS.toFloat()
    val urgent = state.timeRemainingMs in 1..3_000L

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = if (urgent) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LivesChip(lives = state.lives, modifier = Modifier.weight(1f))
            ItemButton(
                label = "${state.helpItems}",
                icon = Icons.Filled.Lightbulb,
                enabled = state.helpItems > 0 && state.lastAnswer == null,
                contentDescription = stringResource(id = R.string.game_wordmatch_world_help_cd),
                onClick = onUseHelp
            )
            ItemButton(
                label = "${state.timeBoostItems}",
                icon = Icons.Filled.Timer,
                enabled = state.timeBoostItems > 0 &&
                    state.lastAnswer == null &&
                    state.timeRemainingMs > 0L,
                contentDescription = stringResource(id = R.string.game_wordmatch_world_time_boost_cd),
                onClick = onUseTimeBoost
            )
        }
    }
}

@Composable
private fun LivesChip(lives: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                val filled = index < lives
                Icon(
                    imageVector = if (filled) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    tint = if (filled) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
                if (index < 2) Spacer(modifier = Modifier.size(2.dp))
            }
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = stringResource(id = R.string.game_wordmatch_world_lives_format, lives),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun ItemButton(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit
) {
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = contentDescription)
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Visual feedback for a single option card. Returned as a sealed
 * type so the [OptionCard] composable can pick the right tint and
 * icon without doing the comparison itself.
 *
 * Four states keep the colour and icon unambiguous when the player
 * answers wrong: a red X marks the option they picked; a blue check
 * highlights the correct answer they missed so the learner can see
 * at a glance which one was right. Distractors that were neither
 * picked nor correct stay neutral.
 */
private sealed class OptionFeedback {
    object Neutral : OptionFeedback()
    object CorrectPicked : OptionFeedback()
    object RevealedCorrect : OptionFeedback()
    object WrongPicked : OptionFeedback()
}

@Composable
private fun feedbackFor(option: String, state: WordMatchGameState.InProgress): OptionFeedback {
    val answer = state.lastAnswer ?: return OptionFeedback.Neutral
    val correctAnswer = state.currentQuestion?.correctAnswer ?: ""
    val isThisCorrect = option.equals(correctAnswer, ignoreCase = true)
    val isThisPicked = option.equals(answer.picked, ignoreCase = true)

    return when {
        isThisCorrect && isThisPicked -> OptionFeedback.CorrectPicked
        isThisCorrect && !isThisPicked -> OptionFeedback.RevealedCorrect
        !isThisCorrect && isThisPicked && !answer.isCorrect -> OptionFeedback.WrongPicked
        else -> OptionFeedback.Neutral
    }
}

private fun OptionFeedback.tint(container: Color): Color = when (this) {
    is OptionFeedback.CorrectPicked -> Color(0xFF34C759)
    is OptionFeedback.RevealedCorrect -> Color(0xFF1E88E5)
    is OptionFeedback.WrongPicked -> Color(0xFFFF3B30)
    OptionFeedback.Neutral -> container
}

@Composable
private fun OptionCard(
    text: String,
    feedback: OptionFeedback,
    enabled: Boolean,
    eliminated: Boolean,
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
                color = if (eliminated) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
            )
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