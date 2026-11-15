package com.example.data.storage

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.model.ChatStorageConsumption
import com.example.data.model.StorageBreakdownOverview
import com.example.data.model.StorageMediaItem
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Gestionnaire du stockage local et cloud ADT Pro.
 * Analyse la consommation des médias (photos, vidéos, audio, docs) par discussion
 * et permet de purger les pièces jointes volumineuses pour libérer de l'espace.
 */
class StorageManager(
    private val context: Context,
    private val database: AppDatabase
) {
    private val TAG = "StorageManager"

    /**
     * Analyse l'ensemble des discussions et messages Room pour construire le bilan de stockage.
     */
    suspend fun computeStorageOverview(): StorageBreakdownOverview = withContext(Dispatchers.IO) {
        try {
            val allChats = database.chatDao().getAllChatsSync()
            val allMessages = database.messageDao().getAllMessagesSync()
            val chatMap = allChats.associateBy { it.id }

            val mediaMessages = allMessages.filter { msg ->
                val type = msg.mediaType.uppercase()
                (type == "PHOTO" || type == "VIDEO" || type == "AUDIO" || type == "DOC" || type == "DOCUMENT" || type == "IMAGE") &&
                        msg.mediaUrl.isNotBlank() &&
                        !msg.text.contains("[Pièce jointe supprimée")
            }

            var photosTotal = 0L
            var videosTotal = 0L
            var audioTotal = 0L
            var docsTotal = 0L

            val mediaItems = mediaMessages.map { msg ->
                val size = estimateOrGetMediaSize(msg)
                val type = msg.mediaType.uppercase()
                when (type) {
                    "PHOTO", "IMAGE" -> photosTotal += size
                    "VIDEO" -> videosTotal += size
                    "AUDIO" -> audioTotal += size
                    "DOC", "DOCUMENT" -> docsTotal += size
                    else -> docsTotal += size
                }

                val chatTitle = chatMap[msg.chatId]?.title ?: "Discussion"

                StorageMediaItem(
                    messageId = msg.id,
                    chatId = msg.chatId,
                    chatTitle = chatTitle,
                    senderName = msg.senderName.ifBlank { "Moi" },
                    mediaType = type,
                    mediaUrl = msg.mediaUrl,
                    timestamp = msg.timestamp,
                    sizeBytes = size
                )
            }

            val chatGrouped = mediaItems.groupBy { it.chatId }

            val chatsConsumption = allChats.mapNotNull { chat ->
                val items = chatGrouped[chat.id] ?: emptyList()
                val totalSize = items.sumOf { it.sizeBytes }

                // Even if no media, include chats that have history or sort them
                val photos = items.filter { it.mediaType == "PHOTO" || it.mediaType == "IMAGE" }
                val videos = items.filter { it.mediaType == "VIDEO" }
                val audios = items.filter { it.mediaType == "AUDIO" }
                val docs = items.filter { it.mediaType == "DOC" || it.mediaType == "DOCUMENT" }

                ChatStorageConsumption(
                    chatId = chat.id,
                    chatTitle = chat.title,
                    avatarUrl = chat.avatarUrl,
                    totalSizeBytes = totalSize,
                    totalMediaCount = items.size,
                    photosCount = photos.size,
                    photosSizeBytes = photos.sumOf { it.sizeBytes },
                    videosCount = videos.size,
                    videosSizeBytes = videos.sumOf { it.sizeBytes },
                    audioCount = audios.size,
                    audioSizeBytes = audios.sumOf { it.sizeBytes },
                    docsCount = docs.size,
                    docsSizeBytes = docs.sumOf { it.sizeBytes },
                    mediaItems = items.sortedByDescending { it.sizeBytes }
                )
            }.sortedByDescending { it.totalSizeBytes }

            val totalMediaBytes = photosTotal + videosTotal + audioTotal + docsTotal
            val cacheBytes = computeCacheSize()
            val freeSpace = context.filesDir.usableSpace
            val totalDevice = context.filesDir.totalSpace

            val largeFiles = mediaItems
                .filter { it.sizeBytes >= 1_500_000L } // >= 1.5 Mo
                .sortedByDescending { it.sizeBytes }

            StorageBreakdownOverview(
                totalAppSizeBytes = totalMediaBytes + cacheBytes,
                totalMediaSizeBytes = totalMediaBytes,
                cacheSizeBytes = cacheBytes,
                photosTotalSizeBytes = photosTotal,
                videosTotalSizeBytes = videosTotal,
                audioTotalSizeBytes = audioTotal,
                docsTotalSizeBytes = docsTotal,
                freeDeviceSpaceBytes = freeSpace,
                totalDeviceSpaceBytes = totalDevice,
                chatsStorage = chatsConsumption,
                largeFiles = largeFiles,
                isLoading = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error computing storage overview: ${e.message}", e)
            StorageBreakdownOverview(isLoading = false)
        }
    }

    /**
     * Supprime ou purge les pièces jointes sélectionnées.
     * Met à jour le message Room pour marquer la pièce jointe comme supprimée,
     * supprimant le fichier local ou distant tout en préservant le fil de discussion.
     */
    suspend fun deleteMediaAttachments(messageIds: Collection<String>): Long = withContext(Dispatchers.IO) {
        var freedBytes = 0L
        for (msgId in messageIds) {
            try {
                val msg = database.messageDao().getMessageById(msgId) ?: continue
                val estimated = estimateOrGetMediaSize(msg)
                freedBytes += estimated

                // 1. Tenter la suppression locale du fichier
                try {
                    if (msg.mediaUrl.startsWith("file://") || msg.mediaUrl.startsWith("/")) {
                        val path = msg.mediaUrl.removePrefix("file://")
                        val file = File(path)
                        if (file.exists()) {
                            file.delete()
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not delete local file for $msgId: ${e.message}")
                }

                // 2. Tenter la suppression Firebase Storage si URL cloud
                try {
                    if (msg.mediaUrl.contains("firebasestorage.googleapis.com")) {
                        val ref = FirebaseStorage.getInstance().getReferenceFromUrl(msg.mediaUrl)
                        ref.delete().await()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Firebase Storage delete optional notice: ${e.message}")
                }

                // 3. Mettre à jour l'entité Message dans Room
                val updated = msg.copy(
                    mediaType = "TEXT",
                    mediaUrl = "",
                    text = if (msg.text.isNotBlank()) "${msg.text} (📎 Pièce jointe supprimée pour libérer de l'espace)" else "📎 Pièce jointe supprimée (${msg.mediaType.lowercase()})"
                )
                database.messageDao().updateMessage(updated)
            } catch (e: Exception) {
                Log.e(TAG, "Failed deleting media $msgId: ${e.message}")
            }
        }
        freedBytes
    }

    /**
     * Purge l'ensemble des médias d'une discussion donnée.
     */
    suspend fun clearChatMedia(chatId: String, mediaTypes: Set<String> = emptySet()): Long = withContext(Dispatchers.IO) {
        val messages = database.messageDao().getMessagesForChatSync(chatId)
        val targets = messages.filter { msg ->
            val type = msg.mediaType.uppercase()
            (mediaTypes.isEmpty() || type in mediaTypes) && msg.mediaUrl.isNotBlank()
        }
        deleteMediaAttachments(targets.map { it.id })
    }

    /**
     * Vide le cache temporaire de l'application (images Coil, audios transitoires).
     */
    suspend fun clearAppCache(): Long = withContext(Dispatchers.IO) {
        var deletedBytes = 0L
        try {
            val cacheDir = context.cacheDir
            if (cacheDir.exists() && cacheDir.isDirectory) {
                cacheDir.listFiles()?.forEach { file ->
                    deletedBytes += file.length()
                    file.deleteRecursively()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed clearing app cache: ${e.message}")
        }
        deletedBytes
    }

    private fun computeCacheSize(): Long {
        return try {
            val cacheDir = context.cacheDir
            if (!cacheDir.exists()) 0L
            else cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
        } catch (e: Exception) {
            0L
        }
    }

    private fun estimateOrGetMediaSize(msg: MessageEntity): Long {
        try {
            if (msg.mediaUrl.startsWith("file://") || msg.mediaUrl.startsWith("/")) {
                val f = File(msg.mediaUrl.removePrefix("file://"))
                if (f.exists() && f.length() > 0) {
                    return f.length()
                }
            }
        } catch (_: Exception) {}

        // Estimation pondérée réaliste basée sur la durée ou le type
        val type = msg.mediaType.uppercase()
        val duration = if (msg.mediaDuration > 0) msg.mediaDuration else 15
        return when (type) {
            "PHOTO", "IMAGE" -> {
                val hashMod = Math.abs(msg.id.hashCode() % 1_200_000L)
                800_000L + hashMod // ~800 Ko à 2.0 Mo
            }
            "VIDEO" -> {
                val sec = duration.coerceIn(5, 120)
                sec * 450_000L // ~450 Ko/s => 20s = 9 Mo
            }
            "AUDIO" -> {
                val sec = duration.coerceIn(3, 90)
                sec * 24_000L // ~24 Ko/s AAC/Opus => 20s = 480 Ko
            }
            "DOC", "DOCUMENT" -> {
                val hashMod = Math.abs(msg.id.hashCode() % 3_500_000L)
                1_200_000L + hashMod // ~1.2 Mo à 4.7 Mo
            }
            else -> 400_000L
        }
    }
}
