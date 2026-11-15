package com.example.data.export

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class ExportFormat(
    val label: String,
    val extension: String,
    val mimeType: String
) {
    JSON("JSON", "json", "application/json"),
    PDF("PDF", "pdf", "application/pdf")
}

data class ConversationExportResult(
    val filePath: String,
    val fileName: String,
    val format: ExportFormat,
    val fileSizeBytes: Long,
    val conversationsCount: Int,
    val messagesCount: Int,
    val exportedAtMillis: Long,
    val scopeLabel: String
) {
    val formattedSize: String
        get() = formatFileSize(fileSizeBytes)
}

data class LocalExportFileInfo(
    val filePath: String,
    val fileName: String,
    val format: ExportFormat,
    val fileSizeBytes: Long,
    val lastModifiedMillis: Long
) {
    val formattedSize: String
        get() = formatFileSize(fileSizeBytes)
}

sealed interface ConversationExportUiState {
    data object Idle : ConversationExportUiState
    data class Exporting(val format: ExportFormat) : ConversationExportUiState
    data class Success(val result: ConversationExportResult) : ConversationExportUiState
    data class Error(val message: String) : ConversationExportUiState
}

internal fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes o"
        bytes < 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f Ko", bytes / 1024.0)
        else -> String.format(Locale.getDefault(), "%.2f Mo", bytes / (1024.0 * 1024.0))
    }
}

object ConversationExportManager {

    private const val TAG = "ConversationExportMgr"
    private const val EXPORTS_DIR_NAME = "exports"

