package com.example.data.firebase

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

/**
 * Service gérant les suggestions de réponses rapides contextuelles basées sur le dernier message reçu,
 * utilisant Firebase AI Logic (modèle gemini-3.8-flash) et une sortie JSON structurée via responseSchema.
 */
object FirebaseAiQuickReplyService {

    private const val TAG = "FirebaseAiQuickReply"
    private const val MODEL_NAME = "gemini-3.8-flash"

    private val quickRepliesSchema = Schema.obj(
        properties = mapOf(
            "suggestions" to Schema.array(
                items = Schema.string(description = "Short contextual quick reply (1 to 6 words)"),
                description = "Exactly 3 short contextual quick replies"
            )
        )
    )

    private val model: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = quickRepliesSchema
                temperature = 0.7f
                maxOutputTokens = 256
            }
        )
    }

    /**
     * Génère 3 suggestions de réponses rapides pour le dernier message reçu via Firebase AI Logic.
     */
    suspend fun generateQuickReplies(
        lastMessage: String,
        senderName: String = "Contact",
        chatTitle: String = "Discussion",
        language: String = "FR",
        tone: String = "FRIENDLY"
    ): List<String> = withContext(Dispatchers.IO) {
        if (lastMessage.isBlank()) return@withContext emptyList()

        val prompt = buildPrompt(
            lastMessage = lastMessage,
            senderName = senderName,
            chatTitle = chatTitle,
            language = language,
            tone = tone
        )

        try {
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val jsonString = response.text
                ?: throw IllegalStateException("Le modèle a retourné une réponse vide.")

            val json = JSONObject(jsonString)
            val array = json.getJSONArray("suggestions")
            val results = (0 until array.length())
                .mapNotNull { idx -> array.optString(idx)?.trim()?.takeIf { it.isNotBlank() } }
                .take(3)

            if (results.isEmpty()) {
                throw IllegalStateException("Aucune suggestion valide générée par le modèle.")
            }

            Log.d(TAG, "Quick replies generated via Firebase AI Logic: $results")
            results
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI quick reply generation failed", e)
            throw e
        }
    }

    private fun buildPrompt(
        lastMessage: String,
        senderName: String,
        chatTitle: String,
        language: String,
        tone: String
    ): String {
        val langInstruction = when (language.uppercase()) {
            "EN" -> "in English"
            "ES" -> "en español"
            "AR" -> "باللغة العربية"
            "RU" -> "на русском"
            "FRANGLAIS" -> "en franglais urbain moderne et amical"
            else -> "en français moderne, naturel et bienveillant"
        }

        val toneInstruction = when (tone.uppercase()) {
            "PROFESSIONAL" -> "Ton de réponse : professionnel, courtois, respectueux et direct."
            "SHORT" -> "Ton de réponse : ultra-court et synthétique (1 à 4 mots maximum par suggestion)."
            else -> "Ton de réponse : chaleureux, naturel, amical et poli."
        }

        return """
            Tu es l'assistant de messagerie instantanée ADT Pro propulsé par Firebase AI.
            Analyse le dernier message reçu et propose exactement 3 suggestions de réponses rapides, courtes et naturelles (1 à 6 mots chacune), $langInstruction.

            $toneInstruction

            Expéditeur: $senderName
            Conversation: $chatTitle
            Dernier message reçu: "$lastMessage"
        """.trimIndent()
    }
}
