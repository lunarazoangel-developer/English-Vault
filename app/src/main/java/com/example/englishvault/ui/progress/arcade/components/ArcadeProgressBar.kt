package com.example.englishvault.ui.progress.arcade.components

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.englishvault.ui.progress.arcade.LocalArcadePalette

/**
 * Solid-fill progress bar with no gradient.
 *
 * The track is `palette.border` and the fill is a single solid
 * color passed in by the caller (typically the category accent or
 * `palette.success` for the "learned" bar). The fill width
 * animates from 0 to the requested fraction on first composition
 * with the cubic-bezier ease `cubic-bezier(0.16, 1, 0.3, 1)` the
 * design brief calls out, so the bar feels like it is "settling"
 * into place rather than snapping.
 */
@Composable
fun ArcadeProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = LocalArcadePalette.current.primary,
    trackColor: Color = LocalArcadePalette.current.border,
    height: Dp = 8.dp
) {
    val animatedFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(
            durationMillis = 1200,
            easing = ArcadeBarEasing
        ),
        label = "arcade-progress"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedFraction)
                .fillMaxHeight()
                .clip(RoundedCornerShape(height / 2))
                .background(color)
        )
    }
}

/**
 * Cubic-bezier(0.16, 1, 0.3, 1) — the "settle" easing the design
 * brief calls out. `Easing` in Compose receives the linear
 * progress in `[0, 1]` and returns the eased progress in `[0, 1]`.
 * P0 is hardcoded at (0, 0) inside the solver so the public
 * function only needs to accept P1.x, P2.x and P3.x.
 */
private val ArcadeBarEasing = Easing { t ->
    val u = solveCubicBezier(t, 0.16f, 0.3f, 1f)
    cubicBezierY(u, 0f, 1f, 1f, 1f)
}

/** Newton-Raphson solver for the X coordinate of a cubic bezier. */
private fun solveCubicBezier(x: Float, p1x: Float, p2x: Float, p3x: Float): Float {
    var u = x
    repeat(8) {
        val current = cubicBezierY(u, 0f, p1x, p2x, p3x) - x
        val derivative = bezierDerivative(u, 0f, p1x, p2x, p3x)
        if (kotlin.math.abs(derivative) < 1e-6f) return u
        u -= current / derivative
    }
    return u
}

private fun cubicBezierY(
    t: Float,
    p0: Float,
    p1: Float,
    p2: Float,
    p3: Float
): Float {
    val oneMinusT = 1f - t
    return oneMinusT * oneMinusT * oneMinusT * p0 +
        3f * oneMinusT * oneMinusT * t * p1 +
        3f * oneMinusT * t * t * p2 +
        t * t * t * p3
}

private fun bezierDerivative(
    t: Float,
    p0: Float,
    p1: Float,
    p2: Float,
    p3: Float
): Float {
    val oneMinusT = 1f - t
    return 3f * oneMinusT * oneMinusT * (p1 - p0) +
        6f * oneMinusT * t * (p2 - p1) +
        3f * t * t * (p3 - p2)
}
