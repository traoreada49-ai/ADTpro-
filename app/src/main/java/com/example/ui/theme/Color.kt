package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ADT Pro Brand Colors (Emerald & Deep Teal WhatsApp Business style)
val AdtPrimary = Color(0xFF00A884)
val AdtPrimaryDark = Color(0xFF075E54)
val AdtPrimaryLight = Color(0xFF25D366)
val AdtAccentGreen = Color(0xFF25D366)
val AdtSecondary = Color(0xFF128C7E)
val AdtAccent = Color(0xFF34B7F1)

// Dark Theme Palette
val DarkBg = Color(0xFF0B141A)
val DarkSurface = Color(0xFF111B21)
val DarkSurfaceVariant = Color(0xFF202C33)
val DarkBorder = Color(0xFF2A3942)
val DarkTextPrimary = Color(0xFFE9EDEF)
val DarkTextSecondary = Color(0xFF8696A0)
val DarkChatBg = Color(0xFF0C1317)
val DarkBubbleOutgoing = Color(0xFF005C4B)
val DarkBubbleIncoming = Color(0xFF202C33)

// Light Theme Palette
val LightBg = Color(0xFFFFFFFF)
val LightSurface = Color(0xFFF0F2F5)
val LightSurfaceVariant = Color(0xFFE9EDEF)
val LightBorder = Color(0xFFE1E7EA)
val LightTextPrimary = Color(0xFF111B21)
val LightTextSecondary = Color(0xFF667781)
val LightChatBg = Color(0xFFEFEAE2)
val LightBubbleOutgoing = Color(0xFFD9FDD3)
val LightBubbleIncoming = Color(0xFFFFFFFF)

// Call & Status Colors
val CallGreen = Color(0xFF25D366)
val CallRed = Color(0xFFEA0038)
val StatusUnreadBorder = Color(0xFF25D366)
val StatusSeenBorder = Color(0xFF8696A0)

// Operator Colors (Telecoms Africains & Internationaux)
val OperatorOrange = Color(0xFFFF7900)
val OperatorMoov = Color(0xFF0066B3)
val OperatorTelecel = Color(0xFFED1C24)
val OperatorMtn = Color(0xFFFFCC00)
val OperatorVodacom = Color(0xFFE60000)
val OperatorAirtel = Color(0xFFED1C24)
val OperatorSafaricom = Color(0xFF009A44)
val OperatorTigo = Color(0xFF00377B)
val OperatorHalotel = Color(0xFFFF6600)
val OperatorAfricell = Color(0xFF800080)
val OperatorTelkom = Color(0xFF0072CE)
val OperatorMauritel = Color(0xFF00843D)
val OperatorChinguitel = Color(0xFF009688)
val OperatorMattel = Color(0xFFD32F2F)
val OperatorCamtel = Color(0xFF1E88E5)
val OperatorNexttel = Color(0xFFE91E63)
val OperatorGeneric = Color(0xFF00A884)

// Wallpaper Themes
val WallpaperDoodleDark = Color(0xFF0B141A)
val WallpaperDoodleLight = Color(0xFFEAE6DF)
val WallpaperEmerald = Color(0xFF07362E)
val WallpaperMidnight = Color(0xFF121B22)
val WallpaperSunset = Color(0xFF2C1810)
val WallpaperSapphire = Color(0xFF0F1E2E)

// =========================================================================
// COULEURS DU THÈME : ROUGE, BLEU, BLANCHE, NOIR, VIOLET, VERT, ROSE (ROSS)
// =========================================================================
val ColorRouge = Color(0xFFE53935)
val ColorRougeDark = Color(0xFFB71C1C)
val ColorRougeLight = Color(0xFFFF6B6B)

val ColorBleu = Color(0xFF1E88E5)
val ColorBleuDark = Color(0xFF0D47A1)
val ColorBleuLight = Color(0xFF64B5F6)

