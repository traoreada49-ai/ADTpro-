package com.example.data.firebase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.util.Log
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File

data class StickerData(
    val id: String,
    val emoji: String,
    val title: String,
    val category: String,
    val tags: List<String> = emptyList(),
    val imageUrl: String? = null,
    val storagePath: String? = null,
    val isCloudAsset: Boolean = false,
    val createdAt: Long = 0L
)

sealed interface StickerSyncStatus {
    data object Idle : StickerSyncStatus
    data object Syncing : StickerSyncStatus
    data class Uploading(val progressPercent: Int) : StickerSyncStatus
    data class Success(val message: String) : StickerSyncStatus
    data class Error(val message: String) : StickerSyncStatus
}

/**
 * Manages custom sticker assets with category organization and Firebase Storage integration.
 * Uploads generated or photo-picked PNG sticker assets to `stickers/custom/{userScope}/{category}/`
 * with custom metadata and synchronizes remote Firebase Storage collections.
 */
class FirebaseStickerManager(private val context: Context) {

    private val tag = "FirebaseStickerMgr"
    private val prefs = context.getSharedPreferences("adt_cloud_stickers_prefs", Context.MODE_PRIVATE)
    private val localCacheDir: File by lazy {
        File(context.filesDir, "stickers_cache").apply { if (!exists()) mkdirs() }
    }

    private val _cloudStickers = MutableStateFlow<List<StickerData>>(loadCachedStickers())
    val cloudStickers: StateFlow<List<StickerData>> = _cloudStickers.asStateFlow()

    private val _syncStatus = MutableStateFlow<StickerSyncStatus>(StickerSyncStatus.Idle)
    val syncStatus: StateFlow<StickerSyncStatus> = _syncStatus.asStateFlow()

    suspend fun syncCloudStickers(userScope: String) = withContext(Dispatchers.IO) {
        val storage = FirebaseInitHelper.getStorage(context)
        if (storage == null) {
            _syncStatus.value = StickerSyncStatus.Error(
                "Firebase Storage indisponible. Vérifiez la configuration du bucket."
            )
            return@withContext
        }

        _syncStatus.value = StickerSyncStatus.Syncing
        try {
            val safeScope = sanitizeScope(userScope)
            val discovered = mutableListOf<StickerData>()

            // Scan user's custom sticker folders + shared public sticker packs on Firebase Storage
            val rootRefs = listOf(
                storage.reference.child("stickers/custom/$safeScope"),
                storage.reference.child("stickers/public")
            )

            for (rootRef in rootRefs) {
                collectStickersRecursively(rootRef, discovered)
            }

            if (discovered.isNotEmpty()) {
                val merged = (discovered + _cloudStickers.value.filter { cached ->
                    discovered.none { it.id == cached.id }
                }).sortedByDescending { it.createdAt }
                _cloudStickers.value = merged
                saveCachedStickers(merged)
            }

            _syncStatus.value = StickerSyncStatus.Success(
                if (discovered.isNotEmpty()) {
                    "${discovered.size} sticker(s) synchronisé(s) depuis Firebase Storage"
                } else {
                    "Bibliothèque cloud connectée (aucun sticker distant pour l'instant)"
                }
            )
        } catch (e: Exception) {
            Log.w(tag, "Sync Firebase Storage stickers note: ${e.message}", e)
            _syncStatus.value = StickerSyncStatus.Error(
                "Synchronisation Firebase Storage impossible : ${e.localizedMessage ?: "vérifiez les règles Storage"}"
            )
        }
    }

    private suspend fun collectStickersRecursively(
        reference: StorageReference,
        output: MutableList<StickerData>
    ) {
        try {
            val listResult = reference.listAll().await()
            for (item in listResult.items) {
                try {
                    val meta = item.metadata.await()
                    val url = item.downloadUrl.await().toString()
                    val title = meta.getCustomMetadata("title").orEmpty()
                        .ifBlank { item.name.substringBeforeLast(".").replace("_", " ") }
                    val category = meta.getCustomMetadata("category").orEmpty().ifBlank { "CLOUD" }
                    val emoji = meta.getCustomMetadata("emoji").orEmpty().ifBlank { "🎨" }
                    val createdAt = meta.creationTimeMillis

                    output.add(
                        StickerData(
                            id = item.name.substringBeforeLast("."),
                            emoji = emoji,
                            title = title,
                            category = category,
                            tags = listOf(title.lowercase(), category.lowercase(), "cloud"),
                            imageUrl = url,
                            storagePath = item.path,
                            isCloudAsset = true,
                            createdAt = createdAt
                        )
                    )
                } catch (itemEx: Exception) {
                    Log.w(tag, "Skipping item ${item.path}: ${itemEx.message}")
                }
            }
            for (prefix in listResult.prefixes) {
                collectStickersRecursively(prefix, output)
            }
        } catch (e: Exception) {
            // Empty or non-existent directory on Firebase Storage is expected before first upload
            Log.d(tag, "Prefix ${reference.path} note: ${e.message}")
        }
    }

