package com.example.data.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class VoiceToTextState(
    val isAvailable: Boolean = true,
    val isListening: Boolean = false,
    val partialText: String = "",
    val finalText: String = "",
    val rmsDb: Float = 0f,
    val errorMessage: String? = null
)

/**
 * Wraps Android's native SpeechRecognizer API for real-time streaming voice-to-text transcription
 * inside the chat composer before sending.
 */
class VoiceToTextManager(context: Context) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null

    private val _state = MutableStateFlow(
        VoiceToTextState(isAvailable = isSpeechRecognitionSupported())
    )
    val state: StateFlow<VoiceToTextState> = _state.asStateFlow()

    private fun isSpeechRecognitionSupported(): Boolean {
        return try {
            SpeechRecognizer.isRecognitionAvailable(appContext)
        } catch (_: Exception) {
            false
        }
    }

    fun startListening(languageTag: String = Locale.getDefault().toLanguageTag()) {
        mainHandler.post {
            if (!isSpeechRecognitionSupported()) {
                _state.value = _state.value.copy(
                    isAvailable = false,
                    isListening = false,
                    errorMessage = "La reconnaissance vocale Android n'est pas disponible sur cet appareil."
                )
                return@post
            }

            try {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
                    setRecognitionListener(createRecognitionListener())
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
                }

                _state.value = VoiceToTextState(
                    isAvailable = true,
                    isListening = true,
                    partialText = "",
                    finalText = "",
                    rmsDb = 0f,
                    errorMessage = null
                )
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isListening = false,
                    errorMessage = "Impossible de démarrer la dictée vocale : ${e.localizedMessage ?: "erreur interne"}"
                )
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {
            }
            _state.value = _state.value.copy(isListening = false, rmsDb = 0f)
        }
    }

    fun cancel() {
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
            } catch (_: Exception) {
            }
            _state.value = _state.value.copy(
                isListening = false,
                partialText = "",
                rmsDb = 0f,
                errorMessage = null
            )
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    fun destroy() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
            } catch (_: Exception) {
            }
            speechRecognizer = null
        }
    }

    private fun createRecognitionListener(): RecognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _state.value = _state.value.copy(isListening = true, errorMessage = null)
        }

        override fun onBeginningOfSpeech() {
            _state.value = _state.value.copy(isListening = true)
        }

        override fun onRmsChanged(rmsdB: Float) {
            _state.value = _state.value.copy(rmsDb = rmsdB.coerceIn(0f, 12f))
        }

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() {
            _state.value = _state.value.copy(isListening = false, rmsDb = 0f)
        }

        override fun onError(error: Int) {
            val msg = mapSpeechError(error)
            _state.value = _state.value.copy(
                isListening = false,
                rmsDb = 0f,
                errorMessage = msg
            )
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val best = matches?.firstOrNull()?.trim().orEmpty()
            _state.value = _state.value.copy(
                isListening = false,
                rmsDb = 0f,
                partialText = best,
                finalText = best,
                errorMessage = null
            )
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = matches?.firstOrNull()?.trim().orEmpty()
            if (partial.isNotEmpty()) {
                _state.value = _state.value.copy(
                    partialText = partial,
                    errorMessage = null
                )
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun mapSpeechError(errorCode: Int): String = when (errorCode) {
        SpeechRecognizer.ERROR_AUDIO -> "Erreur d'enregistrement audio."
        SpeechRecognizer.ERROR_CLIENT -> "Session de dictée interrompue."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission microphone requise pour la dictée vocale."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "Erreur réseau pour la reconnaissance vocale."
        SpeechRecognizer.ERROR_NO_MATCH -> "Aucune parole reconnue. Réessayez plus distinctement."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Service vocal occupé, réessayez dans un instant."
        SpeechRecognizer.ERROR_SERVER -> "Service de reconnaissance vocale indisponible."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Temps d'écoute écoulé sans parole détectée."
        else -> "Erreur reconnaissance vocale (code $errorCode)."
    }
}
