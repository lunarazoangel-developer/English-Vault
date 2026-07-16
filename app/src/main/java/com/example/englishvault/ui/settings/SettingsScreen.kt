package com.example.englishvault.ui.settings

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.englishvault.R
import com.example.englishvault.ui.progress.arcade.ArcadeFonts
import com.example.englishvault.ui.progress.arcade.LocalArcadePalette
import com.example.englishvault.ui.progress.arcade.components.ArcadeCard
import com.example.englishvault.ui.progress.arcade.components.ArcadeLabel
import com.example.englishvault.ui.settings.viewmodel.SettingsViewModel
import data.database.entities.UserProfileEntity

/**
 * Settings hub, redesigned in the arcade style.
 *
 * Three sections, all backed by the same [SettingsViewModel] so the
 * sliders and the theme picker stay in sync with `user_profile`:
 *  - **Profile**: a single tappable row that navigates to the
 *    "Change name" sub-screen, showing the current display name as a
 *    secondary line. Card accent: gold (premium / identity).
 *  - **Appearance**: two pill buttons that toggle the persisted
 *    `user_profile.themeMode` between dark and light. The change
 *    is applied live at the root of the Compose tree. Card accent:
 *    pink (visual identity).
 *  - **Sound**: two [Slider]s for music and effects volume in
 *    `[0.0, 1.0]`. The music slider ships as a placeholder
 *    because the audio engine is not wired yet. Card accent: cyan
 *    (controls).
 *
 * The screen reads the active [com.example.englishvault.ui.progress.arcade.ArcadePalette]
 * from [LocalArcadePalette] so it adapts to the user's light /
 * dark choice at the same time as the Progress screen and the
 * rest of the arcade-aware UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onEditName: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val palette = LocalArcadePalette.current
    val profile by viewModel.profile.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    Scaffold(
        modifier = modifier,
        containerColor = palette.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.settings_title),
                        color = palette.textMain,
                        fontFamily = ArcadeFonts.Display,
                        fontWeight = ArcadeFonts.DisplayWeight,
                        fontSize = 20.sp,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = stringResource(
                                id = R.string.settings_back
                            ),
                            tint = palette.textMain
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = palette.surface,
                    titleContentColor = palette.textMain,
                    navigationIconContentColor = palette.textMain
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProfileSection(
                currentName = profile?.name
                    ?: UserProfileEntity.DEFAULT_NAME,
                onClick = onEditName
            )

            AppearanceSection(
                themeMode = themeMode,
                onThemeModeChange = viewModel::setThemeMode
            )

            SoundSection(
                musicVolume = profile?.musicVolume
                    ?: UserProfileEntity.DEFAULT_VOLUME,
                effectsVolume = profile?.effectsVolume
                    ?: UserProfileEntity.DEFAULT_VOLUME,
                onMusicVolumeChange = viewModel::updateMusicVolume,
                onEffectsVolumeChange = viewModel::updateEffectsVolume
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProfileSection(
    currentName: String,
    onClick: () -> Unit
) {
    val palette = LocalArcadePalette.current
    ArcadeSectionTitle(text = stringResource(id = R.string.settings_section_profile))

    ArcadeCard(accent = palette.highlight) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsIconBadge(
                icon = Icons.Filled.Person,
                container = palette.highlight,
                content = palette.ink
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.settings_change_name),
                    color = palette.textMain,
                    fontFamily = ArcadeFonts.Display,
                    fontWeight = ArcadeFonts.DisplayWeight,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                ArcadeLabel(text = currentName)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = palette.textDim
            )
        }
    }
}

@Composable
private fun AppearanceSection(
    themeMode: String,
    onThemeModeChange: (String) -> Unit
) {
    val palette = LocalArcadePalette.current
    ArcadeSectionTitle(text = stringResource(id = R.string.settings_section_appearance))

    ArcadeCard(accent = palette.primary) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsIconBadge(
                    icon = Icons.Filled.DarkMode,
                    container = palette.primary,
                    content = palette.ink
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.settings_theme_label),
                        color = palette.textMain,
                        fontFamily = ArcadeFonts.Display,
                        fontWeight = ArcadeFonts.DisplayWeight,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    ArcadeLabel(text = stringResource(id = R.string.settings_theme_hint))
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ThemePillButton(
                    label = stringResource(id = R.string.settings_theme_dark),
                    selected = themeMode == UserProfileEntity.THEME_MODE_DARK,
                    onClick = {
                        onThemeModeChange(UserProfileEntity.THEME_MODE_DARK)
                    },
                    modifier = Modifier.weight(1f)
                )
                ThemePillButton(
                    label = stringResource(id = R.string.settings_theme_light),
                    selected = themeMode == UserProfileEntity.THEME_MODE_LIGHT,
                    onClick = {
                        onThemeModeChange(UserProfileEntity.THEME_MODE_LIGHT)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Pill button used by the theme picker. Flat (no shadow), solid
 * fill when selected, surface-dark when not. Stays in the same
 * arcade family as [com.example.englishvault.ui.progress.arcade.components.ArcadeChip]
 * but uses display font and a slightly bigger pill because it is
 * the primary affordance of the Appearance section.
 *
 * Renders as two stacked `Box`es so the active variant gets a
 * subtle inner border that reads as a "depressed" chip — the inner
 * box fills with the background color, leaving a 2 dp rim of the
 * outer container around it. The inactive variant hides the inner
 * rim and stays flat.
 */
