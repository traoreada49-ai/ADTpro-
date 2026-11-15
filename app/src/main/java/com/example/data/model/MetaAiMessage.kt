package com.example.data.model

/**
 * Modèle de données pour les messages de la conversation Meta AI Direct.
 */
data class MetaAiMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isImageGeneration: Boolean = false,
    val imageUrl: String? = null,
    val imagePrompt: String? = null,
    val isThinking: Boolean = false,
    val suggestedPrompts: List<String> = emptyList()
)
