package com.example.data.model

/**
 * Durées disponibles pour les messages éphémères (autodestruction des messages).
 * Permet de définir des intervalles de 24h, 7 jours ou 90 jours pour l'autodestruction des messages.
 */
enum class EphemeralMessageDuration(
    val code: String,
    val label: String,
    val shortLabel: String,
    val subtitle: String,
    val durationMs: Long,
    val icon: String
) {
    OFF(
        code = "OFF",
        label = "Désactivé",
        shortLabel = "Off",
        subtitle = "Conserver tous les messages indéfiniment",
        durationMs = 0L,
        icon = "♾️"
    ),
    HOURS_24(
        code = "24H",
        label = "24 heures",
        shortLabel = "24h",
        subtitle = "Autodestruction après 24 heures (Confidentialité maximale)",
        durationMs = 24L * 3600L * 1000L,
        icon = "⚡"
    ),
    DAYS_7(
        code = "7D",
        label = "7 jours",
        shortLabel = "7 jours",
        subtitle = "Autodestruction après 1 semaine (Équilibre)",
        durationMs = 7L * 24L * 3600L * 1000L,
        icon = "📅"
    ),
    DAYS_90(
        code = "90D",
        label = "90 jours",
        shortLabel = "90 jours",
        subtitle = "Autodestruction après 3 mois (Archivage temporaire)",
        durationMs = 90L * 24L * 3600L * 1000L,
        icon = "🗄️"
    );

    companion object {
        fun fromCode(code: String?): EphemeralMessageDuration {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: OFF
        }
    }
}
