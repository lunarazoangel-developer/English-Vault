package com.example.englishvault.ui.games.listening

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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.englishvault.R
import com.example.englishvault.ui.games.common.GameMode
import com.example.englishvault.ui.games.listening.model.ListeningGameState
import com.example.englishvault.ui.games.listening.model.ListeningGameState.Companion.QUESTION_TIME_MS
import com.example.englishvault.ui.games.listening.viewmodel.ListeningViewModel
import com.example.englishvault.ui.progress.arcade.ArcadeFonts
import com.example.englishvault.ui.progress.arcade.LocalArcadePalette
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
 * Active gameplay screen for the Listening mini-game.
 *
 * Receives the chosen [level] as a navigation argument and seeds
 * the VM on first composition. Renders one of three bodies
 * depending on the [ListeningGameState]:
 *  - [ListeningGameState.Loading] / [ListeningGameState.Empty] —
 *    a full-screen branded loading panel with the app's pink
 *    gradient so the user never sees a plain "Loading…" flash.
 *  - [ListeningGameState.InProgress] — the giant 🔊 Listen button
 *    at the top, four option cards below it, and the WORLD-mode
 *    HUD (lives, countdown, re-listens, 50/50) when applicable.
 *    The first time the question becomes visible the VM also
 *    auto-plays the TTS so the player does not need to tap
 *    manually on every question.
 *  - [ListeningGameState.Finished] — the results panel rendered
 *    in place (see [ListeningEndContent]); "Play again" resets
 *    the VM and "Back to games" hands control back to the parent.
 *
 * Renders end-to-end against the arcade palette (reads
 * [LocalArcadePalette] at the screen root) so the screen flips
 * between dark and light with the rest of the UI.
 */
@Composable
fun ListeningGameScreen(
    level: Int,
    onBack: () -> Unit,
    onExitToGames: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ListeningViewModel = hiltViewModel()
) {
    val state by viewModel.gameState.collectAsState()
    val mode by viewModel.currentMode.collectAsState()
    val palette = LocalArcadePalette.current

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
        if (s is ListeningGameState.InProgress && s.lastAnswer != null) {
            delay(1500)
            viewModel.acknowledgeAnswer()
        }
    }

    // Auto-play the TTS as soon as a fresh question appears so the
    // player does not need to tap Listen every time.
    LaunchedEffect(state) {
        val s = state
        if (s is ListeningGameState.InProgress && s.lastAnswer == null) {
            viewModel.speakCurrentWord()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
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
                    contentDescription = stringResource(id = R.string.game_listening_back),
                    tint = palette.textMain
                )
            }
            Text(
                text = stringResource(id = R.string.game_listening_header, level),
                color = palette.textMain,
                fontFamily = ArcadeFonts.Display,
                fontWeight = ArcadeFonts.DisplayWeight,
                fontSize = 16.sp,
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
            is ListeningGameState.Loading -> BrandedLoadingPanel()
            is ListeningGameState.Empty -> BrandedEmptyPanel()
            is ListeningGameState.InProgress -> InProgressState(
                state = s,
                onPicked = viewModel::submitAnswer,
                onListen = viewModel::speakCurrentWord,
                onUseHelp = viewModel::useHelpItem
            )
            is ListeningGameState.Finished -> ListeningEndContent(
                state = s,
                onPlayAgain = { viewModel.startGame(level) },
                onExit = onExitToGames
            )
        }
    }
}

/**
 * Dev-only icon that swaps [GameMode.NORMAL] ↔ [GameMode.WORLD].
 * Hidden when [DEV_MODE_TOGGLE_ENABLED] is `false`; remove the call
 * site in [ListeningGameScreen] to delete the feature entirely.
 */
@Composable
private fun ModeToggleButton(mode: GameMode, onClick: () -> Unit) {
    val palette = LocalArcadePalette.current
    IconButton(onClick = onClick) {
        Icon(
            imageVector = if (mode == GameMode.WORLD) {
                Icons.Filled.Public
            } else {
                Icons.Filled.SportsEsports
            },
            contentDescription = stringResource(
                id = R.string.game_listening_world_dev_toggle_cd
            ),
            tint = if (mode == GameMode.WORLD) palette.primary else palette.textMain
        )
    }
}

