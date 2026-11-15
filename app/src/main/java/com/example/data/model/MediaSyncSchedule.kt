package com.example.data.model

import java.util.Locale

/**
 * Préréglages recommandés pour les plages horaires de synchronisation des médias.
 */
enum class SchedulePreset(
    val title: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val icon: String,
    val description: String
) {
    NIGHT_CHARGE(
        title = "Nuit & Charge 🌙",
        startHour = 23,
        startMinute = 0,
        endHour = 7,
        endMinute = 0,
        icon = "🌙",
        description = "23h00 à 07h00 : Idéal pendant la charge nocturne sur table de chevet."
    ),
    OFF_PEAK(
        title = "Heures creuses ⚡",
        startHour = 1,
        startMinute = 0,
        endHour = 6,
        endMinute = 0,
        icon = "⚡",
        description = "01h00 à 06h00 : Optimise le réseau et l'énergie aux heures creuses."
    ),
    LUNCH_REST(
        title = "Recharge midi ☀️",
        startHour = 12,
        startMinute = 30,
        endHour = 14,
        endMinute = 0,
        icon = "☀️",
        description = "12h30 à 14h00 : Pendant la recharge rapide au bureau ou à la pause."
    ),
    CUSTOM(
        title = "Personnalisé ⚙️",
        startHour = 22,
        startMinute = 0,
        endHour = 6,
        endMinute = 30,
        icon = "⚙️",
        description = "Plage horaire configurée manuellement selon vos habitudes."
    )
}

/**
 * Configuration de la plage horaire spécifique de synchronisation complète des médias
 * pour privilégier les périodes où l'appareil est en charge.
 */
data class MediaSyncSchedule(
    val enabled: Boolean = true,
    val startHour: Int = 23,
    val startMinute: Int = 0,
    val endHour: Int = 7,
    val endMinute: Int = 0,
    val requireCharging: Boolean = true,
    val requireWifi: Boolean = true,
    val syncPhotos: Boolean = true,
    val syncVideos: Boolean = true,
    val syncDocs: Boolean = true,
    val syncAudio: Boolean = true,
    val preset: SchedulePreset = SchedulePreset.NIGHT_CHARGE
) {
    /**
     * Vérifie si l'heure donnée (ou actuelle) se trouve dans la plage horaire programmée.
     * Gère correctement les plages chevauchant minuit (ex: 23h00 à 07h00).
     */
    fun isCurrentlyInWindow(currentHour: Int, currentMinute: Int): Boolean {
        if (!enabled) return false

        val currentTotalMinutes = currentHour * 60 + currentMinute
        val startTotalMinutes = startHour * 60 + startMinute
        val endTotalMinutes = endHour * 60 + endMinute

        return if (startTotalMinutes <= endTotalMinutes) {
            // Plage dans la même journée (ex: 12h30 à 14h00)
            currentTotalMinutes in startTotalMinutes until endTotalMinutes
        } else {
            // Plage traversant minuit (ex: 23h00 à 07h00)
            currentTotalMinutes >= startTotalMinutes || currentTotalMinutes < endTotalMinutes
        }
    }

    /**
     * Détermine si toutes les conditions de synchronisation complète sont réunies :
     * 1. Option activée
     * 2. Heure actuelle dans la plage
     * 3. Appareil en charge (si exigé)
     * 4. Réseau Wi-Fi disponible (si exigé)
     */
    fun isFullSyncAuthorized(
        currentHour: Int,
        currentMinute: Int,
        isDeviceCharging: Boolean,
        isWifi: Boolean
    ): Boolean {
        if (!enabled) return false
        if (!isCurrentlyInWindow(currentHour, currentMinute)) return false
        if (requireCharging && !isDeviceCharging) return false
        if (requireWifi && !isWifi) return false
        return true
    }

    /**
     * Formatage lisible de la plage horaire (ex: "23:00 – 07:00").
     */
    fun getFormattedWindow(): String {
        return String.format(Locale.getDefault(), "%02d:%02d – %02d:%02d", startHour, startMinute, endHour, endMinute)
    }

    /**
     * Calcule la durée totale de la plage horaire en heures et minutes.
     */
    fun getDurationSummary(): String {
        val startTotal = startHour * 60 + startMinute
        val endTotal = endHour * 60 + endMinute
        val diffMinutes = if (endTotal >= startTotal) {
            endTotal - startTotal
        } else {
            (24 * 60 - startTotal) + endTotal
        }
        val h = diffMinutes / 60
        val m = diffMinutes % 60
        return if (m == 0) "${h}h" else "${h}h${String.format(Locale.getDefault(), "%02d", m)}"
    }

    /**
     * Statut textuel détaillé de la plage horaire.
     */
    fun getStatusDescription(
        currentHour: Int,
        currentMinute: Int,
        isDeviceCharging: Boolean,
        isWifi: Boolean
    ): String {
        if (!enabled) {
            return "Plage désactivée (Téléchargements régis par les règles standards)"
        }
        val inWindow = isCurrentlyInWindow(currentHour, currentMinute)
        return when {
            inWindow && (!requireCharging || isDeviceCharging) && (!requireWifi || isWifi) -> {
                "Plage active : Synchronisation complète en cours ⚡🔌"
            }
            inWindow && requireCharging && !isDeviceCharging -> {
                "Dans la plage horaire (${getFormattedWindow()}) : Branchez le chargeur pour lancer la synchronisation 🔌"
            }
            inWindow && requireWifi && !isWifi -> {
                "Dans la plage horaire (${getFormattedWindow()}) : En attente du Wi-Fi 📶"
            }
            else -> {
                val chargeCondition = if (requireCharging) " (sur chargeur)" else ""
                "Prochaine synchronisation complète à ${String.format(Locale.getDefault(), "%02d:%02d", startHour, startMinute)}$chargeCondition"
            }
        }
    }
}
