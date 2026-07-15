package com.example.englishvault.ui.progress

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.englishvault.R
import com.example.englishvault.ui.common.LevelUpCelebrationOverlay
import com.example.englishvault.ui.components.SectionHeader
import com.example.englishvault.ui.progress.viewmodel.CategoryProgressUi
import com.example.englishvault.ui.progress.viewmodel.ProgressViewModel
import com.example.englishvault.ui.progress.viewmodel.SkillProgressUi
import com.example.englishvault.ui.progress.viewmodel.XpProgress
import data.database.entities.Skill
import data.database.entities.UserProfileEntity

/**
 * Progress dashboard.
 *
 * Wires the screen to Room through [ProgressViewModel]. Every counter
 * and derived value (level, XP slice, daily goal, streak, per-category
 * progression) is a [kotlinx.coroutines.flow.StateFlow] so the UI
 * updates reactively when the underlying tables change.
 *
 * Phase 4.6 layout:
 *  - Greeting + header (`Your progress`)
 *  - Streak banner
 *  - Global XP card (legacy `user_profile.totalXp`)
 *  - Daily goal card
 *  - "Progress by category" — eight [CategoryProgressCard]s in the
 *    canonical [com.example.englishvault.ui.words.WordTypeFilter.TRACKED]
 *    order, each showing the per-category level, XP bar, learned bar
 *    and the hybrid-gate status.
 *
 * Phase 7.1: the greeting row is now tappable and surfaces a
 * `Settings` icon on the right. Tapping it navigates to the Settings
 * hub via [onOpenSettings] (wired by `MainScaffold`).
 */
@Composable
fun ProgressScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProgressViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val xp by viewModel.xp.collectAsState()
    val dailyXp by viewModel.dailyXp.collectAsState()
    val categories by viewModel.categoryProgress.collectAsState()
    val skills by viewModel.skills.collectAsState()
    val promotionEvent by viewModel.promotionEvent.collectAsState()

    val greetingName = profile?.name
        ?: stringResource(id = R.string.progress_default_name)

    // Wrap the whole dashboard in a Box so the level-up overlay can
    // sit on top of every other composable. The overlay covers the
    // full screen (semi-transparent backdrop + confetti + badge) so
    // it needs to ignore the parent padding.
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onOpenSettings,
                    role = Role.Button
                )
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.progress_greeting, greetingName),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(
                    id = R.string.progress_greeting_settings_cd
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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

        SectionHeader(title = stringResource(id = R.string.progress_skills_title))

        SkillsGrid(skills = skills)

        SectionHeader(title = stringResource(id = R.string.progress_your_path))

        categories.forEach { ui ->
            CategoryProgressCard(ui = ui)
        }

            Spacer(modifier = Modifier.height(24.dp))
        }

        LevelUpCelebrationOverlay(
            event = promotionEvent,
            onConsumed = { viewModel.consumePromotionEvent() }
        )
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
                Icon(
                    imageVector = Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier.size(32.dp)
                )
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
 * shows progress into the next level. Numbers above the bar are the
 * absolute slice ("320 / 500 XP to Level 5").
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

/**
 * 2-column grid of [SkillCard]s, one per language skill
 * (Listening, Speaking, Reading, Writing, Grammar). Built from
 * [Row]s chunked by 2 instead of a lazy grid because the count is
 * fixed at five and the parent column already scrolls vertically —
 * nesting a lazy grid inside a scrollable parent breaks layout in
 * Compose, and [FlowRow] does not expose child `weight` modifiers.
 *
 * 5 cards → 2 + 2 + 1: Listening & Speaking on row 1, Reading &
 * Writing on row 2, Grammar alone on row 3. The cards stay wider
 * and less cramped than a 3-column layout.
 */
@Composable
private fun SkillsGrid(skills: List<SkillProgressUi>) {
    val rows = Skill.ALL.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { rowSkills ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowSkills.forEach { skill ->
                    val ui = skills.firstOrNull { it.skill == skill }
                        ?: SkillProgressUi.empty(skill)
                    SkillCard(ui = ui, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Single skill tile. Shows the skill icon and name, the headline
 * XP number, an optional "Cycle N" chip once the user has
 * completed at least one cycle, and a cyclic linear progress bar
 * that fills up to [SkillProgressUi.cycleSize] (1000 XP by default)
 * before resetting to zero. There is no level cap — the bar is a
 * soft "chunking" indicator, not a level gate.
 */
@Composable
private fun SkillCard(ui: SkillProgressUi, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = ui.skill.icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(id = ui.skill.labelRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    id = R.string.progress_skill_xp_total_format,
                    ui.xpTotal
                ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            if (ui.cycleIndex > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(
                        id = R.string.progress_skill_cycle_format,
                        ui.cycleIndex
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { ui.progressFraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    id = R.string.progress_skill_xp_in_cycle_format,
                    ui.xpInCycle,
                    ui.cycleSize
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * One row in the "Progress by category" list. Carries:
 *  - The category name and a chip with the level (e.g. `Level 2 / 5`).
 *  - An XP bar driven by [CategoryProgressUi.xpIntoLevel] /
 *    [CategoryProgressUi.xpRequired].
 *  - A learned bar showing `learned / total` and the percentage.
 *  - A status line that switches between "Ready to level up!",
 *    "Max level reached", or a "Need X XP / Y% learned" message
 *    reflecting the hybrid gate.
 */
@Composable
private fun CategoryProgressCard(ui: CategoryProgressUi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: category name + level chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(id = ui.filter.labelRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = { /* read-only chip */ },
                    enabled = false,
                    label = {
                        Text(
                            text = stringResource(
                                id = R.string.progress_category_level_format,
                                ui.currentLevel,
                                ui.maxLevel
                            ),
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        disabledLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // XP bar
            Text(
                text = stringResource(
                    id = R.string.progress_category_xp_format,
                    ui.xpIntoLevel,
                    ui.xpRequired
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = {
                    if (ui.xpRequired <= 0) 0f
                    else (ui.xpIntoLevel.toFloat() / ui.xpRequired).coerceIn(0f, 1f)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Learned bar
            val learnedPctInt = (ui.learnedPct * 100).toInt()
            Text(
                text = stringResource(
                    id = R.string.progress_category_learned_format,
                    ui.learnedCount,
                    ui.totalCount,
                    learnedPctInt
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { ui.learnedPct.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Gate status
            val statusText: String? = when {
                ui.locked -> stringResource(id = R.string.progress_category_maxed)
                ui.canUnlockNext -> stringResource(id = R.string.progress_category_ready)
                ui.meetsXp && !ui.meetsLearnedPct -> stringResource(
                    id = R.string.progress_category_need_pct,
                    80
                )
                !ui.meetsXp && ui.meetsLearnedPct -> stringResource(
                    id = R.string.progress_category_need_xp,
                    50 - ui.xpSinceLevelUp
                )
                else -> stringResource(
                    id = R.string.progress_category_need_both,
                    50 - ui.xpSinceLevelUp,
                    80
                )
            }
            if (statusText != null) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        ui.locked -> MaterialTheme.colorScheme.primary
                        ui.canUnlockNext -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (ui.canUnlockNext || ui.locked) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}