package com.example.englishvault.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Material 3 color schemes for English Vault.
 *
 * Uses the Duolingo-inspired blue palette defined in [Color.kt]. Dynamic
 * color is disabled by default so the brand identity stays consistent
 * across devices; pass `dynamicColor = true` to opt-in to Material You.
 */

// region: Color schemes
private val DarkColorScheme = darkColorScheme(
    primary = BluePrimary80,
    onPrimary = BackgroundDark,
    primaryContainer = BluePrimary40,
    onPrimaryContainer = BackgroundDark,
    secondary = IndigoSecondary80,
    onSecondary = BackgroundDark,
    tertiary = AmberTertiary80,
    onTertiary = BackgroundDark,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = OnSurfaceDark,
    outline = OutlineDark,
    error = ErrorRed80,
    onError = BackgroundDark
)

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary40,
    onPrimary = BackgroundLight,
    primaryContainer = BluePrimary80,
    onPrimaryContainer = OnSurfaceLight,
    secondary = IndigoSecondary40,
    onSecondary = BackgroundLight,
    tertiary = AmberTertiary40,
    onTertiary = OnSurfaceLight,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = OnSurfaceLight,
    outline = OutlineLight,
    error = ErrorRed40,
    onError = BackgroundLight
)
// endregion

/**
 * Root theme composable. Wrap your screen content with this so typography,
 * colors and shapes are consistent across the app.
 *
 * @param darkTheme Whether to apply the dark color scheme.
 * @param dynamicColor When true, uses Material You on Android 12+.
 *   Defaults to false to preserve the English Vault brand colors.
 * @param content The composable hierarchy that will receive the theme.
 */
@Composable
fun EnglishVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}