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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.englishvault.R
import com.example.englishvault.ui.common.LevelUpCelebrationOverlay
import com.example.englishvault.ui.progress.arcade.ArcadeFonts
import com.example.englishvault.ui.progress.arcade.ArcadePalette
import com.example.englishvault.ui.progress.arcade.LocalArcadePalette
import com.example.englishvault.ui.progress.arcade.components.ArcadeCard
import com.example.englishvault.ui.progress.arcade.components.ArcadeChip
import com.example.englishvault.ui.progress.arcade.components.ArcadeIconButton
import com.example.englishvault.ui.progress.arcade.components.ArcadeLabel
import com.example.englishvault.ui.progress.arcade.components.ArcadeProgressBar
import com.example.englishvault.ui.progress.viewmodel.CategoryProgressUi
import com.example.englishvault.ui.progress.viewmodel.ProgressViewModel
import com.example.englishvault.ui.progress.viewmodel.SkillProgressUi
import com.example.englishvault.ui.progress.viewmodel.XpProgress
import data.database.entities.Skill
import data.database.entities.UserProfileEntity

/**
 * Progress dashboard, redesigned in the arcade style.
 *
 * Phase 8.x: the active [ArcadePalette] (dark or light) is read from
 * [LocalArcadePalette] and applied to the whole screen. The
 * surrounding M3 theme is flipped independently by
 * [com.example.englishvault.ui.theme.EnglishVaultTheme] so the rest
 * of the app (Words, Games, World, Settings) follows the user's
 * dark / light choice while this screen mirrors the same choice
 * in the arcade language.
 *
 * Layout (top to bottom):
 *  1. Greeting row with the player name + a 3D settings icon
 *     that opens the Settings screen.
 *  2. Big "Your progress" headline in the display font.
 *  3. Streak banner — gold-accented card.
 *  4. Level + XP card — pink-accented.
 *  5. Daily goal card — cyan-accented with a percentage chip.
 *  6. Skills grid (2 columns) — each card with a rotating accent.
 *  7. Category progress list — one card per tracked grammatical
 *     bucket, bordered with the category color.
 *
 * Overlay:
 *  - The level-up celebration overlay still uses the standard M3
 *    styling; it sits on top of everything else and ignores the
 *    arcade background.
 */
@Composable
fun ProgressScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProgressViewModel = hiltViewModel()
) {
    val palette = LocalArcadePalette.current
    val profile by viewModel.profile.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val xp by viewModel.xp.collectAsState()
    val dailyXp by viewModel.dailyXp.collectAsState()
    val categories by viewModel.categoryProgress.collectAsState()
    val skills by viewModel.skills.collectAsState()
    val promotionEvent by viewModel.promotionEvent.collectAsState()

    val greetingName = profile?.name
        ?: stringResource(id = R.string.progress_default_name)

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            GreetingRow(
                name = greetingName,
                onSettingsClick = onOpenSettings
            )

            Headline(text = stringResource(id = R.string.progress_title))

            StreakBanner(days = profile?.streakDays ?: 0)

            LevelXpCard(
                xp = xp,
                totalWords = stats.totalWords,
                learnedWords = stats.learnedWords
            )

            DailyGoalCard(
                dailyXp = dailyXp,
                dailyGoalXp = profile?.dailyGoalXp ?: UserProfileEntity.DEFAULT_DAILY_GOAL
            )

            SectionHeader(text = stringResource(id = R.string.progress_skills_title))

            SkillsGrid(skills = skills)

            SectionHeader(text = stringResource(id = R.string.progress_your_path))

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

/**
 * Greeting row: the player name (left, weight 1) and a 3D settings
 * icon (right) that opens the Settings hub. The whole row is
 * tappable so the user has a generous hit target; the icon itself
 * also reacts to taps.
 */
@Composable
private fun GreetingRow(name: String, onSettingsClick: () -> Unit) {
    val palette = LocalArcadePalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSettingsClick, role = Role.Button)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.progress_greeting, name),
            color = palette.textMain,
            fontFamily = ArcadeFonts.Display,
            fontWeight = ArcadeFonts.DisplayWeight,
            fontSize = 18.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.weight(1f)
        )
        ArcadeIconButton(
            onClick = onSettingsClick,
            color = palette.secondary,
            shadow = palette.shadowOf(palette.secondary)
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(
                    id = R.string.progress_greeting_settings_cd
                ),
                tint = palette.ink
            )
        }
    }
}