@Composable
private fun BrandedLoadingPanel() {
    val palette = LocalArcadePalette.current
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
                text = stringResource(id = R.string.game_listening_loading_title),
                color = palette.ink,
                fontFamily = ArcadeFonts.Display,
                fontWeight = ArcadeFonts.DisplayWeight,
                fontSize = 26.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.game_listening_loading_subtitle),
                color = palette.ink.copy(alpha = 0.78f),
                fontFamily = ArcadeFonts.Pixel,
                fontWeight = ArcadeFonts.PixelWeight,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            androidx.compose.material3.CircularProgressIndicator(
                color = palette.ink,
                strokeWidth = 4.dp,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
private fun BrandedEmptyPanel() {
    val palette = LocalArcadePalette.current
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
                text = stringResource(id = R.string.game_listening_loading_title),
                color = palette.ink,
                fontFamily = ArcadeFonts.Display,
                fontWeight = ArcadeFonts.DisplayWeight,
                fontSize = 26.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.game_listening_no_words),
                color = palette.ink.copy(alpha = 0.78f),
                fontFamily = ArcadeFonts.Pixel,
                fontWeight = ArcadeFonts.PixelWeight,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Vertical brand gradient used for the loading and empty panels.
 * Reads the live [LocalArcadePalette] so the gradient swaps between
 * the dark and light variants with the rest of the UI.
 */
@Composable
private fun BrandedGradient(): Brush {
    val palette = LocalArcadePalette.current
    return Brush.verticalGradient(
        colors = listOf(
            palette.primary,
            palette.primary.copy(alpha = 0.72f)
        )
    )
}

@Composable
private fun InProgressState(
    state: ListeningGameState.InProgress,
    onPicked: (String) -> Unit,
    onListen: () -> Unit,
    onUseHelp: () -> Unit
) {
    val question = state.currentQuestion ?: return
    val palette = LocalArcadePalette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(
                id = R.string.game_listening_progress,
                state.currentIndex + 1,
                state.totalQuestions
            ),
            color = palette.textDim,
            fontFamily = ArcadeFonts.Pixel,
            fontWeight = ArcadeFonts.PixelWeight,
            fontSize = 12.sp
        )

        if (state.mode == GameMode.WORLD) {
            WorldModeHud(
                state = state,
                onUseHelp = onUseHelp
            )
        }

        // Big "Listen" button + Spanish hint.
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
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.game_listening_listen_prompt),
                    color = palette.textMain,
                    fontFamily = ArcadeFonts.Display,
                    fontWeight = ArcadeFonts.DisplayWeight,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                FilledIconButton(
                    onClick = onListen,
                    modifier = Modifier.size(96.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = palette.primary,
                        contentColor = palette.ink
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.VolumeUp,
                        contentDescription = stringResource(
                            id = R.string.game_listening_listen_button
                        ),
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.game_listening_listen_button),
                    color = palette.textMain,
                    fontFamily = ArcadeFonts.Pixel,
                    fontWeight = ArcadeFonts.PixelWeight,
                    fontSize = 12.sp
                )
                if (state.timedOut) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(id = R.string.game_listening_world_timeout),
                        color = palette.error,
                        fontFamily = ArcadeFonts.Pixel,
                        fontWeight = ArcadeFonts.PixelWeight,
                        fontSize = 12.sp
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
 * World-mode HUD: lives chip, countdown bar, re-listen counter,
 * 50/50 button. Sits between the progress label and the listen
 * button so the player always sees both the timer and the lives
 * count without scrolling.
 */
