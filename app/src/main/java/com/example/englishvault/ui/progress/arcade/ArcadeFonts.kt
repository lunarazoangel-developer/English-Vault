package com.example.englishvault.ui.progress.arcade

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * Font placeholders for the arcade dashboard.
 *
 * The "production" version of this design relies on two display
 * faces (Bungee for headers and buttons, Press Start 2P for tiny
 * labels). Neither is bundled in `res/font/` yet, so this file
 * exposes stand-ins that share the same visual weight so the rest
 * of the dashboard can be iterated on without blocking on the
 * font asset import.
 *
 * | Role       | Production font  | Placeholder                              |
 * |------------|------------------|------------------------------------------|
 * | Display    | Bungee           | `SansSerif` + `ExtraBold`                |
 * | Pixel      | Press Start 2P   | `Monospace` + `Bold`                     |
 * | Body       | JetBrains Mono   | `Monospace` + `Normal`                   |
 *
 * To switch to the real faces, drop the .ttf files into
 * `app/src/main/res/font/` and replace the [FontFamily] values
 * below with `FontFamily(Font(R.font.bungee, FontWeight.Bold))`
 * style lookups. The rest of the dashboard reads from these three
 * aliases only, so the migration is one file.
 */
object ArcadeFonts {
    val Display: FontFamily = FontFamily.SansSerif
    val Pixel: FontFamily = FontFamily.Monospace
    val Body: FontFamily = FontFamily.Monospace

    /** Default weight used by [Display] text. */
    val DisplayWeight: FontWeight = FontWeight.ExtraBold

    /** Default weight used by [Pixel] text. */
    val PixelWeight: FontWeight = FontWeight.Bold

    /** Default weight used by [Body] text. */
    val BodyWeight: FontWeight = FontWeight.Normal
}
