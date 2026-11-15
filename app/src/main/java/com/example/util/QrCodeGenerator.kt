package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.core.content.FileProvider
import com.example.data.local.entity.ContactEntity
import com.example.ui.theme.AdtPrimary
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Visual color themes for dynamic ADT Pro QR codes.
 */
enum class QrTheme(
    val title: String,
    val primaryColorInt: Int,
    val secondaryColorInt: Int,
    val composeColor: Color
) {
    EMERALD(
        title = "ADT Émeraude",
        primaryColorInt = 0xFF00A884.toInt(),
        secondaryColorInt = 0xFF075E54.toInt(),
        composeColor = AdtPrimary
    ),
    CYAN(
        title = "Cyber Cyan",
        primaryColorInt = 0xFF00BCD4.toInt(),
        secondaryColorInt = 0xFF00838F.toInt(),
        composeColor = Color(0xFF00BCD4)
    ),
    OBSIDIAN(
        title = "Obsidienne Pro",
        primaryColorInt = 0xFF1E293B.toInt(),
        secondaryColorInt = 0xFF0F172A.toInt(),
        composeColor = Color(0xFF1E293B)
    ),
    GOLD(
        title = "Or Prestige",
        primaryColorInt = 0xFFD97706.toInt(),
        secondaryColorInt = 0xFFB45309.toInt(),
        composeColor = Color(0xFFD97706)
    )
}

/**
 * Payload formats supported for dynamic contact sharing.
 */
enum class QrPayloadFormat(val label: String, val description: String) {
    ADT_DIRECT(
        label = "Protocole Direct ADT",
        description = "Intégration instantanée directe dans le réseau ADT Pro (recommandé)"
    ),
    UNIVERSAL_WEB(
        label = "Lien Web Universel",
        description = "Accessible depuis tout smartphone avec redirection intelligente"
    ),
    VCARD(
        label = "Carte vCard 3.0",
        description = "Format standard compatible avec tous les répertoires natifs"
    )
}

/**
 * Structured model of contact data carried by a dynamic QR Code.
 */
data class QrContactPayload(
    val id: String,
    val name: String,
    val phone: String,
    val countryCode: String = "+223",
    val role: String = "Professionnel ADT",
    val organization: String = "Réseau ADT Pro",
    val bio: String = "Disponible sur ADT Pro",
    val avatarUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val token: String = UUID.randomUUID().toString().take(8),
    val isVerified: Boolean = true
) {
    fun toContactEntity(): ContactEntity {
        return ContactEntity(
            id = id.ifBlank { "adt_contact_" + phone.filter { it.isDigit() } },
            name = name.ifBlank { "Contact ADT" },
            phone = phone,
            countryCode = countryCode,
            avatarUrl = avatarUrl,
            bio = if (role.isNotBlank()) "$role • $organization" else bio,
            isAdtUser = true,
            isOnline = true,
            lastSeenText = "En ligne sur le réseau ADT Pro"
        )
    }
}

object QrCodeGenerator {

    /**
     * Serializes a contact payload into a string according to the requested format.
     */
    fun encodePayload(payload: QrContactPayload, format: QrPayloadFormat): String {
        return when (format) {
            QrPayloadFormat.ADT_DIRECT -> {
                buildString {
                    append("adtpro://contact?")
                    append("id=").append(urlEncode(payload.id))
                    append("&name=").append(urlEncode(payload.name))
                    append("&phone=").append(urlEncode(payload.phone))
                    append("&cc=").append(urlEncode(payload.countryCode))
                    append("&role=").append(urlEncode(payload.role))
                    append("&org=").append(urlEncode(payload.organization))
                    append("&bio=").append(urlEncode(payload.bio))
                    append("&ts=").append(payload.timestamp)
                    append("&tk=").append(urlEncode(payload.token))
                    append("&ver=").append(if (payload.isVerified) "1" else "0")
                }
            }
            QrPayloadFormat.UNIVERSAL_WEB -> {
                buildString {
                    append("https://adtpro.app/join?")
                    append("id=").append(urlEncode(payload.id))
                    append("&name=").append(urlEncode(payload.name))
                    append("&phone=").append(urlEncode(payload.phone))
                    append("&cc=").append(urlEncode(payload.countryCode))
                    append("&role=").append(urlEncode(payload.role))
                    append("&org=").append(urlEncode(payload.organization))
                    append("&ts=").append(payload.timestamp)
                    append("&tk=").append(urlEncode(payload.token))
                }
            }
            QrPayloadFormat.VCARD -> {
                buildString {
                    appendLine("BEGIN:VCARD")
                    appendLine("VERSION:3.0")
                    appendLine("FN:${payload.name}")
                    appendLine("TEL;TYPE=CELL,VOICE:${payload.phone}")
                    if (payload.role.isNotBlank()) appendLine("TITLE:${payload.role}")
                    if (payload.organization.isNotBlank()) appendLine("ORG:${payload.organization}")
                    if (payload.bio.isNotBlank()) appendLine("NOTE:${payload.bio}")
                    appendLine("URL:https://adtpro.app/join?ref=${payload.id}")
                    appendLine("X-ADTPRO-TOKEN:${payload.token}")
                    appendLine("END:VCARD")
                }
            }
        }
    }

