package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Catégories de médias stockés dans les conversations ADT Pro.
 */
enum class StorageMediaType(val label: String) {
    PHOTO("Photos"),
    VIDEO("Vidéos"),
    AUDIO("Messages vocaux"),
    DOC("Documents"),
    OTHER("Autres")
}

/**
 * Représentation d'une pièce jointe stockée pour analyse et nettoyage.
 */
data class StorageMediaItem(
    val messageId: String,
    val chatId: String,
    val chatTitle: String,
    val senderName: String,
    val mediaType: String,
    val mediaUrl: String,
    val timestamp: Long,
    val sizeBytes: Long,
    val isLargeFile: Boolean = sizeBytes >= 2 * 1024 * 1024 // Fichier volumineux >= 2 Mo
) {
    val formattedSize: String
        get() = formatBytes(sizeBytes)

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.FRENCH)
            return sdf.format(Date(timestamp))
        }
}

/**
 * Consommation détaillée du stockage pour une discussion spécifique.
 */
data class ChatStorageConsumption(
    val chatId: String,
    val chatTitle: String,
    val avatarUrl: String,
    val totalSizeBytes: Long,
    val totalMediaCount: Int,
    val photosCount: Int,
    val photosSizeBytes: Long,
    val videosCount: Int,
    val videosSizeBytes: Long,
    val audioCount: Int,
    val audioSizeBytes: Long,
    val docsCount: Int,
    val docsSizeBytes: Long,
    val mediaItems: List<StorageMediaItem> = emptyList()
) {
    val formattedTotalSize: String
        get() = formatBytes(totalSizeBytes)
}

/**
 * Vue globale de la répartition du stockage local, médias et espace disque disponible.
 */
data class StorageBreakdownOverview(
    val totalAppSizeBytes: Long = 0L,
    val totalMediaSizeBytes: Long = 0L,
    val cacheSizeBytes: Long = 0L,
    val photosTotalSizeBytes: Long = 0L,
    val videosTotalSizeBytes: Long = 0L,
    val audioTotalSizeBytes: Long = 0L,
    val docsTotalSizeBytes: Long = 0L,
    val freeDeviceSpaceBytes: Long = 0L,
    val totalDeviceSpaceBytes: Long = 0L,
    val chatsStorage: List<ChatStorageConsumption> = emptyList(),
    val largeFiles: List<StorageMediaItem> = emptyList(),
    val isLoading: Boolean = false
) {
    val formattedTotalMedia: String get() = formatBytes(totalMediaSizeBytes)
    val formattedCacheSize: String get() = formatBytes(cacheSizeBytes)
    val formattedFreeSpace: String get() = formatBytes(freeDeviceSpaceBytes)
    val formattedTotalDevice: String get() = formatBytes(totalDeviceSpaceBytes)
}

/**
 * Formate un nombre d'octets en Ko, Mo ou Go.
 */
fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 Ko"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.2f Go", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f Mo", mb)
        else -> String.format(Locale.US, "%.0f Ko", kb)
    }
}
