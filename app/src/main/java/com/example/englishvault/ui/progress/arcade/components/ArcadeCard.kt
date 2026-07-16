package com.example.englishvault.ui.progress.arcade.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.englishvault.ui.progress.arcade.ArcadeFonts
import com.example.englishvault.ui.progress.arcade.LocalArcadePalette

/**
 * Solid-color container used everywhere in the dashboard.
 *
 * The signature visual cue is a 4 dp colored left border that
 * doubles as the card's category accent (or any other "meaning"
 * the caller wants to surface). The rest of the surface is a
 * flat `palette.surface` with a 16 dp corner radius — no shadow,
 * no gradient.
 *
 * Implemented as a [Row] of two `Box`es so the colored stripe is a
 * real element of the layout (not a `Modifier.border`, which would
 * draw on all four sides). The content area takes the remaining
 * width via [RowScope.weight]; the stripe owns its own 4 dp track.
 *
 * Reads the active [com.example.englishvault.ui.progress.arcade.ArcadePalette]
 * through [LocalArcadePalette] so flipping the theme at the root
 * of the Compose tree re-tints the whole card automatically.
 */
@Composable
fun ArcadeCard(
    modifier: Modifier = Modifier,
    accent: Color = LocalArcadePalette.current.primary,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit
) {
    val palette = LocalArcadePalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.surface)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(accent)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(contentPadding),
            contentAlignment = Alignment.TopStart
        ) {
            content()
        }
    }
}

/**
 * Lightweight text label rendered in the pixel-style
 * [ArcadeFonts.Pixel] family. Used for the secondary lines under
 * headlines (e.g. "Level X / Y" on the category cards). Centralises
 * the size and color so the look stays consistent across every
 * card.
 */
@Composable
fun ArcadeLabel(
    text: String,
    color: Color = LocalArcadePalette.current.textDim,
    modifier: Modifier = Modifier,
    style: TextStyle = androidx.compose.material3.MaterialTheme.typography.labelSmall
) {
    Text(
        text = text,
        color = color,
        fontFamily = ArcadeFonts.Pixel,
        fontWeight = ArcadeFonts.PixelWeight,
        style = style,
        modifier = modifier
    )
}
