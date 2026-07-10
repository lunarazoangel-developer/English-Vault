package com.example.englishvault.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography scale for English Vault.
 *
 * Currently inherits the Material 3 defaults; screens rely on
 * [Typography.bodyLarge], `titleMedium`, `labelSmall`, etc.
 * Future phases can introduce a custom font family (e.g. Nunito or
 * Inter) to better match the Duolingo-style aesthetic.
 */

// region: Default Material 3 typography
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)
// endregion