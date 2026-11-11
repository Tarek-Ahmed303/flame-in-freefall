package com.flame.astronaut.voice

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/**
 * Reads an emergency step aloud with Android's built-in TextToSpeech.
 *
 * Behaviour (as agreed): BUTTON ONLY.
 *  - Nothing is spoken automatically when the alarm starts.
 *  - Tapping the speaker button reads the current step (title + body).
 *  - Tapping it again while it is speaking stops the speech.
 *  - Moving to another step / leaving the screen stops the speech.
 *
 * Uses USAGE_ALARM audio so the voice is audible during a fire alert even
 * when media volume is low (same channel as the alarm sound).
 *
 * One instance per emergency screen. Call [release] in onDestroy / DisposableEffect.
 */
class StepSpeaker(
    context: Context,
    private val onSpeakingChanged: (Boolean) -> Unit = {},
) {
    private var ready = false
    private var pendingText: String? = null
    private var speaking = false

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            val res = tts.setLanguage(Locale.US)
            ready = res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED
            if (!ready) Log.w(TAG, "English TTS voice not installed on this device")
            tts.setSpeechRate(0.95f)   // slightly slower = clearer under stress
            tts.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            // A tap that arrived before the engine finished initialising.
            pendingText?.let { speakNow(it) }
            pendingText = null
        } else {
            Log.e(TAG, "TextToSpeech init failed: $status")
        }
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = setSpeaking(true)
            override fun onDone(utteranceId: String?) = setSpeaking(false)
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = setSpeaking(false)
            override fun onError(utteranceId: String?, errorCode: Int) = setSpeaking(false)
            override fun onStop(utteranceId: String?, interrupted: Boolean) = setSpeaking(false)
        })
    }

    val isSpeaking: Boolean get() = speaking

    /** The speaker-button handler: speak the step, or stop if already speaking. */
    fun toggle(stepNumber: Int, title: String, body: String) {
        if (speaking) stop() else speak(stepNumber, title, body)
    }

    fun speak(stepNumber: Int, title: String, body: String) {
        val text = buildSpokenText(stepNumber, title, body)
        if (!ready) { pendingText = text; return }
        speakNow(text)
    }

    fun stop() {
        pendingText = null
        tts.stop()
        setSpeaking(false)
    }

    fun release() {
        tts.stop()
        tts.shutdown()
    }

    private fun speakNow(text: String) {
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        // QUEUE_FLUSH: a new tap always replaces whatever was being read.
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, UTTERANCE_ID)
    }

    private fun setSpeaking(value: Boolean) {
        if (speaking == value) return
        speaking = value
        onSpeakingChanged(value)   // called on a background thread – post to UI if needed
    }

    companion object {
        private const val TAG = "StepSpeaker"
        private const val UTTERANCE_ID = "emergency_step"

        /**
         * "Step 1. Protect Yourself. Protect yourself from smoke ..."
         * Also expands symbols/abbreviations so they read naturally aloud.
         */
        fun buildSpokenText(stepNumber: Int, title: String, body: String): String {
            val raw = "Step $stepNumber. ${title.trim().trimEnd('.')}. ${body.trim()}"
            return raw
                .replace(Regex("\\bO2\\b|O₂"), "oxygen")
                .replace(Regex("\\bCO2\\b|CO₂"), "carbon dioxide")
                .replace(Regex("\\bCO\\b"), "carbon monoxide")
                .replace(Regex("(\\d)\\s*%"), "$1 percent")
                .replace(Regex("\\bppm\\b", RegexOption.IGNORE_CASE), "parts per million")
                .replace(Regex("(\\d)\\s*m/s\\b"), "$1 meters per second")
                .replace(Regex("[·•*_#`]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        }
    }
}
