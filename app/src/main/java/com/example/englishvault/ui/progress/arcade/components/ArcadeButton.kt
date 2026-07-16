package com.example.englishvault.ui.progress.arcade.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.englishvault.ui.progress.arcade.ArcadeFonts
import com.example.englishvault.ui.progress.arcade.LocalArcadePalette

/**
 * Pressable pill button with the arcade 3D treatment.
 *
 * Renders as two stacked boxes: a solid [shadow] rectangle drawn 6 dp
 * below the button proper, and the button face on top. On press
 * the face drops by 4 dp (so it "sinks" into the shadow) and the
 * shadow shrinks to 2 dp so the visual depth is preserved. Both
 * offsets animate with [tween] for the snappy-but-not-jerky feel
 * the design brief asks for.
 *
 * No box-shadow blur is used anywhere — the depth comes from the
 * solid offset rectangles, which is what gives the button the
 * "physical chip" look.
 */
@Composable
fun ArcadeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = LocalArcadePalette.current.primary,
    shadow: Color = LocalArcadePalette.current.shadowOf(color),
    textColor: Color = LocalArcadePalette.current.ink,
    enabled: Boolean = true
) {
    val palette = LocalArcadePalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val faceOffset by animateDpAsState(
        targetValue = if (isPressed) 4.dp else 0.dp,
        animationSpec = tween(durationMillis = 90),
        label = "arcade-button-face"
    )
    val shadowOffset by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 6.dp,
        animationSpec = tween(durationMillis = 90),
        label = "arcade-button-shadow"
    )

    Box(
        modifier = modifier
            .height(48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = shadowOffset)
                .clip(RoundedCornerShape(999.dp))
                .background(if (enabled) shadow else palette.border)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = faceOffset)
                .clip(RoundedCornerShape(999.dp))
                .background(if (enabled) color else palette.surfaceDark),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (enabled) textColor else palette.textDim,
                fontFamily = ArcadeFonts.Display,
                fontWeight = ArcadeFonts.DisplayWeight,
                fontSize = 16.sp,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Compact square variant used for the settings icon in the progress
 * header. Same press / shadow treatment as [ArcadeButton] but
 * rendered as a 40 dp square so it visually balances the text next
 * to it.
 */
@Composable
fun ArcadeIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = LocalArcadePalette.current.secondary,
    shadow: Color = LocalArcadePalette.current.shadowOf(color),
    content: @Composable () -> Unit
) {
    val palette = LocalArcadePalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val faceOffset by animateDpAsState(
        targetValue = if (isPressed) 3.dp else 0.dp,
        animationSpec = tween(durationMillis = 90),
        label = "arcade-icon-button-face"
    )
    val shadowOffset by animateDpAsState(
        targetValue = if (isPressed) 1.dp else 4.dp,
        animationSpec = tween(durationMillis = 90),
        label = "arcade-icon-button-shadow"
    )

    Box(
        modifier = modifier
            .size(40.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = shadowOffset)
                .clip(RoundedCornerShape(12.dp))
                .background(shadow)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = faceOffset)
                .clip(RoundedCornerShape(12.dp))
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}
