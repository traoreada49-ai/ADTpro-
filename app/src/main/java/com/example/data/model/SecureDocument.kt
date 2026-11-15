package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modèle pour les documents chiffrés et vérifiés partagés dans ADT Pro.
 */
data class SecureDocument(
    val id: String = "",
    val name: String = "Document.pdf",
    val sizeBytes: Long = 1845000L,
    val mimeType: String = "application/pdf",
    val url: String = "",
    val isEncrypted: Boolean = true,
    val sha256Checksum: String = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    val securityLevel: String = "MAXIMUM (AES-256-GCM)",
    val isVerified: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
) {
    val formattedSize: String
        get() = when {
            sizeBytes >= 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f Mo", sizeBytes / (1024f * 1024f))
            sizeBytes >= 1024 -> "${sizeBytes / 1024} Ko"
            else -> "$sizeBytes octets"
        }

    val formattedDate: String
        get() = SimpleDateFormat("dd MMM, HH:mm", Locale.FRENCH).format(Date(timestamp))
}
