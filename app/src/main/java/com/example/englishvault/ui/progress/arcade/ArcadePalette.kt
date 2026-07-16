package com.example.englishvault.ui.progress.arcade

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.englishvault.ui.words.WordTypeFilter

/**
 * Color tokens for the arcade-style UI.
 *
 * Every value is a fully saturated, solid color. No gradients, no
 * semi-transparent overlays, no blur — the design language is
 * "physical chip on a table" and demands clean hues.
 *
 * Two palettes ship with the project:
 *  - [ArcadePalettes.Dark] — the canonical dark variant (deep purple
 *    background, cream text, vivid accents).
 *  - [ArcadePalettes.Light] — a paper-style light variant (cream
 *    background, dark text, same accents) so the user can flip the
 *    app to a brighter feel without leaving the arcade language.
 *
 * Components read the active palette through [LocalArcadePalette]
 * rather than referencing one of the static instances directly, so a
 * `CompositionLocalProvider` swap on the root of the Compose tree
 * is enough to flip the whole UI from dark to light.
 *
 * The four accent colors (primary, secondary, highlight, success)
 * and the eight category colors stay the same in both variants —
 * they are the "ink" colors that look good on either surface.
 *
 * @property primary Vivid pink. CTAs, active accents, primary chips.
 * @property secondary Electric cyan. Secondary accents, controls.
 * @property highlight Gold. XP / streak / "premium" elements.
 * @property success Lime. Correct answers, success states.
 * @property background Screen background.
 * @property surface Card / panel surface.
 * @property surfaceDark Secondary surface (chips, inactive items).
 * @property border Subtle separator / outline.
 * @property ink Dark ink for text drawn on top of the accents.
 * @property textMain Primary text color.
 * @property textDim Secondary text color.
 * @property shadow Color used for the offset drop of 3D buttons.
 * @property switchOff Background of the off-state toggle track.
 */
@Immutable
data class ArcadePalette(
    val primary: Color,
    val secondary: Color,
    val highlight: Color,
    val success: Color,
    val background: Color,
    val surface: Color,
    val surfaceDark: Color,
    val border: Color,
    val ink: Color,
    val textMain: Color,
    val textDim: Color,
    val shadow: Color,
    val switchOff: Color
) {
    /**
     * Maps every tracked grammatical bucket to its accent color.
     * Used as the colored left border on the per-category progress
     * cards and as the fill on the per-category XP bar.
     *
     * Verbs (regular and irregular) share the same pink so the
     * dashboard reads as "verbs" at a glance even though they are
     * two separate progression rows.
     */
    fun categoryColor(filter: WordTypeFilter): Color = when (filter) {
        WordTypeFilter.VERBS_REGULAR,
        WordTypeFilter.VERBS_IRREGULAR -> Color(0xFFFF007A)
        WordTypeFilter.ADJECTIVES -> Color(0xFFFFD700)
        WordTypeFilter.ADVERBS -> Color(0xFF9D4EDD)
        WordTypeFilter.NOUNS -> Color(0xFF00D4FF)
        WordTypeFilter.CONJUNCTIONS -> Color(0xFFFF8C00)
        WordTypeFilter.PREPOSITIONS -> Color(0xFFFFF5E6)
        WordTypeFilter.INTERJECTIONS -> Color(0xFF5FB878)
        WordTypeFilter.ALL,
        WordTypeFilter.MINE -> surfaceDark
    }

    /**
     * Returns a darker version of [color] suitable for the offset
     * shadow of a 3D button rendered on top of it. The mix keeps
     * the hue recognizable while pushing the value down so the
     * shadow reads as a true drop, not as a duplicate chip.
     */
    fun shadowOf(color: Color): Color {
        val r = (color.red * 0.75f).coerceIn(0f, 1f)
        val g = (color.green * 0.75f).coerceIn(0f, 1f)
        val b = (color.blue * 0.75f).coerceIn(0f, 1f)
        return Color(red = r, green = g, blue = b, alpha = color.alpha)
    }

    /**
     * Rotating accent used by the skill tiles so each card gets its
     * own border color. The order is `primary → secondary →
     * highlight → success` and wraps around.
     */
    fun skillAccent(index: Int): Color = when (index % 4) {
        0 -> primary
        1 -> secondary
        2 -> highlight
        else -> success
    }
}

/**
 * Static, project-wide palette instances.
 *
 *  - [Dark] is the canonical design the visual iteration is built
 *    around; it is the default for [LocalArcadePalette] and is also
 *    what the [com.example.englishvault.ui.components.ArcadeBottomBar]
 *    uses directly (the bottom bar is a fixed chrome that does not
 *    respond to the theme toggle).
 *  - [Light] is the paper-style variant swapped in by the
 *    `CompositionLocalProvider` at the root of the Compose tree
 *    when the user picks the light theme in Settings.
 */
object ArcadePalettes {
    val Dark: ArcadePalette = ArcadePalette(
        primary = Color(0xFFFF007A),
        secondary = Color(0xFF00D4FF),
        highlight = Color(0xFFFFD700),
        success = Color(0xFF5FB878),
        background = Color(0xFF0A0518),
        surface = Color(0xFF1A0F3A),
        surfaceDark = Color(0xFF2A1A4A),
        border = Color(0xFF4A2A6A),
        ink = Color(0xFF0A0518),
        textMain = Color(0xFFFFF5E6),
        textDim = Color(0xFF8A7DA0),
        shadow = Color(0xFF050210),
        switchOff = Color(0xFF050210)
    )

    val Light: ArcadePalette = ArcadePalette(
        primary = Color(0xFFFF007A),
        secondary = Color(0xFF00D4FF),
        highlight = Color(0xFFFFD700),
        success = Color(0xFF5FB878),
        background = Color(0xFFFFF5E6),
        surface = Color(0xFFFFE9C7),
        surfaceDark = Color(0xFFF0DDB8),
        border = Color(0xFFC8B886),
        ink = Color(0xFFFFF5E6),
        textMain = Color(0xFF0A0518),
        textDim = Color(0xFF5C4D6F),
        shadow = Color(0xFF9D7F4F),
        switchOff = Color(0xFFD4C7A8)
    )
}

/**
 * Provides the active [ArcadePalette] to every arcade component in
 * the composition. Defaults to [ArcadePalettes.Dark] so a screen
 * that forgets to wire a provider still renders the canonical
 * design.
 */
val LocalArcadePalette = staticCompositionLocalOf { ArcadePalettes.Dark }
