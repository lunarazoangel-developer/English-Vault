package com.example.englishvault.ui.progress.arcade.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.englishvault.ui.progress.arcade.ArcadeFonts
import com.example.englishvault.ui.progress.arcade.LocalArcadePalette

/**
 * Pill chip with the arcade pixel-label treatment.
 *
 * Two states:
 *  - **Inactive** (default): `palette.surfaceDark` background, dim
 *    text.
 *  - **Active**: `palette.secondary` background, dark text.
 *
 * Optional [onClick] turns the chip into a tappable toggle. When
 * [onClick] is `null` the chip is rendered read-only.
 */
@Composable
fun ArcadeChip(
    text: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val palette = LocalArcadePalette.current
    val container = if (active) palette.secondary else palette.surfaceDark
    val content = if (active) palette.ink else palette.textDim
    val baseModifier = modifier
        .clip(RoundedCornerShape(999.dp))
        .background(container)
    val finalModifier = if (onClick != null) {
        baseModifier.clickable(onClick = onClick)
    } else {
        baseModifier
    }
    Box(
        modifier = finalModifier.padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = content,
            fontFamily = ArcadeFonts.Pixel,
            fontWeight = ArcadeFonts.PixelWeight,
            fontSize = 9.sp,
            letterSpacing = 1.sp
        )
    }
}