/** Section header rendered in the display font, all caps. */
@Composable
private fun Headline(text: String) {
    val palette = LocalArcadePalette.current
    Text(
        text = text.uppercase(),
        color = palette.textMain,
        fontFamily = ArcadeFonts.Display,
        fontWeight = ArcadeFonts.DisplayWeight,
        fontSize = 22.sp,
        letterSpacing = 2.sp
    )
}

/** In-card section header — same look as [Headline] but smaller. */
@Composable
private fun SectionHeader(text: String) {
    val palette = LocalArcadePalette.current
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = text.uppercase(),
        color = palette.textMain,
        fontFamily = ArcadeFonts.Display,
        fontWeight = ArcadeFonts.DisplayWeight,
        fontSize = 16.sp,
        letterSpacing = 2.sp
    )
}

/**
 * Streak banner: gold-accented card with a fire icon inside a
 * gold circle. The day count is the headline; the supporting line
 * keeps a friendly tone.
 */
@Composable
private fun StreakBanner(days: Int) {
    val palette = LocalArcadePalette.current
    ArcadeCard(accent = palette.highlight) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(palette.highlight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = palette.ink,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.size(16.dp))
            Column {
                Text(
                    text = stringResource(id = R.string.progress_streak_days, days),
                    color = palette.textMain,
                    fontFamily = ArcadeFonts.Display,
                    fontWeight = ArcadeFonts.DisplayWeight,
                    fontSize = 22.sp
                )
                ArcadeLabel(text = "Keep it going!")
            }
        }
    }
}

/**
 * Level + XP card. The level number is the headline; below it the
 * XP bar fills with the primary color and a small caption names
 * the level-up threshold. The bottom line shows total words
 * learned.
 */
@Composable
private fun LevelXpCard(
    xp: XpProgress,
    totalWords: Int,
    learnedWords: Int
) {
    val palette = LocalArcadePalette.current
    ArcadeCard(accent = palette.primary) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(id = R.string.progress_xp_label, xp.level),
                color = palette.textMain,
                fontFamily = ArcadeFonts.Display,
                fontWeight = ArcadeFonts.DisplayWeight,
                fontSize = 28.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(
                    id = R.string.progress_xp_to_next,
                    xp.xpIntoLevel,
                    xp.xpRequired,
                    xp.nextLevel
                ),
                color = palette.textDim,
                fontFamily = ArcadeFonts.Pixel,
                fontWeight = ArcadeFonts.PixelWeight,
                fontSize = 10.sp,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            ArcadeProgressBar(
                fraction = if (xp.xpRequired <= 0) 0f
                else (xp.xpIntoLevel.toFloat() / xp.xpRequired).coerceIn(0f, 1f),
                color = palette.primary
            )
            Spacer(modifier = Modifier.height(10.dp))
            ArcadeLabel(text = "$learnedWords / $totalWords words learned")
        }
    }
}

/**
 * Daily goal card. Cyan-accented. The percentage is rendered as a
 * big pixel-font number on a solid cyan chip so the player sees
 * the progress at a glance even when scrolling fast.
 */
@Composable
private fun DailyGoalCard(dailyXp: Int, dailyGoalXp: Int) {
    val palette = LocalArcadePalette.current
    ArcadeCard(accent = palette.secondary) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val fraction = if (dailyGoalXp <= 0) 0f
                else (dailyXp.toFloat() / dailyGoalXp).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(palette.secondary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${(fraction * 100).toInt()}%",
                    color = palette.ink,
                    fontFamily = ArcadeFonts.Display,
                    fontWeight = ArcadeFonts.DisplayWeight,
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.progress_daily_goal),
                    color = palette.textMain,
                    fontFamily = ArcadeFonts.Display,
                    fontWeight = ArcadeFonts.DisplayWeight,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                ArcadeLabel(
                    text = stringResource(
                        id = R.string.progress_daily_goal_value,
                        dailyXp,
                        dailyGoalXp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                ArcadeProgressBar(fraction = fraction, color = palette.secondary)
            }
        }
    }
}

/**
 * 2-column grid of [SkillTile]s. The accent color rotates through
 * the four primary/secondary/highlight/success slots so each card
 * is visually distinct on the dark background.
 */
