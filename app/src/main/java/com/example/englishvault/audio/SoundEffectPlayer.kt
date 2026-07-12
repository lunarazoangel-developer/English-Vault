package com.example.englishvault.audio

import android.media.ToneGenerator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single entry point for playing short sound effects.
 *
 * Phase 7.2 placeholder: backs onto [ToneGenerator], which produces
 * system DTMF-style beeps with zero asset dependency. When real audio
 * files land in `res/raw/`, swap the internal implementation to
 * `SoundPool` — the public API ([play]) is shaped so callers don't
 * change.
 *
 * Lifecycle: a fresh [ToneGenerator] is created on every [play] call
 * with the right relative volume and released immediately afterwards.
 * [ToneGenerator] exposes no per-call volume control — `startTone`
 * only takes the tone type and an optional duration — so recreating is
 * the cleanest way to react to slider changes in real time. The OS
 * reclaims the underlying native handle within microseconds so the
 * overhead is negligible.
 */
@Singleton
class SoundEffectPlayer @Inject constructor() {

    /**
     * Plays [key] at the user-selected effects level.
     *
     * @param key Which SFX to play.
     * @param effectsVolume User preference in `[0.0, 1.0]`. The
     *   effective gain passed to the underlying generator is
     *   `effectsVolume.coerceIn(0, 1) * key.gain`. A zero volume is a
     *   no-op, so callers don't need to pre-check the slider value.
     */
    fun play(key: SoundKey, effectsVolume: Float) {
        val gain = (effectsVolume.coerceIn(0f, 1f) * key.gain).coerceIn(0f, 1f)
        if (gain <= 0f) return

        // ToneGenerator uses its own 0..100 scale. Passing
        // AudioManager.STREAM_MUSIC (value = 3) silently capped the
        // beep at ~3 % loudness, which was almost inaudible on the
        // emulator. Multiplying by the user-selected gain keeps the
        // slider responsive.
        val relativeVolume = (gain * MAX_TONE_VOLUME).toInt().coerceIn(1, MAX_TONE_VOLUME)

        // Allocate a fresh generator per call so the volume reflects
        // the current slider value. Wrap in try / finally so a
        // platform failure never leaks the native handle.
        val generator = try {
            ToneGenerator(relativeVolume, ToneGenerator.TONE_DTMF_S)
        } catch (t: Throwable) {
            return
        }
        try {
            // Explicit duration so the beep is long enough to be
            // heard even at low gain (the default for TONE_PROP_ACK
            // is around 150 ms, which feels like a click rather than
            // a reward).
            generator.startTone(key.tone, TONE_DURATION_MS)
        } finally {
            generator.release()
        }
    }

    companion object {
        /**
         * `ToneGenerator` expects a relative volume in `[0, 100]`.
         * Anything above the platform maximum is clamped internally,
         * but staying at `100` keeps the math predictable from the
         * Kotlin side. The actual loudness reaching the speaker is
         * also gated by the system's media stream volume.
         */
        private const val MAX_TONE_VOLUME: Int = 100

        /**
         * Duration of every SFX beep. `TONE_PROP_ACK` defaults to a
         * short click (~150 ms); extending to 300 ms gives the player
         * an actual reward feel and survives quieter streams better.
         */
        private const val TONE_DURATION_MS: Int = 300
    }
}