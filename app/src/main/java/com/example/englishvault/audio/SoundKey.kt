package com.example.englishvault.audio

import com.example.englishvault.R

/**
 * Catalog of every short sound effect the app can play.
 *
 * Backed by real audio assets bundled in `res/raw/`. The [resId]
 * field is the Android resource id of the file (e.g.
 * `R.raw.correct_sound` → `res/raw/correct_sound.mp3`). The [gain]
 * is a relative scalar in `[0.0, 1.0]` that [SoundEffectPlayer]
 * multiplies by the user's `effectsVolume` setting so individual
 * effects can be tuned louder or softer relative to each other
 * without re-balancing every call site.
 *
 * Currently only ships [Correct]. A `Wrong` entry will land here
 * alongside a `wrong_sound.mp3` asset when the wrong-answer SFX is
 * wired up.
 */
enum class SoundKey(val resId: Int, val gain: Float) {
    /**
     * Positive SFX played when the user picks the correct answer.
     * Backed by `R.raw.correct_sound` (file lives at
     * `res/raw/correct_sound.mp3`). The [gain] stays at `1.0f`
     * because `SoundPool` honours the per-playback volume directly
     * — no headroom needed the way `ToneGenerator` required.
     */
    Correct(R.raw.correct_sound, 1.0f)
}