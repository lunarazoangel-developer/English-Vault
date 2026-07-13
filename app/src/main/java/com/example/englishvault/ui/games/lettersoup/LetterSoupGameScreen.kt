package com.example.englishvault.ui.games.lettersoup

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Translate
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.englishvault.R
import com.example.englishvault.ui.games.lettersoup.model.HintMode
import com.example.englishvault.ui.games.lettersoup.model.LetterSoupCell
import com.example.englishvault.ui.games.lettersoup.model.LetterSoupGameState
import com.example.englishvault.ui.games.lettersoup.model.LetterSoupGameState.Companion.HINT_TIMEOUT_MS
import com.example.englishvault.ui.games.lettersoup.model.LetterSoupGameState.Companion.INITIAL_ENGLISH_HINTS
import com.example.englishvault.ui.games.lettersoup.model.LetterSoupGameState.Companion.INITIAL_LOCATION_HINTS
import com.example.englishvault.ui.games.lettersoup.model.LetterSoupGameState.Companion.WORLD_GAME_TIME_MS
import com.example.englishvault.ui.games.lettersoup.model.LetterSoupWord
import com.example.englishvault.ui.games.lettersoup.util.LetterPalette
import com.example.englishvault.ui.games.lettersoup.viewmodel.LetterSoupViewModel
import kotlinx.coroutines.delay

/**
 * Master switch for the dev toggle button. Flip to `false` to hide
 * the icon and force every run into [HintMode.NORMAL]; flip to
 * `true` to bring the toggle back for manual QA / play-testing.
 */
private const val DEV_MODE_TOGGLE_ENABLED: Boolean = true

/**
 * Active gameplay screen for the Letter Soup mini-game.
 *
 * Drives the [LetterSoupViewModel] from a single
 * `LaunchedEffect(level)` so the run resets whenever the screen
 * enters composition. Renders one of three bodies depending on
 * [LetterSoupGameState]:
 *  - [LetterSoupGameState.Loading] — branded loading panel.
 *  - [LetterSoupGameState.InProgress] — top HUD with the move budget,
 *    words fixed and the two hint buttons; the always-visible
 *    translations list; the auto-scaling 8×8 / 10×10 grid; and a
 *    brief "fixed word" / "failed swap" flash. World-mode runs
 *    additionally show a 5-minute countdown bar and `X / Y`
 *    counters on the hint buttons.
 *  - [LetterSoupGameState.Finished] — the end-of-run panel rendered
 *    in place via [LetterSoupEndContent].
 */
@Composable
fun LetterSoupGameScreen(
    level: Int,
    onBack: () -> Unit,
    onExitToGames: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LetterSoupViewModel = hiltViewModel()
) {
    val state by viewModel.gameState.collectAsState()
    val hintMode by viewModel.currentHintMode.collectAsState()

    LaunchedEffect(level) {
        viewModel.startGame(level)
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
                    contentDescription = stringResource(id = R.string.game_lettersoup_back),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = stringResource(id = R.string.game_lettersoup_header, level),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (DEV_MODE_TOGGLE_ENABLED) {
                ModeToggleButton(
                    mode = hintMode,
                    onClick = viewModel::toggleHintMode
                )
            }
        }

        when (val s = state) {
            is LetterSoupGameState.Loading -> BrandedLoadingPanel()
            is LetterSoupGameState.InProgress -> InProgressBody(
                state = s,
                onTap = viewModel::tapCell,
                onAnimationAck = viewModel::acknowledgeAnimation,
                onLocationHint = viewModel::revealLocationHint,
                onEnglishHint = viewModel::revealEnglishHint,
                onLocationHintAck = viewModel::acknowledgeLocationHint,
                onEnglishHintAck = viewModel::acknowledgeEnglishHint
            )
            is LetterSoupGameState.Finished -> LetterSoupEndContent(
                level = s.level,
                won = s.won,
                wordsFixed = s.wordsFixed,
                wordsToWin = s.wordsToWin,
                onPlayAgain = { viewModel.startGame(level) },
                onExit = onExitToGames
            )
        }
    }
}

