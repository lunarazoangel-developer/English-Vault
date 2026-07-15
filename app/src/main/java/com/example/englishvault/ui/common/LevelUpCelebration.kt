package com.example.englishvault.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.englishvault.R
import data.game.PromotionEvent
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter

/**
 * Full-screen overlay shown when a [PromotionEvent] arrives.
 *
 * Renders two layered animations:
 *  - A [KonfettiView] that bursts particles from the top-centre of
 *    the screen for ~2 seconds.
 *  - A badge with the category name and the new level, scaled in
 *    with a spring-like overshoot and faded out when dismissed.
 *
 * The overlay auto-dismisses after [CELEBRATION_DURATION_MILLIS] and
 * calls [onConsumed] so the host screen can clear its one-shot
 * `StateFlow<PromotionEvent?>` and avoid replaying the celebration
 * on configuration changes.
 *
 * @param event Current promotion event, or `null` when no
 *   celebration should be visible.
 * @param onConsumed Invoked exactly once per celebration, after the
 *   dismissal animation finishes. The host should clear its event
 *   StateFlow here.
 * @param modifier Optional [Modifier] for sizing (defaults to
 *   fillMaxSize so the overlay covers the whole screen).
 */
@Composable
fun LevelUpCelebrationOverlay(
    event: PromotionEvent?,
    onConsumed: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = event != null,
        enter = fadeIn(animationSpec = tween(durationMillis = 220)),
        exit = fadeOut(animationSpec = tween(durationMillis = 220))
    ) {
        if (event != null) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                KonfettiView(
                    modifier = Modifier.fillMaxSize(),
                    parties = remember { listOf(buildParty()) }
                )
                LevelUpBadge(event = event)

                // Auto-dismiss after a fixed window. The exit animation
                // keeps playing for 220 ms; calling onConsumed here
                // lets the host null out its event StateFlow so the
                // overlay does not re-show on the next recomposition.
                LaunchedEffect(event) {
                    delay(CELEBRATION_DURATION_MILLIS)
                    onConsumed()
                }
            }
        }
    }
}

/**
 * Central "Level X unlocked!" badge. Animates its scale from 0.6 to
 * 1.0 with a soft overshoot so the celebration feels punchy without
 * being noisy.
 */
@Composable
private fun LevelUpBadge(event: PromotionEvent) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(event) {
        visible = true
    }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.6f,
        animationSpec = tween(durationMillis = 360, easing = LinearEasing),
        label = "level-up-badge-scale"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 28.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.level_up_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = stringResource(
                id = R.string.level_up_message,
                event.categoryKey.toCategoryLabel(),
                event.newLevel
            ),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontSize = 18.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/**
 * Maps the canonical category key emitted by [PromotionEvent] (a
 * `WordTypeFilter.name` like `ADJECTIVES`) to a localised label by
 * resolving the matching [com.example.englishvault.ui.words.WordTypeFilter]
 * entry and reading its `labelRes`.
 *
 * Synthetic keys (`LETTER_SOUP`, `LISTENING`) are rendered using the
 * dedicated level-up labels in case they ever flow through the gate.
 */
@Composable
private fun String.toCategoryLabel(): String {
    val labelRes = when (this) {
        "VERBS_REGULAR" -> R.string.words_tab_regular
        "VERBS_IRREGULAR" -> R.string.words_tab_irregular
        "ADJECTIVES" -> R.string.words_tab_adjectives
        "ADVERBS" -> R.string.words_tab_adverbs
        "NOUNS" -> R.string.words_tab_nouns
        "CONJUNCTIONS" -> R.string.words_tab_conjunctions
        "PREPOSITIONS" -> R.string.words_tab_prepositions
        "INTERJECTIONS" -> R.string.words_tab_interjections
        else -> R.string.words_tab_all
    }
    return stringResource(id = labelRes)
}

/**
 * Konfetti [Party] tuned for the level-up overlay:
 *  - Particles burst from the top-centre of the screen so the badge
 *    stays visible in the middle.
 *  - Duration is just long enough for the badge to be readable but
 *    short enough to dismiss before the user gets impatient.
 *  - Palette uses the four ARGB colors from konfetti's reference
 *    example for high contrast on the dark backdrop.
 */
private fun buildParty(): Party = Party(
    speed = 0f,
    maxSpeed = 30f,
    damping = 0.9f,
    spread = 360,
    colors = listOf(
        0xfce18a, 0xff726d, 0xb48def, 0xf4306d
    ),
    emitter = Emitter(duration = 2_000, TimeUnit.MILLISECONDS).max(120),
    position = Position.Relative(0.5, 0.3)
)

/**
 * Total time the overlay stays visible before calling [onConsumed].
 * Tuned to give the badge enough screen-time to be read while
 * keeping the celebration snappy.
 */
private const val CELEBRATION_DURATION_MILLIS: Long = 2_500