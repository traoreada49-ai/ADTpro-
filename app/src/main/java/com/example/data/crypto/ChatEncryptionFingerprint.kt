package com.example.data.crypto

import androidx.compose.ui.graphics.Color

/**
 * Model representing the unique cryptographic fingerprint and security properties
 * for a specific End-to-End Encrypted (Signal Double Ratchet) chat session.
 */
data class ChatEncryptionFingerprint(
    val chatId: String,
    val title: String,
    val subtitle: String,
    val avatarUrl: String,
    val isGroup: Boolean,
    val safetyNumber: String,               // 60-digit Signal Safety Number (12 groups of 5)
    val hexFingerprint: String,             // SHA-256 session key fingerprint (32 hex pairs)
    val shortFingerprint: String,           // First 8 characters (e.g. "E4:A1:8B:20")
    val visualFingerprintColors: List<Color>, // 4 colors derived deterministically from the hash
    val protocolName: String = "Signal Double Ratchet",
    val cipherSuite: String = "AES-256-GCM / HKDF-SHA256",
    val ephemeralKeySnippet: String,        // Ephemeral curve25519/EC public key snippet
    val ratchetStep: Int,
    val isVerified: Boolean = false,
    val verifiedDate: String? = null,
    val totalMessagesEncrypted: Int = 0,
    val lastActiveTimestamp: Long = 0L
) {
    /**
     * Text representation for exporting, sharing, or copying
     */
    fun toSharableSummary(): String {
        return buildString {
            appendLine("=== EMPREINTE DE SÉCURITÉ ADT PRO (E2EE) ===")
            appendLine("Discussion : $title")
            appendLine("Identifiant : $subtitle")
            appendLine("Protocole : $protocolName ($cipherSuite)")
            appendLine("Numéro de sécurité (60 chiffres) :")
            appendLine(safetyNumber)
            appendLine("Empreinte SHA-256 :")
            appendLine(hexFingerprint)
            appendLine("Statut de vérification : ${if (isVerified) "VÉRIFIÉ ✓" else "NON VÉRIFIÉ"}")
        }
    }

    companion object {
        /**
         * Helper to derive 4 harmonious colors deterministically from a hex fingerprint
         * for instant optical comparison (like visual identicons).
         */
        fun deriveVisualColors(hex: String): List<Color> {
            val cleanHex = hex.replace(":", "").filter { it.isLetterOrDigit() }
            if (cleanHex.length < 24) {
                return listOf(Color(0xFF00A884), Color(0xFF1E88E5), Color(0xFF8E24AA), Color(0xFFE53935))
            }
            return (0 until 4).map { i ->
                val r = cleanHex.substring(i * 6, i * 6 + 2).toIntOrNull(16) ?: 100
                val g = cleanHex.substring(i * 6 + 2, i * 6 + 4).toIntOrNull(16) ?: 150
                val b = cleanHex.substring(i * 6 + 4, i * 6 + 6).toIntOrNull(16) ?: 200
                // Ensure sufficient saturation and brightness
                val adjR = (r * 0.7f + 50).toInt().coerceIn(40, 240)
                val adjG = (g * 0.7f + 50).toInt().coerceIn(40, 240)
                val adjB = (b * 0.7f + 50).toInt().coerceIn(40, 240)
                Color(adjR, adjG, adjB)
            }
        }
    }
}
