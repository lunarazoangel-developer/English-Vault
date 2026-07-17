package com.example.englishvault.ui.games.wordmatchverbs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Text
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.englishvault.R
import com.example.englishvault.ui.games.wordmatchverbs.model.GameMode
import com.example.englishvault.ui.games.wordmatchverbs.model.WordMatchGameState
import com.example.englishvault.ui.games.wordmatchverbs.model.WordMatchGameState.Companion.QUESTION_TIME_MS
import com.example.englishvault.ui.games.wordmatchverbs.model.WordMatchQuestion
import com.example.englishvault.ui.games.wordmatchverbs.viewmodel.WordMatchVerbsViewModel
import com.example.englishvault.ui.progress.arcade.ArcadeFonts
import com.example.englishvault.ui.progress.arcade.LocalArcadePalette
import kotlinx.coroutines.delay

/**
 * Master switch for the dev toggle button. Flip to `false` to hide
 * the icon and force every run into [GameMode.NORMAL]; flip to
 * `true` to bring the toggle back for manual QA / play-testing.
 */
private const val DEV_MODE_TOGGLE_ENABLED: Boolean = true

/**
 * Active gameplay screen for Word Match Verbs.
 *
 * Phase 7.x redesign:
 *  - Header shows a yellow "Level N" chip + a row of progress
 *    squares that colour themselves as the player answers each
 *    question. The numeric counter ("3 / 10") sits on the left of
 *    the squares.
 *  - The verb card and the four option cards are rendered as a
 *    single `AnimatedContent` block that slides the current question
 *    out to the left and the next one in from the right when
 *    `state.currentIndex` advances.
 *  - World mode HUD: timer bar (pink / red when urgent), lives chip
 *    with bigger hearts on a neutral background, plus a cyan
 *    50/50 button and a green +5s button so the two help items
 *    read as distinct actions.
 *
 * Renders end-to-end against the arcade palette (reads
 * [LocalArcadePalette] at the screen root).
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
    val palette = LocalArcadePalette.current

    LaunchedEffect(level) {
        viewModel.startGame(level)
    }

    // Auto-advance after the player answers. The 1.5s delay gives the
    // feedback overlay enough time to be readable before the slide
    // animation kicks in.
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
            .background(palette.background)
    ) {
        // Header: [← back] [yellow Level chip] ... [mode toggle]
        GameHeader(
            level = level,
            mode = mode,
            onBack = onBack,
            onToggleMode = viewModel::toggleMode
        )

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
 * Header row with three slots:
 *  - Left: back arrow + the yellow "Level N" chip.
 *  - Centre: empty `Spacer(weight=1f)` pushes the dev toggle right.
 *  - Far right: dev mode toggle.
 *
 * The in-run progress (counter + squares) lives in the [InProgressState]
 * body so it can flow below the header when the run has many
 * questions — the squares wrap onto multiple lines via
 * [FlowRow] without colliding with the chrome.
 */
@Composable
private fun GameHeader(
    level: Int,
    mode: GameMode,
    onBack: () -> Unit,
    onToggleMode: () -> Unit
) {
    val palette = LocalArcadePalette.current
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
                tint = palette.textMain
            )
        }
        // Yellow level chip — the "Level N" text is painted over the
        // highlighted background so the dictionary progression number
        // reads as its own badge.
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(palette.highlight)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = stringResource(id = R.string.game_wordmatch_header, level),
                color = palette.ink,
                fontFamily = ArcadeFonts.Display,
                fontWeight = ArcadeFonts.DisplayWeight,
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        if (DEV_MODE_TOGGLE_ENABLED) {
            ModeToggleButton(mode = mode, onClick = onToggleMode)
        }
    }
}

/**
 * Dev-only icon that swaps [GameMode.NORMAL] ↔ [GameMode.WORLD].
 */
@Composable
private fun ModeToggleButton(mode: GameMode, onClick: () -> Unit) {
    val palette = LocalArcadePalette.current
    IconButton(onClick = onClick) {
        Icon(
            imageVector = if (mode == GameMode.WORLD) Icons.Filled.Public
            else Icons.Filled.SportsEsports,
            contentDescription = stringResource(
                id = R.string.game_wordmatch_world_dev_toggle_cd
            ),
            tint = if (mode == GameMode.WORLD) palette.primary else palette.textMain
        )
    }
}