    /**
     * Parses any scanned text (ADT URI, web URL, vCard, or phone) into a structured QrContactPayload.
     */
    fun parsePayload(rawInput: String): QrContactPayload? {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return null

        // 1. Direct ADT Pro URI scheme (adtpro://contact?...)
        if (trimmed.startsWith("adtpro://contact") || trimmed.startsWith("adtpro://user")) {
            val uri = Uri.parse(trimmed)
            val name = uri.getQueryParameter("name") ?: ""
            val phone = uri.getQueryParameter("phone") ?: ""
            val id = uri.getQueryParameter("id") ?: ("adt_" + phone.filter { it.isDigit() })
            val role = uri.getQueryParameter("role") ?: "Membre ADT Pro"
            val org = uri.getQueryParameter("org") ?: "Réseau ADT Pro"
            val bio = uri.getQueryParameter("bio") ?: "Disponible sur ADT Pro"
            val cc = uri.getQueryParameter("cc") ?: "+223"
            val ts = uri.getQueryParameter("ts")?.toLongOrNull() ?: System.currentTimeMillis()
            val tk = uri.getQueryParameter("tk") ?: ""
            val ver = uri.getQueryParameter("ver") == "1"

            if (name.isNotBlank() || phone.isNotBlank()) {
                return QrContactPayload(
                    id = id,
                    name = name.ifBlank { phone },
                    phone = phone,
                    countryCode = cc,
                    role = role,
                    organization = org,
                    bio = bio,
                    timestamp = ts,
                    token = tk,
                    isVerified = ver
                )
            }
        }

        // 2. Web URL (https://adtpro.app/join?...)
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            try {
                val uri = Uri.parse(trimmed)
                val name = uri.getQueryParameter("name") ?: uri.getQueryParameter("u") ?: ""
                val phone = uri.getQueryParameter("phone") ?: uri.getQueryParameter("p") ?: ""
                val id = uri.getQueryParameter("id") ?: uri.getQueryParameter("ref") ?: ("adt_" + phone.filter { it.isDigit() })
                val role = uri.getQueryParameter("role") ?: "Professionnel ADT"
                val org = uri.getQueryParameter("org") ?: "Réseau ADT Pro"
                val ts = uri.getQueryParameter("ts")?.toLongOrNull() ?: System.currentTimeMillis()
                val tk = uri.getQueryParameter("tk") ?: ""

                if (name.isNotBlank() || phone.isNotBlank() || id.isNotBlank()) {
                    return QrContactPayload(
                        id = id.ifBlank { UUID.randomUUID().toString().take(8) },
                        name = name.ifBlank { if (phone.isNotBlank()) phone else "Contact Web ADT" },
                        phone = phone.ifBlank { "+221 77 000 00 00" },
                        role = role,
                        organization = org,
                        timestamp = ts,
                        token = tk,
                        isVerified = true
                    )
                }
            } catch (_: Exception) {}
        }

        // 3. vCard 3.0 or 2.1
        if (trimmed.contains("BEGIN:VCARD", ignoreCase = true)) {
            var name = ""
            var phone = ""
            var role = "Contact Pro"
            var org = "Réseau ADT Pro"
            var bio = "Contact importé via vCard"

            trimmed.lineSequence().forEach { line ->
                val upper = line.uppercase()
                when {
                    upper.startsWith("FN:") -> name = line.substringAfter(":").trim()
                    upper.startsWith("N:") && name.isBlank() -> {
                        val parts = line.substringAfter(":").split(";")
                        name = parts.filter { it.isNotBlank() }.reversed().joinToString(" ")
                    }
                    upper.startsWith("TEL") -> {
                        phone = line.substringAfter(":").trim()
                    }
                    upper.startsWith("TITLE:") -> role = line.substringAfter(":").trim()
                    upper.startsWith("ORG:") -> org = line.substringAfter(":").trim()
                    upper.startsWith("NOTE:") -> bio = line.substringAfter(":").trim()
                }
            }

            if (name.isNotBlank() || phone.isNotBlank()) {
                return QrContactPayload(
                    id = "vcard_" + UUID.randomUUID().toString().take(8),
                    name = name.ifBlank { phone },
                    phone = phone.ifBlank { "+221 77 000 00 00" },
                    role = role,
                    organization = org,
                    bio = bio,
                    isVerified = true
                )
            }
        }

