package com.example.englishvault.ui.progress.arcade.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.englishvault.ui.progress.arcade.LocalArcadePalette

/**
 * Two-state on/off switch with the arcade pill treatment.
 *
 * Track is 52×28 dp, fully rounded. When [checked] the track fills
 * with `palette.success` and the 22 dp circular thumb sits on the
 * right edge; when off, the track is `palette.switchOff` and the
 * thumb sits on the left. The thumb animates horizontally with
 * [tween] for a snappy but smooth feel.
 *
 * The thumb color also flips: dark ink on the green "on" track,
 * light text on the dark "off" track. That contrast gives the
 * switch a second visual cue beyond the position so the state is
 * unambiguous at a glance.
 */
@Composable
fun ArcadeSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalArcadePalette.current
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 24.dp else 0.dp,
        animationSpec = tween(durationMillis = 140),
        label = "arcade-switch-thumb"
    )
    val trackColor = if (checked) palette.success else palette.switchOff
    val thumbColor = if (checked) palette.ink else palette.textMain
    Box(
        modifier = modifier
            .size(width = 52.dp, height = 28.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(trackColor)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(22.dp)
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}
