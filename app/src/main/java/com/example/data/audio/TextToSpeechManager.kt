package com.example.data.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Gestionnaire de Synthèse Vocale (Text-to-Speech) pour l'écoute à voix haute
 * des messages longs et l'accessibilité dans ADT Pro.
 */
class TextToSpeechManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "TextToSpeechManager"
        const val DEFAULT_LONG_MESSAGE_THRESHOLD_CHARS = 75
        const val DEFAULT_LONG_MESSAGE_THRESHOLD_WORDS = 14
    }

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentMessageId = MutableStateFlow<String?>(null)
    val currentMessageId: StateFlow<String?> = _currentMessageId.asStateFlow()

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _isTtsFeatureEnabled = MutableStateFlow(true)
    val isTtsFeatureEnabled: StateFlow<Boolean> = _isTtsFeatureEnabled.asStateFlow()

    private val _isEngineAvailable = MutableStateFlow(false)
    val isEngineAvailable: StateFlow<Boolean> = _isEngineAvailable.asStateFlow()

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'instanciation de TextToSpeech: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            _isEngineAvailable.value = true
            tts?.setSpeechRate(_speechRate.value)
            configureLocale(Locale.FRENCH)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    scope.launch {
                        _isPlaying.value = true
                        _currentMessageId.value = utteranceId
                    }
                }

                override fun onDone(utteranceId: String?) {
                    scope.launch {
                        _isPlaying.value = false
                        _currentMessageId.value = null
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    scope.launch {
                        _isPlaying.value = false
                        _currentMessageId.value = null
                    }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    scope.launch {
                        Log.w(TAG, "TTS Utterance error: $errorCode for id: $utteranceId")
                        _isPlaying.value = false
                        _currentMessageId.value = null
                    }
                }
            })
            Log.d(TAG, "TextToSpeech initialisé avec succès")
        } else {
            isInitialized = false
            _isEngineAvailable.value = false
            Log.w(TAG, "Initialisation TextToSpeech échouée (status=$status)")
        }
    }

    private fun configureLocale(locale: Locale): Boolean {
        return try {
            val result = tts?.setLanguage(locale)
            result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        } catch (e: Exception) {
            Log.w(TAG, "Erreur configuration langue TTS: ${e.message}")
            false
        }
    }

    /**
     * Lit à voix haute le texte d'un message.
     * Si ce même message est déjà en cours de lecture, l'appel stoppe la lecture (toggle).
     */
    fun speak(
        messageId: String,
        text: String,
        languageCode: String? = "FR",
        onCompletion: (() -> Unit)? = null
    ) {
        // Si le message courant est déjà en cours de lecture, appuyer dessus stoppe la lecture
        if (_isPlaying.value && _currentMessageId.value == messageId) {
            stop()
            return
        }

        stop()

        val cleaned = sanitizeTextForSpeech(text)
        if (cleaned.isBlank()) return

        val targetLocale = when (languageCode?.uppercase()) {
            "EN" -> Locale.ENGLISH
            "ES" -> Locale("es", "ES")
            "AR" -> Locale("ar")
            "RU" -> Locale("ru", "RU")
            else -> Locale.FRENCH
        }

        _isPlaying.value = true
        _currentMessageId.value = messageId

        if (isInitialized && tts != null) {
            try {
                if (!configureLocale(targetLocale)) {
                    configureLocale(Locale.getDefault())
                }
                tts?.setSpeechRate(_speechRate.value)

                val params = Bundle().apply {
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                }

                val res = tts?.speak(
                    cleaned,
                    TextToSpeech.QUEUE_FLUSH,
                    params,
                    messageId
                )

                if (res != TextToSpeech.SUCCESS) {
                    Log.w(TAG, "tts.speak returned non-success code: $res")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during tts.speak: ${e.message}")
            }
        } else {
            Log.i(TAG, "TTS engine not ready yet, state marked active for id=$messageId")
        }
    }

    /**
     * Interrompt immédiatement toute synthèse vocale en cours.
     */
    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping TTS: ${e.message}")
        }
        _isPlaying.value = false
        _currentMessageId.value = null
    }

    /**
     * Modifie la vitesse de lecture (ex: 0.8f, 1.0f, 1.25f, 1.5f).
     */
    fun setSpeechRate(rate: Float) {
        val clamped = rate.coerceIn(0.5f, 2.5f)
        _speechRate.value = clamped
        try {
            tts?.setSpeechRate(clamped)
        } catch (e: Exception) {
            Log.w(TAG, "Error setting TTS speech rate: ${e.message}")
        }
    }

    /**
     * Alterne rapidement la vitesse de lecture : 1.0x -> 1.25x -> 1.5x -> 0.8x -> 1.0x.
     */
    fun cycleSpeechRate(): Float {
        val next = when (_speechRate.value) {
            1.0f -> 1.25f
            1.25f -> 1.5f
            1.5f -> 0.8f
            else -> 1.0f
        }
        setSpeechRate(next)
        return next
    }

    fun setTtsFeatureEnabled(enabled: Boolean) {
        _isTtsFeatureEnabled.value = enabled
        if (!enabled) {
            stop()
        }
    }

    /**
     * Détecte si un message textuel est considéré comme "long"
     * et éligible à la suggestion de lecture vocale TTS.
     */
    fun isLongMessage(text: String, mediaType: String? = null): Boolean {
        if (!_isTtsFeatureEnabled.value) return false
        if (mediaType != null && mediaType != "TEXT") return false
        val trimmed = text.trim()
        if (trimmed.length < DEFAULT_LONG_MESSAGE_THRESHOLD_CHARS) {
            val words = trimmed.split(Regex("""\s+""")).filter { it.isNotBlank() }
            if (words.size < DEFAULT_LONG_MESSAGE_THRESHOLD_WORDS && !trimmed.contains("\n")) {
                return false
            }
        }
        return true
    }

    /**
     * Estime la durée de lecture à voix haute en secondes.
     */
    fun estimateReadingDurationSeconds(text: String): Int {
        val words = text.trim().split(Regex("""\s+""")).filter { it.isNotBlank() }.size
        val rate = _speechRate.value.coerceAtLeast(0.5f)
        val wordsPerSecond = 2.4f * rate
        return (words / wordsPerSecond).roundToInt().coerceAtLeast(1)
    }

    /**
     * Nettoie le texte pour une élocution fluide :
     * retire les URL, les caractères répétitifs et simplifie les balises.
     */
    fun sanitizeTextForSpeech(raw: String): String {
        return raw
            // Remplacer les URLs
            .replace(Regex("""https?://\S+"""), "lien internet")
            // Remplacer les tirets ou puces
            .replace(Regex("""^[•\-\*]\s*""", RegexOption.MULTILINE), "")
            // Supprimer balises markdown
            .replace(Regex("""[\*_~`#]"""), "")
            // Remplacer sauts de ligne multiples
            .replace(Regex("""\n+"""), ". ")
            .trim()
    }

    /**
     * Libère les ressources du moteur de synthèse.
     */
    fun shutdown() {
        try {
            stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
            _isEngineAvailable.value = false
        } catch (e: Exception) {
            Log.w(TAG, "Error shutting down TTS: ${e.message}")
        }
    }
}