    fun getExportsDirectory(context: Context): File {
        val dir = File(context.filesDir, EXPORTS_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun listLocalExports(context: Context): List<LocalExportFileInfo> {
        val dir = getExportsDirectory(context)
        val files = dir.listFiles()?.filter { it.isFile } ?: return emptyList()
        return files.mapNotNull { file ->
            val fmt = when (file.extension.lowercase(Locale.ROOT)) {
                "json" -> ExportFormat.JSON
                "pdf" -> ExportFormat.PDF
                else -> return@mapNotNull null
            }
            LocalExportFileInfo(
                filePath = file.absolutePath,
                fileName = file.name,
                format = fmt,
                fileSizeBytes = file.length(),
                lastModifiedMillis = file.lastModified()
            )
        }.sortedByDescending { it.lastModifiedMillis }
    }

    suspend fun exportConversations(
        context: Context,
        format: ExportFormat,
        user: UserEntity?,
        chats: List<ChatEntity>,
        messagesByChat: Map<String, List<MessageEntity>>,
        scopeLabel: String
    ): ConversationExportResult = withContext(Dispatchers.IO) {
        val exportedAt = System.currentTimeMillis()
        val fileTimestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(exportedAt))
        val scopeSlug = if (chats.size == 1) {
            chats.first().title
                .lowercase(Locale.ROOT)
                .replace(Regex("[^a-z0-9]+"), "_")
                .trim('_')
                .take(24)
                .ifBlank { "conversation" }
        } else {
            "toutes_conversations"
        }

        val fileName = "adt_pro_${scopeSlug}_$fileTimestamp.${format.extension}"
        val exportsDir = getExportsDirectory(context)
        val outputFile = File(exportsDir, fileName)

        val totalMessages = chats.sumOf { chat -> messagesByChat[chat.id]?.size ?: 0 }

        when (format) {
            ExportFormat.JSON -> {
                val jsonContent = buildJsonPayload(
                    exportedAt = exportedAt,
                    user = user,
                    chats = chats,
                    messagesByChat = messagesByChat,
                    scopeLabel = scopeLabel
                )
                outputFile.writeText(jsonContent, Charsets.UTF_8)
            }

            ExportFormat.PDF -> {
                writePdfDocument(
                    outputFile = outputFile,
                    exportedAt = exportedAt,
                    user = user,
                    chats = chats,
                    messagesByChat = messagesByChat,
                    scopeLabel = scopeLabel
                )
            }
        }

        Log.i(TAG, "Exported ${chats.size} chats ($totalMessages msgs) to ${outputFile.absolutePath}")

        ConversationExportResult(
            filePath = outputFile.absolutePath,
            fileName = outputFile.name,
            format = format,
            fileSizeBytes = outputFile.length(),
            conversationsCount = chats.size,
            messagesCount = totalMessages,
            exportedAtMillis = exportedAt,
            scopeLabel = scopeLabel
        )
    }

    private fun buildJsonPayload(
        exportedAt: Long,
        user: UserEntity?,
        chats: List<ChatEntity>,
        messagesByChat: Map<String, List<MessageEntity>>,
        scopeLabel: String
    ): String {
        val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val readableFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

        val ownerName = buildString {
            val first = user?.firstName?.trim().orEmpty()
            val last = user?.lastName?.trim().orEmpty()
            append("$first $last".trim().ifBlank { "Utilisateur ADT Pro" })
        }

        val conversationsArray = JSONArray()
        var totalMessages = 0

        for (chat in chats) {
            val chatMessages = messagesByChat[chat.id].orEmpty().sortedBy { it.timestamp }
            totalMessages += chatMessages.size

            val messagesArray = JSONArray()
            for (msg in chatMessages) {
                val msgObj = JSONObject().apply {
                    put("id", msg.id)
                    put("senderName", msg.senderName.ifBlank { if (msg.isOutgoing) ownerName else chat.title })
                    put("senderPhone", msg.senderPhone)
                    put("isOutgoing", msg.isOutgoing)
                    put("timestamp", msg.timestamp)
                    put("timestampFormatted", readableFormatter.format(Date(msg.timestamp)))
                    put("mediaType", msg.mediaType)
                    if (msg.mediaUrl.isNotBlank()) put("mediaUrl", msg.mediaUrl)
                    if (msg.mediaDuration > 0) put("mediaDurationSeconds", msg.mediaDuration)
                    put("text", msg.text)
                    if (!msg.replyToSender.isNullOrBlank()) put("replyToSender", msg.replyToSender)
                    if (!msg.replyToText.isNullOrBlank()) put("replyToText", msg.replyToText)
                    if (!msg.reaction.isNullOrBlank()) put("reaction", msg.reaction)
                    put("isForwarded", msg.isForwarded)
                    put("status", msg.status)
                }
                messagesArray.put(msgObj)
            }

            val chatObj = JSONObject().apply {
                put("chatId", chat.id)
                put("title", chat.title)
                put("type", if (chat.isGroup) "GROUP" else "DIRECT")
                put("isArchived", chat.isArchived)
                put("isLocked", chat.isLocked)
                put("membersCount", chat.membersCount)
                put("lastTimestamp", chat.lastTimestamp)
                put("lastTimestampFormatted", readableFormatter.format(Date(chat.lastTimestamp)))
                put("messagesCount", chatMessages.size)
                put("messages", messagesArray)
            }
            conversationsArray.put(chatObj)
        }

        val root = JSONObject().apply {
            put("exportVersion", "1.0")
            put("app", "ADT Pro")
            put("scope", scopeLabel)
            put("exportedAtEpochMs", exportedAt)
            put("exportedAtIso", isoFormatter.format(Date(exportedAt)))
            put("exportedAtFormatted", readableFormatter.format(Date(exportedAt)))
            put("exportedBy", JSONObject().apply {
                put("name", ownerName)
                put("phone", user?.phone.orEmpty())
            })
            put("totalConversations", chats.size)
            put("totalMessages", totalMessages)
            put("conversations", conversationsArray)
        }

        return root.toString(2)
    }

    private fun writePdfDocument(
        outputFile: File,
        exportedAt: Long,
        user: UserEntity?,
        chats: List<ChatEntity>,
        messagesByChat: Map<String, List<MessageEntity>>,
        scopeLabel: String
    ) {
        val pageWidth = 595
        val pageHeight = 842
        val marginHorizontal = 36f
        val marginTop = 36f
        val marginBottom = 42f
        val contentWidth = (pageWidth - marginHorizontal * 2).toInt()

        val dateFormatter = SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.getDefault())
        val msgDateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        val ownerName = buildString {
            val first = user?.firstName?.trim().orEmpty()
            val last = user?.lastName?.trim().orEmpty()
            append("$first $last".trim().ifBlank { "Utilisateur ADT Pro" })
        }
        val ownerPhone = user?.phone?.takeIf { it.isNotBlank() } ?: "Non renseigné"
        val totalMessages = chats.sumOf { messagesByChat[it.id]?.size ?: 0 }

        val headerBgPaint = Paint().apply { color = Color.rgb(7, 94, 84) }
        val sectionBgPaint = Paint().apply { color = Color.rgb(236, 245, 243) }
        val outgoingBgPaint = Paint().apply { color = Color.rgb(241, 248, 245) }
        val incomingBgPaint = Paint().apply { color = Color.rgb(247, 248, 250) }
        val dividerPaint = Paint().apply {
            color = Color.rgb(218, 224, 228)
            strokeWidth = 0.8f
        }

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(220, 245, 238)
            textSize = 9.5f
        }
        val sectionTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(7, 94, 84)
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val sectionMetaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(85, 95, 102)
            textSize = 9f
        }
        val senderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(20, 30, 36)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val timestampPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(110, 120, 128)
            textSize = 8.5f
        }
        val mediaTagPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0, 128, 105)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val replyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(95, 105, 112)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(33, 37, 41)
            textSize = 9.5f
        }
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(130, 138, 145)
            textSize = 8f
        }

        val document = PdfDocument()
        try {
            var pageNumber = 1
            var currentPage = document.startPage(
                PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            )
            var canvas: Canvas = currentPage.canvas
            var cursorY = marginTop

            fun drawFooter(targetCanvas: Canvas, number: Int) {
                targetCanvas.drawLine(
                    marginHorizontal,
                    pageHeight - 28f,
                    pageWidth - marginHorizontal,
                    pageHeight - 28f,
                    dividerPaint
                )
                targetCanvas.drawText(
                    "ADT Pro • Sauvegarde locale chiffrée • Exporté le ${dateFormatter.format(Date(exportedAt))}",
                    marginHorizontal,
                    pageHeight - 16f,
                    footerPaint
                )
                val pageLabel = "Page $number"
                val labelWidth = footerPaint.measureText(pageLabel)
                targetCanvas.drawText(
                    pageLabel,
                    pageWidth - marginHorizontal - labelWidth,
                    pageHeight - 16f,
                    footerPaint
                )
            }

            fun ensureSpace(requiredHeight: Float) {
                if (cursorY + requiredHeight > pageHeight - marginBottom) {
                    drawFooter(canvas, pageNumber)
                    document.finishPage(currentPage)
                    pageNumber++
                    currentPage = document.startPage(
                        PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    )
                    canvas = currentPage.canvas
                    cursorY = marginTop
                }
            }

            // First-page Header Banner
            val bannerHeight = 76f
            canvas.drawRoundRect(
                marginHorizontal,
                cursorY,
                pageWidth - marginHorizontal,
                cursorY + bannerHeight,
                8f,
                8f,
                headerBgPaint
            )
            canvas.drawText(
                "ADT Pro — Export des conversations",
                marginHorizontal + 14f,
                cursorY + 24f,
                titlePaint
            )
            canvas.drawText(
                "Compte : $ownerName ($ownerPhone) • Portée : $scopeLabel",
                marginHorizontal + 14f,
                cursorY + 43f,
                subtitlePaint
            )
            canvas.drawText(
                "Généré le ${dateFormatter.format(Date(exportedAt))} • ${chats.size} conversation(s) • $totalMessages message(s)",
                marginHorizontal + 14f,
                cursorY + 59f,
                subtitlePaint
            )
            cursorY += bannerHeight + 16f

            if (chats.isEmpty()) {
                canvas.drawText(
                    "Aucune conversation disponible pour cette exportation.",
                    marginHorizontal,
                    cursorY + 14f,
                    bodyPaint
                )
            } else {
                for (chat in chats) {
                    val chatMessages = messagesByChat[chat.id].orEmpty().sortedBy { it.timestamp }
                    val chatKind = buildString {
                        append(if (chat.isGroup) "Groupe (${chat.membersCount} membres)" else "Discussion directe")
                        if (chat.isArchived) append(" • Archivée")
                    }

                    ensureSpace(52f)

                    // Conversation Section Card
                    val sectionHeight = 34f
                    canvas.drawRoundRect(
                        marginHorizontal,
                        cursorY,
                        pageWidth - marginHorizontal,
                        cursorY + sectionHeight,
                        6f,
                        6f,
                        sectionBgPaint
                    )
                    canvas.drawText(
                        chat.title.ifBlank { "Conversation sans titre" },
                        marginHorizontal + 10f,
                        cursorY + 14f,
                        sectionTitlePaint
                    )
                    canvas.drawText(
                        "$chatKind • ${chatMessages.size} message(s)",
                        marginHorizontal + 10f,
                        cursorY + 27f,
                        sectionMetaPaint
                    )
                    cursorY += sectionHeight + 8f

                    if (chatMessages.isEmpty()) {
                        ensureSpace(24f)
                        canvas.drawText(
                            "Aucun message dans cette conversation.",
                            marginHorizontal + 8f,
                            cursorY + 12f,
                            sectionMetaPaint
                        )
                        cursorY += 22f
                    } else {
                        for (msg in chatMessages) {
                            val sender = if (msg.isOutgoing) {
                                "Moi ($ownerName)"
                            } else {
                                msg.senderName.ifBlank { chat.title }
                            }
                            val timeStr = msgDateFormatter.format(Date(msg.timestamp))
                            val mediaLabel = formatMediaLabel(msg)
                            val bodyContent = buildString {
                                append(msg.text.ifBlank { if (mediaLabel != null) mediaLabel else "(Message vide)" })
                                if (!msg.reaction.isNullOrBlank()) {
                                    append("  [Réaction : ${msg.reaction}]")
                                }
                            }

                            val textLayout = StaticLayout.Builder
                                .obtain(bodyContent, 0, bodyContent.length, bodyPaint, contentWidth - 16)
                                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                                .setLineSpacing(2f, 1.0f)
                                .build()

                            val hasReply = !msg.replyToText.isNullOrBlank()
                            val extraHeader = (if (mediaLabel != null && msg.text.isNotBlank()) 13f else 0f) +
                                (if (hasReply) 13f else 0f)
                            val blockHeight = 20f + extraHeader + textLayout.height + 10f

                            ensureSpace(blockHeight + 4f)

                            val bgPaint = if (msg.isOutgoing) outgoingBgPaint else incomingBgPaint
                            canvas.drawRoundRect(
                                marginHorizontal,
                                cursorY,
                                pageWidth - marginHorizontal,
                                cursorY + blockHeight,
                                5f,
                                5f,
                                bgPaint
                            )

                            var innerY = cursorY + 13f
                            canvas.drawText(sender, marginHorizontal + 8f, innerY, senderPaint)
                            val timeWidth = timestampPaint.measureText(timeStr)
                            canvas.drawText(
                                timeStr,
                                pageWidth - marginHorizontal - 8f - timeWidth,
                                innerY,
                                timestampPaint
                            )
                            innerY += 6f

                            if (hasReply) {
                                innerY += 10f
                                val replyAuthor = msg.replyToSender ?: "Message"
                                val replyPreview = msg.replyToText.orEmpty().replace("\n", " ").take(75)
                                canvas.drawText(
                                    "↩ $replyAuthor : $replyPreview",
                                    marginHorizontal + 8f,
                                    innerY,
                                    replyPaint
                                )
                                innerY += 3f
                            }

                            if (mediaLabel != null && msg.text.isNotBlank()) {
                                innerY += 10f
                                canvas.drawText(mediaLabel, marginHorizontal + 8f, innerY, mediaTagPaint)
                                innerY += 3f
                            }

                            innerY += 3f
                            canvas.save()
                            canvas.translate(marginHorizontal + 8f, innerY)
                            textLayout.draw(canvas)
                            canvas.restore()

                            cursorY += blockHeight + 5f
                        }
                    }

                    cursorY += 10f
                }
            }

            drawFooter(canvas, pageNumber)
            document.finishPage(currentPage)

            FileOutputStream(outputFile).use { fos ->
                document.writeTo(fos)
            }
        } finally {
            document.close()
        }
    }

    private fun formatMediaLabel(msg: MessageEntity): String? {
        return when (msg.mediaType.uppercase(Locale.ROOT)) {
            "TEXT" -> null
            "PHOTO" -> "[Pièce jointe : Photo]"
            "AUDIO" -> if (msg.mediaDuration > 0) "[Message vocal : ${msg.mediaDuration}s]" else "[Message vocal]"
            "VIDEO" -> if (msg.mediaDuration > 0) "[Vidéo : ${msg.mediaDuration}s]" else "[Vidéo]"
            "STICKER" -> "[Autocollant]"
            "DOCUMENT", "DOC" -> "[Document]"
            "LOCATION" -> "[Localisation]"
            "LIVE_LOCATION" -> "[Position en direct]"
            else -> "[Média : ${msg.mediaType}]"
        }
    }

    suspend fun copyFileToUri(
        context: Context,
        sourceFilePath: String,
        targetUri: Uri
    ): Long = withContext(Dispatchers.IO) {
        val sourceFile = File(sourceFilePath)
        if (!sourceFile.exists()) {
            throw IllegalStateException("Fichier d'export introuvable localement.")
        }
        var totalBytes = 0L
        context.contentResolver.openOutputStream(targetUri)?.use { output ->
            FileInputStream(sourceFile).use { input ->
                totalBytes = input.copyTo(output)
            }
        } ?: throw IllegalStateException("Impossible d'écrire vers la destination sélectionnée.")
        totalBytes
    }

    fun deleteLocalExport(filePath: String): Boolean {
        val file = File(filePath)
        return if (file.exists()) file.delete() else false
    }

    fun shareExportFile(context: Context, filePath: String, format: ExportFormat): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists()) return false
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = format.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Exportation conversations ADT Pro (${format.label})")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Partager l'export ${format.label}")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share export file", e)
            false
        }
    }

    fun openExportFile(context: Context, filePath: String, format: ExportFormat): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists()) return false
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, format.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.w(TAG, "No viewer app available for ${format.mimeType}: ${e.message}")
            false
        }
    }
}