@Composable
private fun WorldModeHud(
    state: ListeningGameState.InProgress,
    onUseHelp: () -> Unit
) {
    val palette = LocalArcadePalette.current
    val fraction = if (QUESTION_TIME_MS <= 0L) 0f
        else state.timeRemainingMs.toFloat() / QUESTION_TIME_MS.toFloat()
    val urgent = state.timeRemainingMs in 1..3_000L

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = if (urgent) palette.error else palette.primary,
            trackColor = palette.surfaceDark
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LivesChip(lives = state.lives, modifier = Modifier.weight(1f))
            ItemButton(
                label = "${state.relistenItems}",
                icon = Icons.Filled.Replay,
                contentDescription = stringResource(id = R.string.game_listening_world_relisten_cd),
                onClick = {}
            )
            ItemButton(
                label = "${state.helpItems}",
                icon = Icons.Filled.Lightbulb,
                enabled = state.helpItems > 0 && state.lastAnswer == null,
                contentDescription = stringResource(id = R.string.game_listening_world_help_cd),
                onClick = onUseHelp
            )
        }
    }
}

@Composable
private fun LivesChip(lives: Int, modifier: Modifier = Modifier) {
    val palette = LocalArcadePalette.current
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = palette.error.copy(alpha = 0.18f)
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
                    tint = if (filled) palette.error
                        else palette.error.copy(alpha = 0.35f),
                    modifier = Modifier.size(20.dp)
                )
                if (index < 2) Spacer(modifier = Modifier.size(2.dp))
            }
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = stringResource(id = R.string.game_listening_world_lives_format, lives),
                color = palette.error,
                fontFamily = ArcadeFonts.Pixel,
                fontWeight = ArcadeFonts.PixelWeight,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ItemButton(
    label: String,
    icon: ImageVector,
    enabled: Boolean = true,
    contentDescription: String,
    onClick: () -> Unit
) {
    val palette = LocalArcadePalette.current
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = palette.highlight.copy(alpha = 0.22f),
            contentColor = palette.textMain
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
                color = palette.textMain,
                fontFamily = ArcadeFonts.Pixel,
                fontWeight = ArcadeFonts.PixelWeight,
                fontSize = 12.sp
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
 * answers wrong: a green check marks the option they picked when
 * correct; a cyan check highlights the correct answer they missed;
 * a red X marks the option they picked when wrong. Distractors that
 * were neither picked nor correct stay neutral.
 */
private sealed class OptionFeedback {
    object Neutral : OptionFeedback()
    object CorrectPicked : OptionFeedback()
    object RevealedCorrect : OptionFeedback()
    object WrongPicked : OptionFeedback()
}

@Composable
private fun feedbackFor(
    option: String,
    state: ListeningGameState.InProgress
): OptionFeedback {
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

private fun OptionFeedback.tint(palette: com.example.englishvault.ui.progress.arcade.ArcadePalette): Color = when (this) {
    is OptionFeedback.CorrectPicked -> palette.success
    is OptionFeedback.RevealedCorrect -> palette.secondary
    is OptionFeedback.WrongPicked -> palette.error
    OptionFeedback.Neutral -> palette.surface
}

@Composable
private fun OptionCard(
    text: String,
    feedback: OptionFeedback,
    enabled: Boolean,
    eliminated: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalArcadePalette.current
    val containerColor = feedback.tint(palette)
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
                color = if (eliminated) {
                    palette.textMain.copy(alpha = 0.35f)
                } else {
                    palette.textMain
                },
                fontFamily = ArcadeFonts.Display,
                fontWeight = ArcadeFonts.DisplayWeight,
                fontSize = 16.sp,
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
                    contentDescription = stringResource(id = R.string.game_listening_correct),
                    tint = palette.success
                )
            }
            AnimatedVisibility(
                visible = feedback is OptionFeedback.RevealedCorrect,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = stringResource(id = R.string.game_listening_correct),
                    tint = palette.secondary
                )
            }
            AnimatedVisibility(
                visible = feedback is OptionFeedback.WrongPicked,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Icon(
                    imageVector = Icons.Filled.Cancel,
                    contentDescription = stringResource(id = R.string.game_listening_wrong_short),
                    tint = palette.error
                )
            }
        }
    }
}