@Composable
private fun BrandedLoadingPanel() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(id = R.string.game_lettersoup_loading_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.game_lettersoup_loading_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun InProgressBody(
    state: LetterSoupGameState.InProgress,
    onTap: (Int, Int) -> Unit,
    onAnimationAck: () -> Unit,
    onLocationHint: () -> Unit,
    onEnglishHint: () -> Unit,
    onLocationHintAck: () -> Unit,
    onEnglishHintAck: () -> Unit
) {
    LaunchedEffect(state.lastSwapFailedCells, state.lastFixedWord) {
        if (state.lastSwapFailedCells.isNotEmpty() || state.lastFixedWord != null) {
            delay(600)
            onAnimationAck()
        }
    }
    LaunchedEffect(state.isLocationHintRevealed) {
        if (state.isLocationHintRevealed) {
            delay(HINT_TIMEOUT_MS)
            onLocationHintAck()
        }
    }
    LaunchedEffect(state.isEnglishHintRevealed) {
        if (state.isEnglishHintRevealed) {
            delay(HINT_TIMEOUT_MS)
            onEnglishHintAck()
        }
    }

    val activeWrong = state.board.firstActiveWithWrong()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (state.mode == HintMode.WORLD) {
            WorldModeTimerBar(timeRemainingMs = state.timeRemainingMs)
        }

        HudRow(
            state = state,
            onLocationHint = onLocationHint,
            onEnglishHint = onEnglishHint
        )

        TranslationsList(
            placements = state.board.placements,
            isEnglishHintRevealed = state.isEnglishHintRevealed,
            activeWrongWord = activeWrong
        )

        BoardGrid(
            state = state,
            onTap = onTap
        )

        Spacer(modifier = Modifier.height(4.dp))

        HintCard()

        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * Dev-only icon that swaps [HintMode.NORMAL] ↔ [HintMode.WORLD].
 * Hidden when [DEV_MODE_TOGGLE_ENABLED] is `false`; remove the call
 * site in [LetterSoupGameScreen] to delete the feature entirely.
 */
@Composable
private fun ModeToggleButton(mode: HintMode, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = if (mode == HintMode.WORLD) {
                Icons.Filled.Public
            } else {
                Icons.Filled.SportsEsports
            },
            contentDescription = stringResource(
                id = R.string.game_lettersoup_world_dev_toggle_cd
            ),
            tint = if (mode == HintMode.WORLD) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onBackground
            }
        )
    }
}

/**
 * Countdown bar shown above the HUD row in [HintMode.WORLD]. The
 * bar fills left-to-right and turns red during the final 30 seconds
 * so the player has a clear visual cue when time is running out.
 */
@Composable
private fun WorldModeTimerBar(timeRemainingMs: Long) {
    val fraction = if (WORLD_GAME_TIME_MS <= 0L) 0f
        else timeRemainingMs.toFloat() / WORLD_GAME_TIME_MS.toFloat()
    val urgent = timeRemainingMs in 1..30_000L
    val totalSeconds = (timeRemainingMs / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = if (urgent) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Text(
            text = stringResource(
                id = R.string.game_lettersoup_world_timer_format,
                minutes.toInt(),
                seconds.toInt()
            ),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (urgent) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HudRow(
    state: LetterSoupGameState.InProgress,
    onLocationHint: () -> Unit,
    onEnglishHint: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HudChip(
            label = stringResource(id = R.string.game_lettersoup_moves_format, state.movesLeft),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f)
        )
        HudChip(
            label = stringResource(
                id = R.string.game_lettersoup_words_format,
                state.wordsFixed,
                state.wordsToWin
            ),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f)
        )
        HintButton(
            icon = Icons.Filled.Lightbulb,
            contentDescription = stringResource(id = R.string.game_lettersoup_hint_location),
            remaining = state.locationHintsRemaining,
            max = INITIAL_LOCATION_HINTS,
            showCounter = state.mode == HintMode.WORLD,
            enabled = state.mode == HintMode.NORMAL || state.locationHintsRemaining > 0,
            onClick = onLocationHint
        )
        HintButton(
            icon = Icons.Filled.Translate,
            contentDescription = stringResource(id = R.string.game_lettersoup_hint_english),
            remaining = state.englishHintsRemaining,
            max = INITIAL_ENGLISH_HINTS,
            showCounter = state.mode == HintMode.WORLD,
            enabled = state.mode == HintMode.NORMAL || state.englishHintsRemaining > 0,
            onClick = onEnglishHint
        )
    }
}

