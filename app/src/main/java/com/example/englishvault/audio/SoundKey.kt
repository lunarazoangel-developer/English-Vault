package com.example.englishvault.audio

import android.media.ToneGenerator

/**
 * Catalog of every short sound effect the app can play.
 *
 * Phase 7.2 ships only [Correct] as an active entry; [Wrong] is
 * declared so the call sites in the mini-game VMs can reference it
 * without a future code change once the wrong-answer SFX is wired.
 *
 * The [tone] field carries the placeholder `ToneGenerator` constant
 * used while no real audio asset lives in `res/raw/`. The [gain] is a
 * relative scalar (0.0..1.0) that the player multiplies by the user's
 * `effectsVolume` setting so individual effects can be tuned louder
 * or softer relative to each other without re-balancing every call
 * site.
 *
 * When real OGG / MP3 assets land in `res/raw/` the implementation in
 * [SoundEffectPlayer] will switch to `SoundPool` and the `tone` field
 * will become the raw-resource id. The [gain] field stays stable.
 */
enum class SoundKey(val tone: Int, val gain: Float) {
    /** Positive beep played when the user picks the correct answer. */
    Correct(ToneGenerator.TONE_PROP_ACK, 0.8f),

    /**
     * Negative beep played on a wrong answer. Defined here so the VM
     * can already reference the constant; the actual call is not
     * wired yet.
     */
    Wrong(ToneGenerator.TONE_PROP_NACK, 0.8f)
}