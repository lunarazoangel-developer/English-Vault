package com.example.englishvault.ui.games.lettersoup.util

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

/**
 * 26-color palette keyed by uppercase A–Z.
 *
 * Each letter gets its own deterministic background hue so the player
 * can quickly tell which cells belong to the same word. Hues are
 * evenly distributed around the colour wheel (`hue = (letter - 'A') *
 * 360 / 26`) at fixed saturation / lightness, which keeps the palette
 * vivid without any single colour clashing with the brand palette.
 *
 * Letter text is always white because every generated hue has enough
 * saturation / lightness contrast to guarantee WCAG-AA legibility at
 * the 18–20sp sizes the board uses.
 */
object LetterPalette {

    private val background: Map<Char, Color> = ('A'..'Z').associateWith { letter ->
        val index = letter - 'A'
        val hue = (index.toFloat() / 26f) * 360f
        hslToColor(hue, saturation = 0.65f, lightness = 0.55f)
    }

    /** Background colour for the tile that displays [letter]. */
    fun backgroundFor(letter: Char): Color =
        background[letter.uppercaseChar()] ?: Color(0xFF455A64)

    /** Foreground (letter glyph) colour. Always white for legibility. */
    val letterForeground: Color = Color.White

    /**
     * Converts HSL → RGB → packed ARGB [Color]. Adapted from the
     * standard CSS-style algorithm so the output is predictable without
     * pulling in `androidx.compose.ui.graphics.Color.hsl`.
     */
    private fun hslToColor(hue: Float, saturation: Float, lightness: Float): Color {
        val h = ((hue % 360f) + 360f) % 360f / 360f
        val s = saturation.coerceIn(0f, 1f)
        val l = lightness.coerceIn(0f, 1f)

        val c = (1f - abs(2f * l - 1f)) * s
        val x = c * (1f - abs((h * 6f) % 2f - 1f))
        val m = l - c / 2f

        val (r1, g1, b1) = when {
            h < 1f / 6f -> Triple(c, x, 0f)
            h < 2f / 6f -> Triple(x, c, 0f)
            h < 3f / 6f -> Triple(0f, c, x)
            h < 4f / 6f -> Triple(0f, x, c)
            h < 5f / 6f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        val r = ((r1 + m) * 255f).toInt().coerceIn(0, 255)
        val g = ((g1 + m) * 255f).toInt().coerceIn(0, 255)
        val b = ((b1 + m) * 255f).toInt().coerceIn(0, 255)
        return Color(0xFF000000.toInt() or (r shl 16) or (g shl 8) or b)
    }
}