@Composable
private fun ThemePillButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalArcadePalette.current
    val container = if (selected) palette.primary else palette.surfaceDark
    val content = if (selected) palette.ink else palette.textDim
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (selected) palette.primary else palette.surface)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = content,
                fontFamily = ArcadeFonts.Display,
                fontWeight = ArcadeFonts.DisplayWeight,
                fontSize = 14.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun SoundSection(
    musicVolume: Float,
    effectsVolume: Float,
    onMusicVolumeChange: (Float) -> Unit,
    onEffectsVolumeChange: (Float) -> Unit
) {
    val palette = LocalArcadePalette.current
    ArcadeSectionTitle(text = stringResource(id = R.string.settings_section_sound))

    ArcadeCard(accent = palette.secondary) {
        Column {
            VolumeSlider(
                icon = Icons.Filled.MusicNote,
                label = stringResource(id = R.string.settings_sound_music),
                hint = stringResource(id = R.string.settings_sound_music_hint),
                value = musicVolume,
                onValueChange = onMusicVolumeChange,
                iconContainer = palette.secondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            VolumeSlider(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                label = stringResource(id = R.string.settings_sound_effects),
                hint = null,
                value = effectsVolume,
                onValueChange = onEffectsVolumeChange,
                iconContainer = palette.secondary
            )
        }
    }
}

@Composable
private fun VolumeSlider(
    icon: ImageVector,
    label: String,
    hint: String?,
    value: Float,
    onValueChange: (Float) -> Unit,
    iconContainer: Color
) {
    val palette = LocalArcadePalette.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SettingsIconBadge(
                icon = icon,
                container = iconContainer,
                content = palette.ink
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = palette.textMain,
                    fontFamily = ArcadeFonts.Display,
                    fontWeight = ArcadeFonts.DisplayWeight,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                ArcadeLabel(
                    text = stringResource(
                        id = R.string.settings_sound_volume_format,
                        (value * 100).toInt()
                    )
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = palette.ink,
                activeTrackColor = palette.primary,
                inactiveTrackColor = palette.border
            )
        )
        if (hint != null) {
            Spacer(modifier = Modifier.height(4.dp))
            ArcadeLabel(text = hint)
        }
    }
}

/**
 * Circle badge holding the section's icon. Solid color background
 * matching the section's accent, dark ink icon, 40 dp.
 */
@Composable
private fun SettingsIconBadge(
    icon: ImageVector,
    container: Color,
    content: Color
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(container),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * Section header for the arcade settings — same look as
 * `ProgressScreen.SectionHeader` so the two screens feel like the
 * same family.
 */
@Composable
private fun ArcadeSectionTitle(text: String) {
    val palette = LocalArcadePalette.current
    Text(
        text = text.uppercase(),
        color = palette.textMain,
        fontFamily = ArcadeFonts.Display,
        fontWeight = ArcadeFonts.DisplayWeight,
        fontSize = 14.sp,
        letterSpacing = 2.sp
    )
}