    suspend fun createAndUploadCustomSticker(
        userScope: String,
        emoji: String,
        title: String,
        category: String,
        pickedImageUri: Uri? = null
    ): StickerData = withContext(Dispatchers.IO) {
        val cleanEmoji = emoji.trim().ifBlank { "🌟" }
        val cleanTitle = title.trim().ifBlank { "Sticker ADT" }
        val cleanCategory = category.trim().uppercase().ifBlank { "CUSTOM" }
        val stickerId = "stk_${System.currentTimeMillis()}"
        val safeScope = sanitizeScope(userScope)

        _syncStatus.value = StickerSyncStatus.Uploading(10)

        val pngBytes = if (pickedImageUri != null) {
            decodeAndScaleUriToPng(pickedImageUri) ?: renderStyledStickerPng(cleanEmoji, cleanTitle, cleanCategory)
        } else {
            renderStyledStickerPng(cleanEmoji, cleanTitle, cleanCategory)
        }

        // Write a local cache copy so the asset renders smoothly even offline
        val localFile = File(localCacheDir, "$stickerId.png").apply {
            writeBytes(pngBytes)
        }
        val localUriString = Uri.fromFile(localFile).toString()

        val storage = FirebaseInitHelper.getStorage(context)
        if (storage == null) {
            val localOnly = StickerData(
                id = stickerId,
                emoji = cleanEmoji,
                title = cleanTitle,
                category = cleanCategory,
                tags = listOf(cleanTitle.lowercase(), cleanCategory.lowercase()),
                imageUrl = localUriString,
                storagePath = null,
                isCloudAsset = false,
                createdAt = System.currentTimeMillis()
            )
            addStickerToCache(localOnly)
            _syncStatus.value = StickerSyncStatus.Error(
                "Firebase Storage indisponible : sticker enregistré localement uniquement."
            )
            return@withContext localOnly
        }

        val storagePath = "stickers/custom/$safeScope/$cleanCategory/$stickerId.png"
        val ref = storage.reference.child(storagePath)

        return@withContext try {
            val metadata = StorageMetadata.Builder()
                .setContentType("image/png")
                .setCustomMetadata("title", cleanTitle)
                .setCustomMetadata("category", cleanCategory)
                .setCustomMetadata("emoji", cleanEmoji)
                .build()

            val uploadTask = ref.putBytes(pngBytes, metadata)
            uploadTask.addOnProgressListener { snap ->
                val total = snap.totalByteCount.coerceAtLeast(1L)
                val pct = ((snap.bytesTransferred * 100) / total).toInt().coerceIn(15, 95)
                _syncStatus.value = StickerSyncStatus.Uploading(pct)
            }
            uploadTask.await()

            val downloadUrl = ref.downloadUrl.await().toString()
            val cloudSticker = StickerData(
                id = stickerId,
                emoji = cleanEmoji,
                title = cleanTitle,
                category = cleanCategory,
                tags = listOf(cleanTitle.lowercase(), cleanCategory.lowercase(), "cloud", "firebase"),
                imageUrl = downloadUrl,
                storagePath = storagePath,
                isCloudAsset = true,
                createdAt = System.currentTimeMillis()
            )
            addStickerToCache(cloudSticker)
            _syncStatus.value = StickerSyncStatus.Success(
                "Sticker « $cleanTitle » publié sur Firebase Storage"
            )
            cloudSticker
        } catch (e: Exception) {
            Log.e(tag, "Firebase Storage upload failed: ${e.message}", e)
            val fallbackSticker = StickerData(
                id = stickerId,
                emoji = cleanEmoji,
                title = cleanTitle,
                category = cleanCategory,
                tags = listOf(cleanTitle.lowercase(), cleanCategory.lowercase()),
                imageUrl = localUriString,
                storagePath = storagePath,
                isCloudAsset = false,
                createdAt = System.currentTimeMillis()
            )
            addStickerToCache(fallbackSticker)
            _syncStatus.value = StickerSyncStatus.Error(
                "Envoi Firebase Storage échoué (${e.localizedMessage ?: "vérifiez les règles Storage"}). Copie locale conservée."
            )
            fallbackSticker
        }
    }

    suspend fun deleteCustomSticker(sticker: StickerData) = withContext(Dispatchers.IO) {
        val storage = FirebaseInitHelper.getStorage(context)
        if (storage != null && !sticker.storagePath.isNullOrBlank()) {
            try {
                storage.reference.child(sticker.storagePath).delete().await()
            } catch (e: Exception) {
                Log.w(tag, "Firebase Storage delete note: ${e.message}")
            }
        }
        val localFile = File(localCacheDir, "${sticker.id}.png")
        if (localFile.exists()) {
            localFile.delete()
        }
        val updated = _cloudStickers.value.filterNot { it.id == sticker.id }
        _cloudStickers.value = updated
        saveCachedStickers(updated)
        _syncStatus.value = StickerSyncStatus.Success("Sticker supprimé de la bibliothèque")
    }

