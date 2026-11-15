package com.example.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

data class CameraEffect(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val accentColor: Color,
    val overlayColor: Color,
    val colorMatrix: ColorMatrix
)

object CameraEffectsData {
    // 1. Naturel / Standard
    val NATURAL = CameraEffect(
        id = "NATURAL",
        name = "Original",
        emoji = "✨",
        description = "Rendu standard haute fidélité sans filtre",
        accentColor = Color(0xFF00A884),
        overlayColor = Color.Transparent,
        colorMatrix = ColorMatrix()
    )

    // 2. Citron (Lemon / Warm vibrant solar yellow)
    val CITRON = CameraEffect(
        id = "CITRON",
        name = "Citron",
        emoji = "🍋",
        description = "Éclat solaire jaune citron énergisant et vivifiant",
        accentColor = Color(0xFFFFD600),
        overlayColor = Color(0x33FFEA00),
        colorMatrix = ColorMatrix(
            floatArrayOf(
                1.35f, 0.15f, 0.00f, 0f, 25f,
                0.15f, 1.40f, 0.00f, 0f, 25f,
                0.00f, 0.05f, 0.65f, 0f, 0f,
                0f,    0f,    0f,    1f, 0f
            )
        )
    )

    // 3. Fraisier (Strawberry / Sweet Rosy Pink Glow)
    val FRAISIER = CameraEffect(
        id = "FRAISIER",
        name = "Fraisier",
        emoji = "🍓",
        description = "Douceur rose fraise veloutée et teint frais éclatant",
        accentColor = Color(0xFFFF4081),
        overlayColor = Color(0x33FF4081),
        colorMatrix = ColorMatrix(
            floatArrayOf(
                1.40f, 0.10f, 0.10f, 0f, 35f,
                0.05f, 0.95f, 0.15f, 0f, 10f,
                0.15f, 0.10f, 1.25f, 0f, 25f,
                0f,    0f,    0f,    1f, 0f
            )
        )
    )

    // 4. Orange (Citrus Sunset / Warm Amber)
    val ORANGE = CameraEffect(
        id = "ORANGE",
        name = "Orange",
        emoji = "🍊",
        description = "Chaleur ambrée d'agrumes et coucher de soleil doré",
        accentColor = Color(0xFFFF6D00),
        overlayColor = Color(0x38FF6D00),
        colorMatrix = ColorMatrix(
            floatArrayOf(
                1.50f, 0.15f, 0.00f, 0f, 30f,
                0.10f, 1.15f, 0.00f, 0f, 15f,
                0.00f, 0.00f, 0.60f, 0f, 0f,
                0f,    0f,    0f,    1f, 0f
            )
        )
    )

    // 5. White (Pure White / High-Key Luminescent)
    val WHITE = CameraEffect(
        id = "WHITE",
        name = "White",
        emoji = "⚪",
        description = "Lumière pure studio, haute luminosité et clarté cristalline",
        accentColor = Color(0xFFFFFFFF),
        overlayColor = Color(0x28FFFFFF),
        colorMatrix = ColorMatrix(
            floatArrayOf(
                1.25f, 0.00f, 0.00f, 0f, 40f,
                0.00f, 1.25f, 0.00f, 0f, 40f,
                0.00f, 0.00f, 1.25f, 0f, 40f,
                0f,    0f,    0f,    1f, 0f
            )
        )
    )

    // 6. Noir & Blanc (Classic Monochrome / High Contrast)
    val NOIR_BLANC = CameraEffect(
        id = "NOIR_BLANC",
        name = "Noir & Blanc",
        emoji = "⚫",
        description = "Monochrome intemporel, contraste argentique et élégance",
        accentColor = Color(0xFF9E9E9E),
        overlayColor = Color(0x22000000),
        colorMatrix = ColorMatrix().apply {
            setToSaturation(0f)
        }
    )

    // 7. Bleu (Cyan Ocean / Electric Cold Blue)
    val BLEU = CameraEffect(
        id = "BLEU",
        name = "Bleu",
        emoji = "🔵",
        description = "Océan profond, fraîcheur cyan et ambiance cinéma nocturne",
        accentColor = Color(0xFF00B0FF),
        overlayColor = Color(0x3300B0FF),
        colorMatrix = CameraEffectsData.makeBlueMatrix()
    )

    // 8. Émeraude (Lush green / ADT Nature)
    val EMERAUDE = CameraEffect(
        id = "EMERAUDE",
        name = "Émeraude",
        emoji = "🌿",
        description = "Vert végétal luxuriant et tonalité ADT Pro naturelle",
        accentColor = Color(0xFF00E676),
        overlayColor = Color(0x3000E676),
        colorMatrix = ColorMatrix(
            floatArrayOf(
                0.80f, 0.10f, 0.00f, 0f, 0f,
                0.15f, 1.45f, 0.10f, 0f, 25f,
                0.00f, 0.15f, 0.85f, 0f, 5f,
                0f,    0f,    0f,    1f, 0f
            )
        )
    )

    // 9. Vintage 1980 (Sepia Retro)
    val VINTAGE = CameraEffect(
        id = "VINTAGE",
        name = "Vintage",
        emoji = "🎞️",
        description = "Grain argentique rétro sépia et nostalgie chaleureuse",
        accentColor = Color(0xFFD7CCC8),
        overlayColor = Color(0x338D6E63),
        colorMatrix = ColorMatrix(
            floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 20f,
                0.349f, 0.686f, 0.168f, 0f, 15f,
                0.272f, 0.534f, 0.131f, 0f, 5f,
                0f,     0f,     0f,     1f, 0f
            )
        )
    )

    // 10. Néon Cyber (Purple & Magenta)
    val NEON = CameraEffect(
        id = "NEON",
        name = "Néon",
        emoji = "🟣",
        description = "Atmosphère futuriste cyberpunk violette et rose néon",
        accentColor = Color(0xFFE040FB),
        overlayColor = Color(0x33E040FB),
        colorMatrix = ColorMatrix(
            floatArrayOf(
                1.40f, 0.00f, 0.35f, 0f, 20f,
                0.00f, 0.75f, 0.20f, 0f, 0f,
                0.40f, 0.10f, 1.50f, 0f, 30f,
                0f,    0f,    0f,    1f, 0f
            )
        )
    )

    private fun makeBlueMatrix(): ColorMatrix {
        return ColorMatrix(
            floatArrayOf(
                0.70f, 0.05f, 0.10f, 0f, 0f,
                0.10f, 1.05f, 0.25f, 0f, 10f,
                0.15f, 0.25f, 1.65f, 0f, 35f,
                0f,    0f,    0f,    1f, 0f
            )
        )
    }

    val allEffects: List<CameraEffect> = listOf(
        NATURAL,
        CITRON,
        FRAISIER,
        ORANGE,
        WHITE,
        NOIR_BLANC,
        BLEU,
        EMERAUDE,
        VINTAGE,
        NEON
    )

    fun findById(id: String): CameraEffect {
        return allEffects.find { it.id == id } ?: NATURAL
    }
}
