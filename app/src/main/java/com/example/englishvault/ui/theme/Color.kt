package com.example.englishvault.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Color tokens for the "Duolingo Blue" palette.
 *
 * Replaces the default Material purple set with a brighter, friendlier
 * palette tuned for the English Vault beta UI. Each token has a 40-prefix
 * (light scheme) and 80-prefix (dark scheme) variant following Material 3
 * naming conventions.
 */

// region: Primary — Duolingo-style sky blue
internal val BluePrimary40 = Color(0xFF1CB0F6)
internal val BluePrimary80 = Color(0xFFA5E1FB)
// endregion

// region: Secondary — indigo accent for chips and secondary actions
internal val IndigoSecondary40 = Color(0xFF4F46E5)
internal val IndigoSecondary80 = Color(0xFFB4B0FA)
// endregion

// region: Tertiary — amber for XP, streaks and rewards
internal val AmberTertiary40 = Color(0xFFFFC107)
internal val AmberTertiary80 = Color(0xFFFFE082)
// endregion

// region: Status colors
internal val ErrorRed40 = Color(0xFFFF4B4B)
internal val ErrorRed80 = Color(0xFFFFB4B4)
internal val SuccessGreen40 = Color(0xFF58CC02)
// endregion

// region: Neutrals — backgrounds, surfaces, text
internal val BackgroundLight = Color(0xFFFFFFFF)
internal val BackgroundDark = Color(0xFF0F1419)
internal val SurfaceLight = Color(0xFFF7F7F7)
internal val SurfaceDark = Color(0xFF1A1F26)
internal val OnSurfaceLight = Color(0xFF1B1B1B)
internal val OnSurfaceDark = Color(0xFFE6E6E6)
internal val OutlineLight = Color(0xFFE2E2E2)
internal val OutlineDark = Color(0xFF3A3A3A)
// endregion