/**
 * Compact hint button. In [HintMode.WORLD] it carries a `remaining /
 * max` counter so the player knows how many uses they have left; in
 * [HintMode.NORMAL] it shows just the icon, matching the original
 * Phase 7.3 design.
 */
@Composable
private fun HintButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    remaining: Int,
    max: Int,
    showCounter: Boolean,
    enabled: Boolean,
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
        if (showCounter) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Icon(imageVector = icon, contentDescription = contentDescription)
                Text(
                    text = stringResource(
                        id = R.string.game_lettersoup_world_hints_format,
                        remaining,
                        max
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Icon(imageVector = icon, contentDescription = contentDescription)
        }
    }
}

@Composable
private fun HudChip(
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

/**
 * Always-visible Spanish translation chips above the board.
 *
 * Each placement contributes one chip. The chip shows the Spanish
 * translation by default; when the player taps the English hint
 * button, the chip belonging to the active placement temporarily
 * swaps its text for the English word. Fixed words stay on the list
 * with a ✓ badge and a muted background.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TranslationsList(
    placements: List<LetterSoupWord>,
    isEnglishHintRevealed: Boolean,
    activeWrongWord: LetterSoupWord?
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        placements.forEach { word ->
            val showEnglish = isEnglishHintRevealed && word === activeWrongWord
            TranslationChip(
                translation = word.translation ?: "—",
                englishWord = word.original,
                showEnglish = showEnglish,
                fixed = word.fixed
            )
        }
    }
}

@Composable
private fun TranslationChip(
    translation: String,
    englishWord: String,
    showEnglish: Boolean,
    fixed: Boolean
) {
    val containerColor = when {
        showEnglish -> MaterialTheme.colorScheme.tertiary
        fixed -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = when {
        showEnglish -> MaterialTheme.colorScheme.onTertiary
        fixed -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    Card(
        shape = RoundedCornerShape(50),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (showEnglish) englishWord else translation,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
            if (fixed) {
                Spacer(modifier = Modifier.size(6.dp))
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = stringResource(id = R.string.game_lettersoup_fixed_word_cd),
                    tint = Color(0xFF34C759),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun BoardGrid(
    state: LetterSoupGameState.InProgress,
    onTap: (Int, Int) -> Unit
) {
    val boardSize = state.board.boardSize
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            for (row in 0 until boardSize) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    for (col in 0 until boardSize) {
                        LetterCell(
                            letter = state.board[row, col],
                            role = state.board.roleAt(row, col),
                            isSelected = state.selectedCell == row to col,
                            isFailedSwap = (row to col) in state.lastSwapFailedCells,
                            isJustFixed = state.lastFixedWord?.cells()?.contains(row to col) == true,
                            showWrongMarker = state.isLocationHintRevealed &&
                                state.board.roleAt(row, col) == LetterSoupCell.WordWrong,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clickable { onTap(row, col) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LetterCell(
    letter: Char,
    role: LetterSoupCell,
    isSelected: Boolean,
    isFailedSwap: Boolean,
    isJustFixed: Boolean,
    showWrongMarker: Boolean,
    modifier: Modifier = Modifier
) {
    val background = LetterPalette.backgroundFor(letter)
    val isWordCell = role != LetterSoupCell.Soup

    val transition = rememberInfiniteTransition(label = "wrong-letter-pulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wrong-letter-alpha"
    )

    val borderColor = when {
        isFailedSwap -> Color(0xFFFF3B30)
        isJustFixed -> Color(0xFF34C759)
        isSelected -> MaterialTheme.colorScheme.primary
        isWordCell -> Color.White
        else -> Color.Transparent
    }
    val borderWidth = when {
        isFailedSwap || isJustFixed -> 3.dp
        isSelected -> 3.dp
        isWordCell -> 2.dp
        else -> 0.dp
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .border(width = borderWidth, color = borderColor, shape = RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = LetterPalette.letterForeground
        )
        if (showWrongMarker) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(14.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFFF3B30).copy(alpha = pulseAlpha)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Cancel,
                    contentDescription = stringResource(id = R.string.game_lettersoup_wrong_letter_cd),
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun HintCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            text = stringResource(id = R.string.game_lettersoup_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(12.dp)
        )
    }
}