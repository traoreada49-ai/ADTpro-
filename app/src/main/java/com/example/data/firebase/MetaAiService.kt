package com.example.data.firebase

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.example.data.model.MetaAiMessage
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Service gérant l'assistant conversationnel Meta AI Direct via Firebase AI Logic.
 */
object MetaAiService {

    private const val TAG = "MetaAiService"
    private const val CHAT_MODEL_NAME = "gemini-3.8-flash"
    private const val IMAGE_MODEL_NAME = "gemini-nano-banana-2.1"

    private const val SYSTEM_PROMPT = """
Tu es l'assistant d'intelligence artificielle intégré à ADT Pro, propulsé par Gemini via Firebase AI Logic.
Tu es polyvalent, bienveillant, précis, concis et dynamique.
Tu maîtrises parfaitement le français, l'anglais, le bambara, le wolof, l'arabe et les langues du Sahel.
Tes réponses doivent être claires, bien présentées avec des puces et des emojis appropriés.
Si l'utilisateur demande des conseils professionnels, technologiques, d'écriture, de voyage ou de vie quotidienne, donne des réponses concrètes et inspirantes.
"""

    private val chatModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = CHAT_MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.7f
                maxOutputTokens = 1024
            },
            systemInstruction = content { text(SYSTEM_PROMPT) }
        )
    }

    private val imageModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = IMAGE_MODEL_NAME,
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    /**
     * Diffuse la réponse en streaming pour réduire le Time-to-First-Token.
     */
    fun generateResponseStream(
        prompt: String,
        history: List<MetaAiMessage> = emptyList()
    ): Flow<String> = flow {
        val trimmed = prompt.trim()
        val chatHistory = history
            .filter { it.text.isNotBlank() && !it.isImageGeneration }
            .takeLast(8)
            .map { msg ->
                content(role = if (msg.isUser) "user" else "model") {
                    text(msg.text)
                }
            }

        val chat = chatModel.startChat(history = chatHistory)
        chat.sendMessageStream(trimmed).collect { chunk ->
            val piece = chunk.text.orEmpty()
            if (piece.isNotEmpty()) {
                emit(piece)
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Génère une image via gemini-nano-banana-2.1 pour la commande /imagine.
     */
    suspend fun generateImagineImage(
        query: String,
        outputDir: File
    ): MetaAiMessage = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim().ifBlank {
            "Un paysage futuriste lumineux au coucher du soleil"
        }

        val bitmap: Bitmap = try {
            withTimeout(60_000L) {
                val response = imageModel.generateContent(cleanQuery)
                val imagePart = response.candidates.firstOrNull()
                    ?.content?.parts?.filterIsInstance<ImagePart>()
                    ?.firstOrNull()
                imagePart?.image
                    ?: throw IllegalStateException("Aucune image n'a été retournée par le modèle.")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI image generation failed", e)
            throw e
        }

        val file = File(outputDir, "imagine_${UUID.randomUUID()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        }

        MetaAiMessage(
            text = "🎨 **/imagine** *$cleanQuery*",
            isUser = false,
            isImageGeneration = true,
            imageUrl = Uri.fromFile(file).toString(),
            imagePrompt = cleanQuery,
            suggestedPrompts = listOf(
                "/imagine style cyberpunk néon",
                "/imagine version aquarelle et coucher de soleil",
                "/imagine vue aérienne cinématique"
            )
        )
    }

    fun generateFollowUpSuggestions(query: String): List<String> {
        val lower = query.lowercase()
        return when {
            lower.contains("business") || lower.contains("projet") -> listOf(
                "Comment rédiger un business plan ?",
                "Quelles stratégies marketing adopter ?",
                "/imagine affiche publicitaire moderne"
            )
            lower.contains("bambara") || lower.contains("mali") -> listOf(
                "Donne-moi 5 proverbes bambara",
                "Comment saluer poliment les aînés ?",
                "/imagine paysage au coucher du soleil à Ségou"
            )
            else -> listOf(
                "Peux-tu m'expliquer plus en détail ?",
                "Donne-moi un exemple concret",
                "/imagine illustration futuriste"
            )
        }
    }
}
