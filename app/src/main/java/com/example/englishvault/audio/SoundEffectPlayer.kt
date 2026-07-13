package com.example.englishvault.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single entry point for playing short sound effects.
 *
 * Backed by [SoundPool], which is the Android-native low-latency
 * audio engine for game SFX. Compared to the previous `ToneGenerator`
 * implementation this solves two issues at once:
 *
 *  - **Volume slider is honoured.** `SoundPool.play(id, volL, volR,
 *    ...)` applies the volume **per playback**, with no gating by
 *    the system's notification / media stream volume. The Settings
 *    effects slider therefore controls SFX loudness 1:1, even when
 *    the device is on silent.
 *  - **No more UI-thread stall.** `SoundPool.play()` is non-blocking
 *    and returns immediately, so callers no longer need to hop to
 *    `Dispatchers.IO` to avoid the multi-millisecond hitch that
 *    `ToneGenerator`'s native constructor used to produce.
 *
 * The pool is allocated lazily once per app process (Hilt
 * `@Singleton`) and the assets referenced by every [SoundKey] are
 * pre-loaded in `init`. `SoundPool.load()` is async, so the very
 * first play after process start may be slightly delayed while the
 * file finishes decoding; subsequent plays are instant.
 */
@Singleton
class SoundEffectPlayer @Inject constructor(
    @ApplicationContext context: Context
) {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val soundIds: Map<SoundKey, Int> = SoundKey.entries.associateWith { key ->
        soundPool.load(context, key.resId, 1)
    }

    /**
     * Plays [key] at the user-selected effects level.
     *
     * @param key Which SFX to play.
     * @param effectsVolume User preference in `[0.0, 1.0]`. The
     *   effective gain passed to the underlying pool is
     *   `effectsVolume.coerceIn(0, 1) * key.gain`. A zero volume is a
     *   no-op, so callers don't need to pre-check the slider value.
     */
    fun play(key: SoundKey, effectsVolume: Float) {
        val gain = (effectsVolume.coerceIn(0f, 1f) * key.gain).coerceIn(0f, 1f)
        if (gain <= 0f) return
        val soundId = soundIds[key] ?: return
        soundPool.play(soundId, gain, gain, PRIORITY_NORMAL, NO_LOOP, FULL_RATE)
    }

    companion object {
        /**
         * Maximum number of overlapping SFX streams. Four is enough
         * headroom for the player answering in rapid succession
         * without losing the first beep to the second.
         */
        private const val MAX_STREAMS: Int = 4

        /** `SoundPool.play()` priority — normal queue position. */
        private const val PRIORITY_NORMAL: Int = 1

        /** No looping for one-shot SFX. */
        private const val NO_LOOP: Int = 0

        /** Native playback rate — `1.0` means unaltered pitch / speed. */
        private const val FULL_RATE: Float = 1.0f
    }
}