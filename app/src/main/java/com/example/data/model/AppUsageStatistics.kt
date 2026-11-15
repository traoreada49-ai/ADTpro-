package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Période d'analyse sélectionnée pour les statistiques d'utilisation.
 */
enum class UsagePeriod(val label: String) {
    DAY_7("7 derniers jours"),
    DAY_30("30 derniers jours"),
    ALL_TIME("Global")
}

/**
 * Données journalières d'utilisation (temps d'écran, messages envoyés/reçus, données consommées).
 */
data class DailyUsageStat(
    val dateKey: String, // Format YYYY-MM-DD
    val dayLabel: String, // ex: "Lun", "Mar", "Mer"
    val fullDateLabel: String, // ex: "11 Nov"
    val screenTimeMinutes: Int, // Temps passé en minutes
    val messagesSent: Int,
    val messagesReceived: Int,
    val dataSentBytes: Long,
    val dataReceivedBytes: Long
) {
    val totalMessages: Int get() = messagesSent + messagesReceived
    val totalDataBytes: Long get() = dataSentBytes + dataReceivedBytes
    val formattedScreenTime: String
        get() {
            val h = screenTimeMinutes / 60
            val m = screenTimeMinutes % 60
            return if (h > 0) "${h}h ${m}m" else "${m} min"
        }
    val formattedData: String get() = formatBytes(totalDataBytes)
}

/**
 * Répartition détaillée de la consommation réseau pour un type de média spécifique (messages, photos, vidéos, appels).
 */
data class MediaNetworkUsageDetail(
    val mediaTypeTitle: String, // "Messages (Texte & Stickers)", "Photos", "Vidéos", "Appels (Audio & Vidéo)", "Documents"
    val iconKey: String, // "MESSAGES", "PHOTOS", "VIDEOS", "CALLS", "DOCS"
    val sentBytes: Long,
    val receivedBytes: Long,
    val totalBytes: Long = sentBytes + receivedBytes,
    val percentageOfTotal: Float, // 0.0 to 100.0
    val itemsCount: Int, // Nombre d'éléments (messages, fichiers ou appels)
    val colorHex: Long,
    val dataSaverAdvice: String // Conseil pour réduire l'impact sur le forfait data
) {
    val formattedTotalBytes: String get() = formatBytes(totalBytes)
    val formattedSentBytes: String get() = formatBytes(sentBytes)
    val formattedReceivedBytes: String get() = formatBytes(receivedBytes)
}

/**
 * Répartition de la consommation de données par type de trafic (pour graphiques Recharts / D3-style).
 */
data class DataUsageCategory(
    val category: String, // "Messages texte", "Photos", "Vidéos", "Appels", "Documents"
    val bytes: Long,
    val percentage: Float, // 0.0 to 100.0
    val colorHex: Long // Couleur pour l'affichage graphique
) {
    val formattedBytes: String get() = formatBytes(bytes)
}

/**
 * Statistiques complètes d'utilisation de l'application ADT Pro.
 */
data class AppUsageStatistics(
    val period: UsagePeriod = UsagePeriod.DAY_7,
    val totalScreenTimeMinutes: Int = 0,
    val averageDailyScreenTimeMinutes: Int = 0,
    val totalMessagesSent: Int = 0,
    val totalMessagesReceived: Int = 0,
    val totalDataSentBytes: Long = 0L,
    val totalDataReceivedBytes: Long = 0L,
    val dailyStats: List<DailyUsageStat> = emptyList(),
    val dataCategories: List<DataUsageCategory> = emptyList(),
    val mediaNetworkDetails: List<MediaNetworkUsageDetail> = emptyList(),
    val peakActivityHour: Int = 14, // Heure la plus active (0-23)
    val mostActiveChatTitle: String = "Général",
    val mostActiveChatMessages: Int = 0,
    val callsDurationMinutes: Int = 0,
    val callsCount: Int = 0,
    val mobileDataSavedBytes: Long = 0L // Estimation des Mo économisés grâce à l'économiseur de données
) {
    val totalMessages: Int get() = totalMessagesSent + totalMessagesReceived
    val totalDataBytes: Long get() = totalDataSentBytes + totalDataReceivedBytes

    val formattedTotalScreenTime: String
        get() {
            val h = totalScreenTimeMinutes / 60
            val m = totalScreenTimeMinutes % 60
            return if (h > 0) "${h}h ${m}m" else "${m} min"
        }

    val formattedAverageDailyScreenTime: String
        get() {
            val h = averageDailyScreenTimeMinutes / 60
            val m = averageDailyScreenTimeMinutes % 60
            return if (h > 0) "${h}h ${m}m" else "${m} min"
        }

    val formattedTotalData: String get() = formatBytes(totalDataBytes)
    val formattedDataSent: String get() = formatBytes(totalDataSentBytes)
    val formattedDataReceived: String get() = formatBytes(totalDataReceivedBytes)
    val formattedDataSaved: String get() = formatBytes(mobileDataSavedBytes)
}
