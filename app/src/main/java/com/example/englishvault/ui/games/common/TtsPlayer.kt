package com.example.englishvault.ui.games.common

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single entry point for the in-game speech synthesis.
 *
 * Wraps [TextToSpeech] (Android's stock text-to-speech engine) so the
 * Listening mini-game can pronounce dictionary words without shipping
 * any audio assets. The TTS engine itself is provided by the device
 * (Google TTS, Samsung TTS, etc.) — this wrapper only owns the
 * lifecycle, the language, and the queue.
 *
 * ## Lifecycle
 *
 * `TextToSpeech` is expensive to construct (the engine takes a few
 * hundred milliseconds to initialise), so the wrapper is a Hilt
 * `@Singleton` and the instance is reused across mini-game runs.
 * Initialisation is async — [isReady] flips to `true` once
 * [TextToSpeech.OnInitListener.onInit] reports [TextToSpeech.SUCCESS].
 * Calls to [speak] that arrive before the engine is ready are
 * discarded (the player would not hear anything anyway, and the
 * in-game `🔊` button re-emits on the next tap).
 *
 * ## Queueing
 *
 * TTS requests are not queued; a new [speak] call **stops** the
 * currently-playing utterance and starts the new one. This matches
 * the in-game flow (one word at a time) and avoids the audible
 * tail-end of the previous word bleeding into the new one. The
 * internal `currentText` field is mirrored in [currentTextFlow] so
 * the UI can disable the "Listen" button while audio is already
 * playing if desired.
 *
 * ## Rate
 *
 * The [speak] overload accepts a `slow` flag that toggles between
 * `SPEECH_RATE_NORMAL` (1.0×) and `SPEECH_RATE_SLOW` (0.7×). The
 * NORMAL run uses the normal rate; WORLD mode does as well — the
 * player can re-listen via the hint item.
 *
 * ## Volume
 *
 * TTS volume is intentionally **independent** of the user's
 * `effectsVolume` slider (which controls the short SFX). Mixing the
 * two would make the device's media-stream volume interact with the
 * user's SFX preference in a confusing way — the device's own
 * volume rockers already govern speech.
 */
@Singleton
class TtsPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val _isReady = MutableStateFlow(false)
    /** Reactive readiness flag — `true` once the TTS engine is bound. */
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _currentTextFlow = MutableStateFlow<String?>(null)
    /**
     * Text of the utterance currently playing, or `null` when idle.
     * Lets the UI mirror the TTS state without polling.
     */
    val currentTextFlow: StateFlow<String?> = _currentTextFlow.asStateFlow()

    private var tts: TextToSpeech? = null
    private var initAttempted: Boolean = false

    /**
     * Lazily creates the [TextToSpeech] engine on first use. Safe to
     * call multiple times — subsequent calls are no-ops once the
     * engine is allocated.
     *
     * The engine is configured for `Locale.US` English. Other
     * dictionaries (e.g. `Locale.UK`) are not strictly necessary for
     * the bundled content which uses American spelling
     * (`color`, `organize`, …).
     */
    fun ensureInitialized() {
        if (tts != null || initAttempted) return
        initAttempted = true
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val engine = tts ?: return@TextToSpeech
                val localeResult = engine.setLanguage(Locale.US)
                val ready = localeResult != TextToSpeech.LANG_MISSING_DATA &&
                    localeResult != TextToSpeech.LANG_NOT_SUPPORTED
                if (ready) {
                    engine.setSpeechRate(SPEECH_RATE_NORMAL)
                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _currentTextFlow.value = utteranceId
                        }

                        override fun onDone(utteranceId: String?) {
                            _currentTextFlow.value = null
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            _currentTextFlow.value = null
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            _currentTextFlow.value = null
                        }
                    })
                }
                _isReady.value = ready
            } else {
                _isReady.value = false
            }
        }
    }

    /**
     * Pronounces [word] in English. If the engine is not yet ready
     * the call is a no-op; the user can re-tap the listen button
     * once [isReady] flips to `true`.
     *
     * @param word The English word to speak. Whitespace is trimmed
     *   and an empty string is rejected.
     * @param slow When `true`, the speech rate is dropped to
     *   [SPEECH_RATE_SLOW] for the duration of the utterance.
     */
    fun speak(word: String, slow: Boolean = false) {
        val trimmed = word.trim()
        if (trimmed.isEmpty()) return
        ensureInitialized()
        val engine = tts ?: return
        if (!_isReady.value) return

        if (slow) {
            engine.setSpeechRate(SPEECH_RATE_SLOW)
        } else {
            engine.setSpeechRate(SPEECH_RATE_NORMAL)
        }
        engine.stop()
        engine.speak(
            trimmed,
            TextToSpeech.QUEUE_FLUSH,
            null,
            trimmed
        )
    }

    /**
     * Stops the currently playing utterance, if any. Safe to call
     * when nothing is playing.
     */
    fun stop() {
        tts?.stop()
        _currentTextFlow.value = null
    }

    /**
     * Releases the [TextToSpeech] engine. Called from the Hilt
     * component's teardown path; the player cannot guarantee a
     * teardown hook today but exposes the API so future iteration
     * can wire it up.
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        _isReady.value = false
        _currentTextFlow.value = null
        initAttempted = false
    }

    companion object {
        /** Default playback rate — unchanged from the engine default. */
        const val SPEECH_RATE_NORMAL: Float = 1.0f

        /** Slow rate for the future "slow pronunciation" hint. */
        const val SPEECH_RATE_SLOW: Float = 0.7f
    }
}