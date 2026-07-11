package com.example.englishvault.ui.progress

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.englishvault.R
import com.example.englishvault.ui.components.SectionHeader
import com.example.englishvault.ui.progress.viewmodel.ProgressViewModel
import com.example.englishvault.ui.progress.viewmodel.UnitProgress
import com.example.englishvault.ui.progress.viewmodel.XpProgress
import data.database.entities.UserProfileEntity

/**
 * Progress dashboard.
 *
 * Wires the screen to Room through [ProgressViewModel]. Every counter
 * and derived value (level, XP slice, daily goal, streak, units by
 * difficulty) is a [kotlinx.coroutines.flow.StateFlow] so the UI
 * updates reactively when the underlying tables change.
 *
 * Layout:
 *  - Greeting + header (`Your progress`)
 *  - Streak banner (amber accent)
 *  - XP card with the current level number and a progress bar to the
 *    next level (visual only — the actual XP-reward mechanic is not
 *    wired yet).
 *  - Daily goal card with circular progress
 *  - "Your path" list of three buckets (EASY / MEDIUM / HARD) with
 *    learned / total counts and linear progress bars.
 */
@Composable
fun ProgressScreen(
    modifier: Modifier = Modifier,
    viewModel: ProgressViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val xp by viewModel.xp.collectAsState()
    val dailyXp by viewModel.dailyXp.collectAsState()
    val units by viewModel.units.collectAsState()

    val greetingName = profile?.name
        ?: stringResource(id = R.string.progress_default_name)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.progress_greeting, greetingName),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(id = R.string.progress_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        StreakBanner(days = profile?.streakDays ?: 0)

        XpCard(
            xp = xp,
            totalWords = stats.totalWords,
            learnedWords = stats.learnedWords
        )

        DailyGoalCard(
            dailyXp = dailyXp,
            dailyGoalXp = profile?.dailyGoalXp ?: UserProfileEntity.DEFAULT_DAILY_GOAL
        )

        SectionHeader(title = stringResource(id = R.string.progress_your_path))

        if (units.isEmpty()) {
            EmptyPathPlaceholder()
        } else {
            units.forEach { unit ->
                UnitProgressRow(unit = unit)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun StreakBanner(days: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "\uD83D\uDD25", style = MaterialTheme.typography.headlineMedium)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = stringResource(id = R.string.progress_streak_days, days),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Keep it going!",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * XP card with the level number as the headline and a linear bar that
 * shows progress into the next level. The numbers above the bar are
 * the absolute slice ("320 / 500 XP to Level 5").
 */
@Composable
private fun XpCard(
    xp: XpProgress,
    totalWords: Int,
    learnedWords: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(id = R.string.progress_xp_label, xp.level),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    id = R.string.progress_xp_to_next,
                    xp.xpIntoLevel,
                    xp.xpRequired,
                    xp.nextLevel
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = {
                    if (xp.xpRequired <= 0) 0f
                    else (xp.xpIntoLevel.toFloat() / xp.xpRequired).coerceIn(0f, 1f)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(12.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "$learnedWords / $totalWords words learned",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
private fun DailyGoalCard(dailyXp: Int, dailyGoalXp: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val progressFraction = if (dailyGoalXp <= 0) 0f
                else (dailyXp.toFloat() / dailyGoalXp).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${(progressFraction * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = stringResource(id = R.string.progress_daily_goal),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(
                        id = R.string.progress_daily_goal_value,
                        dailyXp,
                        dailyGoalXp
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun UnitProgressRow(unit: UnitProgress) {
    val progress = if (unit.total <= 0) 0f
        else (unit.learned.toFloat() / unit.total).coerceIn(0f, 1f)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = unit.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(
                        id = R.string.progress_unit_format,
                        unit.learned,
                        unit.total
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(10.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun EmptyPathPlaceholder() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Text(
            text = "Add words to start tracking your path.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}