/**
 * Flow of small 12×12 dp squares that surface in-run progress.
 *
 * Uses [FlowRow] so the squares wrap onto multiple lines when there
 * are too many to fit horizontally — important for runs with 20+
 * questions where a single row would overflow the screen. At
 * `total ≤ ~12` (the typical short run) everything stays on one
 * line; at `total = 20` it splits into two rows of ten; at `total
 * = 30` into three rows of ten, etc.
 *
 * Each index below [currentIndex] renders as a coloured cell:
 *  - green when the previous question was answered correctly (or no
 *    feedback is available yet, e.g. on the first question)
 *  - red when the last answered question was wrong
 * The [currentIndex] cell is the active pink square with a 2 dp
 * border; everything past it stays as the pending outline.
 *
 * Colours animate via [animateColorAsState] with a cubic-bezier ease
 * so the row reads as "filling up" rather than snapping.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ProgressSquares(
    total: Int,
    currentIndex: Int,
    lastAnswerWasCorrect: Boolean?
) {
    val palette = LocalArcadePalette.current
    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier
            .height(14.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        maxItemsInEachRow = Int.MAX_VALUE
    ) {
        for (i in 0 until total) {
            val target = when {
                i < currentIndex ->
                    if (lastAnswerWasCorrect != false) palette.success
                    else palette.error
                i == currentIndex -> palette.primary
                else -> palette.surfaceDark
            }
            val animated by animateColorAsState(
                targetValue = target,
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                label = "progress-square-$i"
            )
            val isActive = i == currentIndex
            val isAnswered = i < currentIndex
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(animated)
                    .then(
                        if (isActive) Modifier.border(
                            width = 2.dp,
                            color = palette.ink.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(3.dp)
                        )
                        else if (!isAnswered) Modifier.border(
                            width = 1.dp,
                            color = palette.border,
                            shape = RoundedCornerShape(3.dp)
                        )
                        else Modifier
                    )
            )
        }
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
                text = stringResource(id = R.string.game_wordmatch_loading_title),
                color = palette.ink,
                fontFamily = ArcadeFonts.Display,
                fontWeight = ArcadeFonts.DisplayWeight,
                fontSize = 26.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.game_wordmatch_loading_subtitle),
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
                text = stringResource(id = R.string.game_wordmatch_loading_title),
                color = palette.ink,
                fontFamily = ArcadeFonts.Display,
                fontWeight = ArcadeFonts.DisplayWeight,
                fontSize = 26.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.game_wordmatch_no_words),
                color = palette.ink.copy(alpha = 0.78f),
                fontFamily = ArcadeFonts.Pixel,
                fontWeight = ArcadeFonts.PixelWeight,
                fontSize = 12.sp
            )
        }
    }
}

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

/**
 * Body of the in-progress run. Renders the WORLD-mode HUD (when
 * applicable) and then the question + options block. The question
 * block is wrapped in [AnimatedContent] keyed by
 * `state.currentIndex` so each advance triggers a horizontal
 * slide-out → slide-in transition between cards.
 */