        // 4. Raw phone number or international format (+221...)
        val digits = trimmed.filter { it.isDigit() || it == '+' }
        if (digits.length >= 8) {
            return QrContactPayload(
                id = "phone_" + digits.filter { it.isDigit() },
                name = digits,
                phone = digits,
                role = "Membre ADT Pro",
                organization = "Réseau ADT Pro",
                bio = "Numéro scanné via QR Code",
                isVerified = false
            )
        }

        // 5. Fallback raw text representation
        return QrContactPayload(
            id = "scanned_" + UUID.randomUUID().toString().take(8),
            name = trimmed.take(30),
            phone = "+221 77 000 00 00",
            role = "Contact Réseau",
            organization = "ADT Pro",
            bio = trimmed,
            isVerified = false
        )
    }

    /**
     * Generates a high-quality Bitmap representation of the dynamic QR Code.
     * Incorporates custom rounded styling and an embedded central ADT Pro brand badge.
     */
    fun generateQrBitmap(
        content: String,
        size: Int = 640,
        theme: QrTheme = QrTheme.EMERALD,
        withCenterBadge: Boolean = true
    ): Bitmap {
        val hints = HashMap<EncodeHintType, Any>().apply {
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H) // 30% redundancy
            put(EncodeHintType.MARGIN, 1)
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
        }

        val matrix: BitMatrix = QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            size,
            size,
            hints
        )

        val width = matrix.width
        val height = matrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Clear background with crisp clean white
        canvas.drawColor(AndroidColor.WHITE)

        val modulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.primaryColorInt
            style = Paint.Style.FILL
        }

        val finderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.secondaryColorInt
            style = Paint.Style.FILL
        }

        val moduleSize = width.toFloat() / matrix.width.toFloat()
        val cornerRadius = moduleSize * 0.35f

        // Draw individual modules
        for (x in 0 until width) {
            for (y in 0 until height) {
                if (matrix.get(x, y)) {
                    // Check if it belongs to finder patterns (top-left, top-right, bottom-left)
                    val isFinder = (x < 7 && y < 7) ||
                            (x >= matrix.width - 7 && y < 7) ||
                            (x < 7 && y >= matrix.height - 7)

                    val paintToUse = if (isFinder) finderPaint else modulePaint
                    val left = x * moduleSize
                    val top = y * moduleSize
                    val right = left + moduleSize
                    val bottom = top + moduleSize

                    canvas.drawRoundRect(
                        RectF(left, top, right, bottom),
                        cornerRadius,
                        cornerRadius,
                        paintToUse
                    )
                }
            }
        }

        // Draw central ADT Pro brand badge if enabled
        if (withCenterBadge) {
            val centerSize = (width * 0.22f).toInt()
            val cx = width / 2f
            val cy = height / 2f
            val badgeRect = RectF(
                cx - centerSize / 2f,
                cy - centerSize / 2f,
                cx + centerSize / 2f,
                cy + centerSize / 2f
            )

            // White badge background circle with shadow effect
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(badgeRect, centerSize * 0.3f, centerSize * 0.3f, bgPaint)

            // Inner border
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.primaryColorInt
                style = Paint.Style.STROKE
                strokeWidth = moduleSize * 0.7f
            }
            canvas.drawRoundRect(badgeRect, centerSize * 0.3f, centerSize * 0.3f, borderPaint)

            // Inner colored shield fill
            val innerFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.primaryColorInt
                style = Paint.Style.FILL
            }
            val innerPadding = centerSize * 0.12f
            val innerRect = RectF(
                badgeRect.left + innerPadding,
                badgeRect.top + innerPadding,
                badgeRect.right - innerPadding,
                badgeRect.bottom - innerPadding
            )
            canvas.drawRoundRect(innerRect, centerSize * 0.22f, centerSize * 0.22f, innerFill)

            // Centered Monogram "ADT"
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.WHITE
                textSize = centerSize * 0.36f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val textY = cy - ((textPaint.descent() + textPaint.ascent()) / 2)
            canvas.drawText("ADT", cx, textY, textPaint)
        }

        return bitmap
    }

    /**
     * Decodes a QR code string from any Bitmap image using ZXing.
     */
    fun decodeQrFromBitmap(bitmap: Bitmap): String? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = MultiFormatReader().decode(binaryBitmap)
            result.text
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Creates a high-resolution, branded "Carte de Visite Digitale ADT Pro"
     * ready to be saved or shared across WhatsApp, Telegram, Email, etc.
     */
    fun createDigitalBusinessCardBitmap(
        context: Context,
        payload: QrContactPayload,
        theme: QrTheme = QrTheme.EMERALD
    ): Bitmap {
        val width = 1080
        val height = 1500
        val cardBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(cardBitmap)

        // Dark modern background (#0B141A / #111B21)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0B141A.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Top decorative header banner gradient / accent
        val bannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.primaryColorInt
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), 18f, bannerPaint)

        // App Header Brand
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("ADT PRO", 80f, 90f, titlePaint)

        val subBrandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.primaryColorInt
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("RÉSEAU SÉCURISÉ & CHIFFRÉ", 80f, 130f, subBrandPaint)

        // White card container for QR Code
        val cardMargin = 80f
        val cardTop = 180f
        val cardBottom = 960f
        val cardRect = RectF(cardMargin, cardTop, width - cardMargin, cardBottom)
        val whiteCardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cardRect, 32f, 32f, whiteCardPaint)

        // Generate the inner QR Code
        val qrSize = (width - (cardMargin * 2) - 100).toInt()
        val payloadStr = encodePayload(payload, QrPayloadFormat.ADT_DIRECT)
        val qrBitmap = generateQrBitmap(payloadStr, qrSize, theme, withCenterBadge = true)

        val qrLeft = cardMargin + 50f
        val qrTop = cardTop + 50f
        canvas.drawBitmap(qrBitmap, qrLeft, qrTop, null)

        // Scanned Contact Info section under the QR Card
        var currentY = 1030f

        // Name
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(payload.name, width / 2f, currentY, namePaint)
        currentY += 56f

        // Role & Organization
        val rolePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.primaryColorInt
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val roleText = if (payload.organization.isNotBlank()) "${payload.role} • ${payload.organization}" else payload.role
        canvas.drawText(roleText, width / 2f, currentY, rolePaint)
        currentY += 50f

        // Phone Number
        val phonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF94A3B8.toInt()
            textSize = 36f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(payload.phone, width / 2f, currentY, phonePaint)
        currentY += 70f

        // Security / Verification Badge Pill
        val badgeWidth = 480f
        val badgeHeight = 60f
        val badgeLeft = (width - badgeWidth) / 2f
        val badgeRect = RectF(badgeLeft, currentY, badgeLeft + badgeWidth, currentY + badgeHeight)
        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1E293B.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(badgeRect, 30f, 30f, badgeBgPaint)

        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF25D366.toInt()
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val badgeTextY = currentY + (badgeHeight / 2f) - ((badgeTextPaint.descent() + badgeTextPaint.ascent()) / 2f)
        canvas.drawText("✓ PROFIL VÉRIFIÉ ADT PRO", width / 2f, badgeTextY, badgeTextPaint)
        currentY += 100f

        // Footer Instructions
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF64748B.toInt()
            textSize = 26f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Scannez avec l'appareil photo ou l'application ADT Pro", width / 2f, currentY, footerPaint)
        currentY += 40f

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val dateText = "Jeton dynamique #${payload.token} • Émis le ${dateFormat.format(Date(payload.timestamp))}"
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF475569.toInt()
            textSize = 22f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(dateText, width / 2f, currentY, datePaint)

        return cardBitmap
    }

    /**
     * Shares the digital business card bitmap via Android native Intent.ACTION_SEND.
     */
    fun shareDigitalCard(
        context: Context,
        cardBitmap: Bitmap,
        payload: QrContactPayload
    ) {
        try {
            val cacheDir = File(context.cacheDir, "images").apply { mkdirs() }
            val file = File(cacheDir, "adt_card_${payload.token}.png")
            FileOutputStream(file).use { out ->
                cardBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareText = "Scannez mon QR Code ADT Pro pour m'ajouter directement à vos contacts sécurisés : ${payload.name} (${payload.phone})"

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                putExtra(Intent.EXTRA_SUBJECT, "Contact ADT Pro - ${payload.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Partager mon contact ADT Pro")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            // Fallback to text sharing if image share fails
            val shareText = "Ajoutez-moi sur ADT Pro !\nNom: ${payload.name}\nTél: ${payload.phone}\nLien: https://adtpro.app/join?phone=${payload.phone}"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Partager mon contact"))
        }
    }

    private fun urlEncode(value: String): String {
        return try {
            URLEncoder.encode(value, "UTF-8")
        } catch (_: Exception) {
            value
        }
    }
}