@Composable
private fun SkillsGrid(skills: List<SkillProgressUi>) {
    val palette = LocalArcadePalette.current
    val rows = Skill.ALL.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { rowSkills ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowSkills.forEachIndexed { columnIndex, skill ->
                    val absoluteIndex = rows.indexOf(rowSkills) * 2 + columnIndex
                    val ui = skills.firstOrNull { it.skill == skill }
                        ?: SkillProgressUi.empty(skill)
                    SkillTile(
                        ui = ui,
                        accent = palette.skillAccent(absoluteIndex),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * One skill tile. Shows the skill icon, the XP headline, a cycle
 * chip when the user has completed at least one cycle, and the
 * fill bar.
 */
@Composable
private fun SkillTile(
    ui: SkillProgressUi,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val palette = LocalArcadePalette.current
    ArcadeCard(modifier = modifier, accent = accent) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ui.skill.icon,
                        contentDescription = null,
                        tint = palette.ink,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(id = ui.skill.labelRes),
                    color = palette.textMain,
                    fontFamily = ArcadeFonts.Display,
                    fontWeight = ArcadeFonts.DisplayWeight,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    id = R.string.progress_skill_xp_total_format,
                    ui.xpTotal
                ),
                color = palette.textMain,
                fontFamily = ArcadeFonts.Display,
                fontWeight = ArcadeFonts.DisplayWeight,
                fontSize = 22.sp
            )
            if (ui.cycleIndex > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                ArcadeLabel(
                    text = stringResource(
                        id = R.string.progress_skill_cycle_format,
                        ui.cycleIndex
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            ArcadeProgressBar(
                fraction = ui.progressFraction.coerceIn(0f, 1f),
                color = accent
            )
            Spacer(modifier = Modifier.height(4.dp))
            ArcadeLabel(
                text = stringResource(
                    id = R.string.progress_skill_xp_in_cycle_format,
                    ui.xpInCycle,
                    ui.cycleSize
                )
            )
        }
    }
}

/**
 * One row in the "Progress by category" list. Header: category
 * name + an `ArcadeChip` with `Level X / Y`. Body: two
 * `ArcadeProgressBar`s — XP (accent color) and learned (success
 * green). Footer: a small status line that reflects the hybrid
 * gate.
 */
@Composable
private fun CategoryProgressCard(ui: CategoryProgressUi) {
    val palette = LocalArcadePalette.current
    val accent = palette.categoryColor(ui.filter)
    ArcadeCard(accent = accent) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(id = ui.filter.labelRes),
                    color = palette.textMain,
                    fontFamily = ArcadeFonts.Display,
                    fontWeight = ArcadeFonts.DisplayWeight,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                ArcadeChip(
                    text = stringResource(
                        id = R.string.progress_category_level_format,
                        ui.currentLevel,
                        ui.maxLevel
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(
                    id = R.string.progress_category_xp_format,
                    ui.xpIntoLevel,
                    ui.xpRequired
                ),
                color = palette.textDim,
                fontFamily = ArcadeFonts.Pixel,
                fontWeight = ArcadeFonts.PixelWeight,
                fontSize = 9.sp,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            ArcadeProgressBar(
                fraction = if (ui.xpRequired <= 0) 0f
                else (ui.xpIntoLevel.toFloat() / ui.xpRequired).coerceIn(0f, 1f),
                color = accent
            )

            Spacer(modifier = Modifier.height(10.dp))

            val learnedPctInt = (ui.learnedPct * 100).toInt()
            Text(
                text = stringResource(
                    id = R.string.progress_category_learned_format,
                    ui.learnedCount,
                    ui.totalCount,
                    learnedPctInt
                ),
                color = palette.textDim,
                fontFamily = ArcadeFonts.Pixel,
                fontWeight = ArcadeFonts.PixelWeight,
                fontSize = 9.sp,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            ArcadeProgressBar(
                fraction = ui.learnedPct.coerceIn(0f, 1f),
                color = palette.success
            )

            Spacer(modifier = Modifier.height(8.dp))

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
                    color = if (ui.canUnlockNext || ui.locked) {
                        palette.success
                    } else {
                        palette.textDim
                    },
                    fontFamily = ArcadeFonts.Pixel,
                    fontWeight = if (ui.canUnlockNext || ui.locked) {
                        ArcadeFonts.PixelWeight
                    } else {
                        ArcadeFonts.BodyWeight
                    },
                    fontSize = 9.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
