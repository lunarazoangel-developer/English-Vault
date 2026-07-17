package com.example.englishvault.ui.games.wordmatchverbs

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
import com.example.englishvault.ui.games.wordmatchverbs.model.WordMatchError
import com.example.englishvault.ui.games.wordmatchverbs.model.WordMatchGameState
import com.example.englishvault.ui.progress.arcade.ArcadeFonts
import com.example.englishvault.ui.progress.arcade.LocalArcadePalette
import com.example.englishvault.ui.progress.arcade.components.ArcadeButton
import com.example.englishvault.ui.words.WordTypeFilter
import data.database.entities.Skill

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
 * Renders against the arcade palette: primary "Play again" uses
 * [ArcadeButton]; secondary "Back to games" is the outlined variant
 * declared at the bottom of this file; score, XP and error cards
 * tint off the primary / highlight / error accents so they stay
 * legible in both themes.
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
            text = stringResource(id = R.string.game_wordmatch_game_over),
            color = palette.textMain,
            fontFamily = ArcadeFonts.Display,
            fontWeight = ArcadeFonts.DisplayWeight,
            fontSize = 24.sp
        )

        ScoreCard(correct = state.correctCount, total = state.totalQuestions)

        XpSummaryCard(
            correctXpByCategory = state.correctXpByCategory
        )

        if (state.errors.isNotEmpty()) {
            Text(
                text = stringResource(id = R.string.game_wordmatch_errors_title),
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
                text = stringResource(id = R.string.game_wordmatch_perfect),
                color = palette.success,
                fontFamily = ArcadeFonts.Pixel,
                fontWeight = ArcadeFonts.PixelWeight,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        ArcadeButton(
            text = stringResource(id = R.string.game_wordmatch_play_again),
            onClick = onPlayAgain,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedArcadeButton(
            text = stringResource(id = R.string.game_wordmatch_back_games),
            onClick = onExit,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Score card. Container tints off `palette.primary` so it pops
 * without competing with the headline above; the progress bar
 * carries the success accent so a perfect run reads as celebratory
 * at a glance.
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
                    id = R.string.game_wordmatch_score_format,
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
private fun ErrorRow(error: WordMatchError) {
    val palette = LocalArcadePalette.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = palette.error.copy(alpha = 0.18f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = error.question.baseWord,
                    color = palette.error,
                    fontFamily = ArcadeFonts.Display,
                    fontWeight = ArcadeFonts.DisplayWeight,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(id = error.question.askType.promptResId),
                    color = palette.error.copy(alpha = 0.78f),
                    fontFamily = ArcadeFonts.Pixel,
                    fontWeight = ArcadeFonts.PixelWeight,
                    fontSize = 10.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(
                    id = R.string.game_wordmatch_error_user_format,
                    error.userPicked
                ),
                color = palette.textMain,
                fontFamily = ArcadeFonts.Pixel,
                fontWeight = ArcadeFonts.PixelWeight,
                fontSize = 11.sp
            )
            Text(
                text = stringResource(
                    id = R.string.game_wordmatch_error_correct_format,
                    error.question.correctAnswer
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
 * Card summarising how much XP the run earned, broken down by
 * grammatical category and by skill.
 *
 * - **Category** rows list every bucket the run credited (each
 *   bucket key is a `WordTypeFilter.name` literal). Each row shows
 *   the localised category label and the XP earned.
 * - **Skill** row is a single entry — the run's total XP credited
 *   to [Skill.READING] (Word Match Verbs is a reading activity).
 * - When the run earned zero XP, only an empty-state message is
 *   shown.
 */
@Composable
private fun XpSummaryCard(correctXpByCategory: Map<String, Int>) {
    val palette = LocalArcadePalette.current
    val totalXp = correctXpByCategory.values.sum()
    if (totalXp <= 0) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = palette.surfaceDark,
                contentColor = palette.textDim
            )
        ) {
            Text(
                text = stringResource(id = R.string.game_end_xp_no_xp),
                color = palette.textDim,
                fontFamily = ArcadeFonts.Pixel,
                fontWeight = ArcadeFonts.PixelWeight,
                fontSize = 11.sp,
                modifier = Modifier.padding(20.dp)
            )
        }
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = palette.highlight.copy(alpha = 0.18f),
            contentColor = palette.textMain
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(id = R.string.game_end_xp_summary_title),
                color = palette.textMain,
                fontFamily = ArcadeFonts.Display,
                fontWeight = ArcadeFonts.DisplayWeight,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.game_end_xp_category_label),
                color = palette.textDim,
                fontFamily = ArcadeFonts.Pixel,
                fontWeight = ArcadeFonts.PixelWeight,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            correctXpByCategory.entries
                .filter { it.value > 0 }
                .sortedByDescending { it.value }
                .forEach { (key, xp) ->
                    XpRow(
                        label = categoryLabel(key),
                        xp = xp
                    )
                }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.game_end_xp_skill_label),
                color = palette.textDim,
                fontFamily = ArcadeFonts.Pixel,
                fontWeight = ArcadeFonts.PixelWeight,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            XpRow(
                label = Skill.READING.labelRes,
                xp = totalXp
            )
        }
    }
}

/**
 * Resolves a [WordTypeFilter] from its stable [name] key, returning
 * the matching entry's string-resource id (`@StringRes Int`) when
 * found, or `null` otherwise. The composable caller is responsible
 * for turning the id into a `String` via [stringResource] so this
 * helper stays free of the `@Composable` annotation and is safe to
 * call from non-Composable scope.
 */
private fun categoryLabel(key: String): Int? {
    val match = WordTypeFilter.entries.firstOrNull { it.name == key }
    return match?.labelRes
}

/**
 * Single "Label: +X XP" row inside [XpSummaryCard]. Uses the
 * [R.string.game_end_xp_row_format] format string. When [label] is a
 * known `@StringRes`, the resource is resolved via [stringResource];
 * otherwise the raw string is used so an unknown bucket key does not
 * crash the end screen.
 */
@Composable
private fun XpRow(label: Int?, xp: Int) {
    val palette = LocalArcadePalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (label != null) {
                stringResource(
                    id = R.string.game_end_xp_row_format,
                    stringResource(id = label),
                    xp
                )
            } else {
                stringResource(
                    id = R.string.game_end_xp_row_format,
                    "unknown",
                    xp
                )
            },
            color = palette.textMain,
            fontFamily = ArcadeFonts.Pixel,
            fontWeight = ArcadeFonts.PixelWeight,
            fontSize = 11.sp
        )
    }
}

/**
 * Secondary arcade button (matching the helpers in
 * [com.example.englishvault.ui.games.lettersoup.LetterSoupEndScreen]
 * and
 * [com.example.englishvault.ui.games.listening.ListeningEndContent]):
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