val ColorBlanche = Color(0xFFFFFFFF)
val ColorBlancheOff = Color(0xFFF5F5F5)
val ColorBlancheDark = Color(0xFFE0E0E0)

val ColorNoir = Color(0xFF121212)
val ColorNoirPure = Color(0xFF000000)
val ColorNoirLight = Color(0xFF242424)

val ColorViolet = Color(0xFF8E24AA)
val ColorVioletDark = Color(0xFF4A148C)
val ColorVioletLight = Color(0xFFBA68C8)

val ColorVert = Color(0xFF43A047)
val ColorVertDark = Color(0xFF1B5E20)
val ColorVertLight = Color(0xFF81C784)

val ColorRose = Color(0xFFEC407A)
val ColorRoseDark = Color(0xFF880E4F)
val ColorRoseLight = Color(0xFFF48FB1)
val ColorRoss = ColorRose // Alias pour "ross"

/**
 * Palette des 7 couleurs configurables pour ADT Pro.
 */
enum class AdtPaletteColor(
    val id: String,
    val frenchName: String,
    val primary: Color,
    val darkVariant: Color,
    val lightVariant: Color,
    val hexCode: String,
    val emojiBadge: String,
    val description: String
) {
    ROUGE(
        id = "ROUGE",
        frenchName = "Rouge",
        primary = ColorRouge,
        darkVariant = ColorRougeDark,
        lightVariant = ColorRougeLight,
        hexCode = "#E53935",
        emojiBadge = "🔴",
        description = "Écarlate vif & dynamique"
    ),
    BLEU(
        id = "BLEU",
        frenchName = "Bleu",
        primary = ColorBleu,
        darkVariant = ColorBleuDark,
        lightVariant = ColorBleuLight,
        hexCode = "#1E88E5",
        emojiBadge = "🔵",
        description = "Azur moderne & haute confiance"
    ),
    BLANCHE(
        id = "BLANCHE",
        frenchName = "Blanche",
        primary = ColorBlanche,
        darkVariant = ColorBlancheDark,
        lightVariant = ColorBlancheOff,
        hexCode = "#FFFFFF",
        emojiBadge = "⚪",
        description = "Pureté lumineuse & minimalisme"
    ),
    NOIR(
        id = "NOIR",
        frenchName = "Noir",
        primary = ColorNoir,
        darkVariant = ColorNoirPure,
        lightVariant = ColorNoirLight,
        hexCode = "#121212",
        emojiBadge = "⚫",
        description = "Obsidienne & OLED profond"
    ),
    VIOLET(
        id = "VIOLET",
        frenchName = "Violet",
        primary = ColorViolet,
        darkVariant = ColorVioletDark,
        lightVariant = ColorVioletLight,
        hexCode = "#8E24AA",
        emojiBadge = "🟣",
        description = "Pourpre impérial & améthyste"
    ),
    VERT(
        id = "VERT",
        frenchName = "Vert",
        primary = ColorVert,
        darkVariant = ColorVertDark,
        lightVariant = ColorVertLight,
        hexCode = "#43A047",
        emojiBadge = "🟢",
        description = "Émeraude végétale & sérénité"
    ),
    ROSE(
        id = "ROSE",
        frenchName = "Rose (Ross)",
        primary = ColorRose,
        darkVariant = ColorRoseDark,
        lightVariant = ColorRoseLight,
        hexCode = "#EC407A",
        emojiBadge = "🌸",
        description = "Poudré velouté & éclat chaleureux"
    );

    val wallpaperThemeId: String
        get() = "COULEUR_$id"

    companion object {
        val ALL_COLORS = entries.toList()

        fun fromId(id: String?): AdtPaletteColor {
            if (id == null) return VERT
            val clean = id.removePrefix("COULEUR_").uppercase()
            return entries.find { it.name == clean || it.id == clean }
                ?: if (clean.contains("ROSS")) ROSE else VERT
        }
    }
}

