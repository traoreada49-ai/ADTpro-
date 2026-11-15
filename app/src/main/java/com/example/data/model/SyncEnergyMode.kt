package com.example.data.model

/**
 * Représente le mode de synchronisation et de gestion énergétique de l'application ADT Pro.
 * Permet de basculer entre une performance maximale en temps réel et une économie d'énergie prolongée.
 */
enum class SyncEnergyMode(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val syncIntervalMinutes: Int,
    val autoDownloadMedia: Boolean,
    val estimatedBatteryGain: String,
    val networkWakeReductionPercent: Int
) {
    HIGH_PERFORMANCE(
        id = "HIGH_PERFORMANCE",
        title = "Haute Performance",
        subtitle = "Synchronisation temps réel instantanée, streaming direct sans latence et téléchargement média immédiat.",
        icon = "⚡",
        syncIntervalMinutes = 0,
        autoDownloadMedia = true,
        estimatedBatteryGain = "Latence minimale (0s)",
        networkWakeReductionPercent = 0
    ),
    BATTERY_SAVER(
        id = "BATTERY_SAVER",
        title = "Économie d'Énergie",
        subtitle = "Cycles réseau espacés de 15 min, téléchargement média au clic et réduction drastique des wake locks.",
        icon = "🔋",
        syncIntervalMinutes = 15,
        autoDownloadMedia = false,
        estimatedBatteryGain = "+2h à +4h d'autonomie",
        networkWakeReductionPercent = 60
    );

    companion object {
        fun fromId(id: String?): SyncEnergyMode {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: HIGH_PERFORMANCE
        }
    }

    val label: String get() = title
    val estimatedSavings: String get() = estimatedBatteryGain
}