@Composable
private fun InProgressState(
    state: WordMatchGameState.InProgress,
    onPicked: (String) -> Unit,
    onUseHelp: () -> Unit,
    onUseTimeBoost: () -> Unit
) {
    val palette = LocalArcadePalette.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // In-run progress (counter + squares). Sits above the world-mode
        // HUD so the player can see "you're on question 3 of 20" at a
        // glance. FlowRow lets the squares wrap when the run has many
        // questions so the row never collides with the header.
        ProgressRow(
            total = state.totalQuestions,
            currentIndex = state.currentIndex,
            lastAnswerWasCorrect = state.lastAnswer?.isCorrect
        )

        if (state.mode == GameMode.WORLD) {
            WorldModeHud(
                state = state,
                onUseHelp = onUseHelp,
                onUseTimeBoost = onUseTimeBoost
            )
        }

        // Card stack: verb + 4 options animated as one block.
        AnimatedContent(
            targetState = state.currentIndex,
            transitionSpec = {
                val forward = targetState > initialState
                val direction = if (forward) 1 else -1
                (slideInHorizontally(
                    animationSpec = tween(280, easing = FastOutSlowInEasing),
                    initialOffsetX = { fullWidth -> fullWidth * direction }
                ) + fadeIn(animationSpec = tween(220))) togetherWith
                    (slideOutHorizontally(
                        animationSpec = tween(280, easing = FastOutSlowInEasing),
                        targetOffsetX = { fullWidth -> -fullWidth * direction }
                    ) + fadeOut(animationSpec = tween(180)))
            },
            label = "question-card-flip"
        ) { idx ->
            val q = state.questions.getOrNull(idx) ?: return@AnimatedContent
            QuestionCardStack(
                question = q,
                state = state,
                onPicked = onPicked,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * In-run progress row: numeric counter + row of squares that wrap
 * onto multiple lines via [FlowRow] when there are too many to fit
 * on one. Lives in the body (not the header) so a 20+ question run
 * doesn't push the level chip off-screen.
 */
@Composable
private fun ProgressRow(
    total: Int,
    currentIndex: Int,
    lastAnswerWasCorrect: Boolean?
) {
    val palette = LocalArcadePalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(
                id = R.string.game_wordmatch_progress,
                currentIndex + 1,
                total
            ),
            color = palette.textMain,
            fontFamily = ArcadeFonts.Pixel,
            fontWeight = ArcadeFonts.PixelWeight,
            fontSize = 13.sp
        )
        ProgressSquares(
            total = total,
            currentIndex = currentIndex,
            lastAnswerWasCorrect = lastAnswerWasCorrect
        )
    }
}

/**
 * Single question rendered as a "card stack": verb / translation /
 * prompt at the top, four option cards below. Sits inside the
 * [AnimatedContent] block so the whole stack slides on transitions.
 */
@Composable
private fun QuestionCardStack(
    question: WordMatchQuestion,
    state: WordMatchGameState.InProgress,
    onPicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalArcadePalette.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Verb card.
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
                    text = question.baseWord,
                    color = palette.textMain,
                    fontFamily = ArcadeFonts.Display,
                    fontWeight = ArcadeFonts.DisplayWeight,
                    fontSize = 32.sp
                )
                if (question.translation.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = question.translation,
                        color = palette.textMain.copy(alpha = 0.78f),
                        fontFamily = ArcadeFonts.Pixel,
                        fontWeight = ArcadeFonts.PixelWeight,
                        fontStyle = FontStyle.Italic,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = question.askType.promptResId),
                    color = palette.textMain,
                    fontFamily = ArcadeFonts.Display,
                    fontWeight = ArcadeFonts.DisplayWeight,
                    fontSize = 16.sp
                )
                if (state.timedOut) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(id = R.string.game_wordmatch_world_timeout),
                        color = palette.error,
                        fontFamily = ArcadeFonts.Pixel,
                        fontWeight = ArcadeFonts.PixelWeight,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Four option cards (no LazyColumn — the slide transition
        // above swaps the entire block atomically).
        question.options.forEach { option ->
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

/**
 * World-mode HUD: timer bar + lives chip + 50/50 button + +5s
 * button. Sits at the top of the in-progress body so the player
 * always sees the timer and lives without scrolling.
 */
@Composable
private fun WorldModeHud(
    state: WordMatchGameState.InProgress,
    onUseHelp: () -> Unit,
    onUseTimeBoost: () -> Unit
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
            // 50/50 — cyan accent ("smart move").
            HelpItemButton(
                label = "${state.helpItems}",
                icon = Icons.Filled.Lightbulb,
                containerColor = palette.secondary.copy(alpha = 0.22f),
                enabled = state.helpItems > 0 && state.lastAnswer == null,
                contentDescription = stringResource(id = R.string.game_wordmatch_world_help_cd),
                onClick = onUseHelp
            )
            // +5 seconds — green accent ("boost").
            HelpItemButton(
                label = "${state.timeBoostItems}",
                icon = Icons.Filled.Timer,
                containerColor = palette.success.copy(alpha = 0.22f),
                enabled = state.timeBoostItems > 0 &&
                    state.lastAnswer == null &&
                    state.timeRemainingMs > 0L,
                contentDescription = stringResource(id = R.string.game_wordmatch_world_time_boost_cd),
                onClick = onUseTimeBoost
            )
        }
    }
}

/**
 * Lives chip with three hearts. Hearts are enlarged to 24 dp and
 * the chip background drops the red tint so the icons themselves
 * carry the colour.
 */
@Composable
private fun LivesChip(lives: Int, modifier: Modifier = Modifier) {
    val palette = LocalArcadePalette.current
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surfaceDark)
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
                        else palette.error.copy(alpha = 0.20f),
                    modifier = Modifier.size(24.dp)
                )
                if (index < 2) Spacer(modifier = Modifier.size(2.dp))
            }
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = stringResource(id = R.string.game_wordmatch_world_lives_format, lives),
                color = palette.error,
                fontFamily = ArcadeFonts.Pixel,
                fontWeight = ArcadeFonts.PixelWeight,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Single help-item button (50/50 or +5s). The caller picks the
 * accent colour so the two items read as different actions on the
 * WORLD-mode HUD without the user having to memorise icons.
 */
@Composable
private fun HelpItemButton(
    label: String,
    icon: ImageVector,
    containerColor: Color,
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit
) {
    val palette = LocalArcadePalette.current
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = containerColor,
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
                    contentDescription = stringResource(id = R.string.game_wordmatch_correct),
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
                    contentDescription = stringResource(id = R.string.game_wordmatch_correct),
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
                    contentDescription = stringResource(id = R.string.game_wordmatch_wrong_short),
                    tint = palette.error
                )
            }
        }
    }
}