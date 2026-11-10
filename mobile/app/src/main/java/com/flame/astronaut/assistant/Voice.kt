package com.flame.astronaut.assistant

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Push-to-talk speech input (Android SpeechRecognizer). Results are delivered through [onText]. */
class SpeechInput(
    private val context: Context,
    private val onText: (text: String, final: Boolean) -> Unit,
    private val onState: (listening: Boolean, error: String?) -> Unit,
) {
    private var recognizer: SpeechRecognizer? = null

    fun start() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onState(false, "Voice input isn't available on this device.")
            return
        }
        destroy()
        val r = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = onState(true, null)
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() = onState(false, null)
            override fun onError(error: Int) {
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> null // user just didn't speak
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is needed for voice input."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Voice input needs a network connection."
                    else -> "Couldn't understand that. Try again."
                }
                onState(false, msg)
            }

            override fun onResults(results: Bundle?) {
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { onText(it, true) }
                onState(false, null)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { onText(it, false) }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        r.startListening(intent)
    }

    /** Releasing the button: stop listening, the final result still arrives via onResults. */
    fun stop() {
        recognizer?.stopListening()
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
    }
}

/** Read-aloud (TextToSpeech). */
class Speaker(context: Context) {
    private var ready = false
    private val tts: TextToSpeech = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            ready = true
            tts_setLocale()
        }
    }

    private fun tts_setLocale() { runCatching { tts.language = Locale.US } }

    fun speak(text: String) {
        if (ready && text.isNotBlank()) {
            tts.setSpeechRate(com.flame.astronaut.AppPrefs.speechRate)
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "assistant")
        }
    }

    fun stop() { if (ready) tts.stop() }

    fun shutdown() {
        runCatching { tts.stop(); tts.shutdown() }
    }
}

/** What the read-aloud button reads: ONLY the safety insight, or the first two sentences of the answer if it is empty. */
fun readAloudText(r: AskResponse): String {
    r.safetyInsight?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    val answer = r.answer?.trim().orEmpty()
    return answer.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }.take(2).joinToString(" ")
}