    fun clearStatus() {
        _syncStatus.value = StickerSyncStatus.Idle
    }

    private fun decodeAndScaleUriToPng(uri: Uri): ByteArray? {
        return try {
            val rawBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return null

            val maxDim = 512f
            val scale = minOf(maxDim / rawBitmap.width, maxDim / rawBitmap.height, 1f)
            val targetW = (rawBitmap.width * scale).toInt().coerceAtLeast(64)
            val targetH = (rawBitmap.height * scale).toInt().coerceAtLeast(64)
            val scaled = Bitmap.createScaledBitmap(rawBitmap, targetW, targetH, true)

            val output = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.PNG, 95, output)
            if (scaled !== rawBitmap) rawBitmap.recycle()
            scaled.recycle()
            output.toByteArray()
        } catch (e: Exception) {
            Log.w(tag, "Error decoding picked image: ${e.message}")
            null
        }
    }

    /**
     * Renders a crisp 512x512 PNG sticker badge combining gradient art, emoji graphic, and label pill.
     */
    private fun renderStyledStickerPng(emoji: String, title: String, category: String): ByteArray {
        val size = 512
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val (startColor, endColor) = when (category) {
            "TERANGA" -> Pair(Color.parseColor("#00897B"), Color.parseColor("#FFB300"))
            "TECH" -> Pair(Color.parseColor("#1565C0"), Color.parseColor("#00ACC1"))
            "REACTIONS" -> Pair(Color.parseColor("#D81B60"), Color.parseColor("#FB8C00"))
            else -> Pair(Color.parseColor("#00A884"), Color.parseColor("#075E54"))
        }

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                startColor, endColor, Shader.TileMode.CLAMP
            )
        }
        val cardRect = RectF(24f, 24f, size - 24f, size - 24f)
        canvas.drawRoundRect(cardRect, 84f, 84f, bgPaint)

        // Subtle inner highlight border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = Color.argb(90, 255, 255, 255)
        }
        canvas.drawRoundRect(cardRect, 84f, 84f, borderPaint)

        // Center Emoji
        val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 200f
            textAlign = Paint.Align.CENTER
            color = Color.WHITE
        }
        canvas.drawText(emoji, size / 2f, size * 0.56f, emojiPaint)

        // Bottom caption pill
        val pillRect = RectF(52f, size - 122f, size - 52f, size - 44f)
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(230, 255, 255, 255)
        }
        canvas.drawRoundRect(pillRect, 36f, 36f, pillPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 36f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.parseColor("#111B21")
        }
        val clippedTitle = if (title.length > 20) title.take(19) + "…" else title
        canvas.drawText(clippedTitle, size / 2f, size - 70f, textPaint)

        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        bitmap.recycle()
        return out.toByteArray()
    }

    private fun sanitizeScope(raw: String): String {
        val cleaned = raw.replace(Regex("[^a-zA-Z0-9_-]"), "_").trim('_')
        return cleaned.ifBlank { "default_user" }
    }

    private fun addStickerToCache(sticker: StickerData) {
        val updated = listOf(sticker) + _cloudStickers.value.filterNot { it.id == sticker.id }
        _cloudStickers.value = updated
        saveCachedStickers(updated)
    }

    private fun saveCachedStickers(list: List<StickerData>) {
        val jsonArray = JSONArray()
        list.forEach { s ->
            val obj = JSONObject().apply {
                put("id", s.id)
                put("emoji", s.emoji)
                put("title", s.title)
                put("category", s.category)
                put("imageUrl", s.imageUrl ?: "")
                put("storagePath", s.storagePath ?: "")
                put("isCloudAsset", s.isCloudAsset)
                put("createdAt", s.createdAt)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("stickers_json", jsonArray.toString()).apply()
    }

    private fun loadCachedStickers(): List<StickerData> {
        val raw = prefs.getString("stickers_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val result = mutableListOf<StickerData>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                result.add(
                    StickerData(
                        id = obj.optString("id"),
                        emoji = obj.optString("emoji", "🌟"),
                        title = obj.optString("title", "Sticker"),
                        category = obj.optString("category", "CUSTOM"),
                        tags = listOf(obj.optString("title", "").lowercase(), obj.optString("category", "").lowercase()),
                        imageUrl = obj.optString("imageUrl").takeIf { it.isNotBlank() },
                        storagePath = obj.optString("storagePath").takeIf { it.isNotBlank() },
                        isCloudAsset = obj.optBoolean("isCloudAsset", false),
                        createdAt = obj.optLong("createdAt", 0L)
                    )
                )
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }
}
