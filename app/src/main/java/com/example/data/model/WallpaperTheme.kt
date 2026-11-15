package com.example.data.model

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

enum class WallpaperCategory(val displayName: String, val iconEmoji: String) {
    ALL("Tous", "✨"),
    COLORS("Couleurs", "🎨"),
    FLOWERS("Fleurs", "🌸"),
    TREES("Arbres", "🌳"),
    CARS("Voitures", "🏎️"),
    STARS("Étoiles", "✨"),
    MOON("Lune", "🌙"),
    SUN("Soleil", "☀️"),
    CLASSIC("Classiques", "🎨")
}

data class WallpaperItem(
    val id: String,
    val title: String,
    val description: String,
    val category: WallpaperCategory,
    val gradientColors: List<Color>,
    val accentColor: Color,
    val badgeEmoji: String,
    val patternType: String // "COLOR", "FLOWER", "TREE", "CAR", "STAR", "MOON", "SUN", "CLASSIC"
)

object WallpaperData {
    val allWallpapers: List<WallpaperItem> = listOf(
        // =========================================================================
        // COULEURS DU THÈME : ROUGE, BLEU, BLANCHE, NOIR, VIOLET, VERT, ROSE (ROSS)
        // =========================================================================
        WallpaperItem(
            id = "COULEUR_ROUGE",
            title = "Rouge Passion",
            description = "Nuances écarlates, velours carmin et énergie ardente",
            category = WallpaperCategory.COLORS,
            gradientColors = listOf(Color(0xFF2B0707), Color(0xFF521111), Color(0xFF1E0404)),
            accentColor = Color(0xFFE53935),
            badgeEmoji = "🔴",
            patternType = "COLOR_ROUGE"
        ),
        WallpaperItem(
            id = "COULEUR_BLEU",
            title = "Bleu Azur Océan",
            description = "Bleu saphir profond, confiance et clarté maritime",
            category = WallpaperCategory.COLORS,
            gradientColors = listOf(Color(0xFF07192C), Color(0xFF0F3256), Color(0xFF05111F)),
            accentColor = Color(0xFF1E88E5),
            badgeEmoji = "🔵",
            patternType = "COLOR_BLEU"
        ),
        WallpaperItem(
            id = "COULEUR_BLANCHE",
            title = "Blanche Pureté",
            description = "Finition blanc nacré et ivoire épuré ultra lumineux",
            category = WallpaperCategory.COLORS,
            gradientColors = listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0), Color(0xFFFFFFFF)),
            accentColor = Color(0xFF334155),
            badgeEmoji = "⚪",
            patternType = "COLOR_BLANCHE"
        ),
        WallpaperItem(
            id = "COULEUR_NOIR",
            title = "Noir Absolu OLED",
            description = "Obsidienne sombre ténébreuse optimisée pour écrans AMOLED",
            category = WallpaperCategory.COLORS,
            gradientColors = listOf(Color(0xFF080808), Color(0xFF141414), Color(0xFF000000)),
            accentColor = Color(0xFFEEEEEE),
            badgeEmoji = "⚫",
            patternType = "COLOR_NOIR"
        ),
        WallpaperItem(
            id = "COULEUR_VIOLET",
            title = "Violet Impérial",
            description = "Dégradé pourpre royal, améthyste et mystère céleste",
            category = WallpaperCategory.COLORS,
            gradientColors = listOf(Color(0xFF1B0728), Color(0xFF381052), Color(0xFF11031A)),
            accentColor = Color(0xFF8E24AA),
            badgeEmoji = "🟣",
            patternType = "COLOR_VIOLET"
        ),
        WallpaperItem(
            id = "COULEUR_VERT",
            title = "Vert Émeraude",
            description = "Fraîcheur végétale, forêt tropicale et sérénité naturelle",
            category = WallpaperCategory.COLORS,
            gradientColors = listOf(Color(0xFF06200F), Color(0xFF0D3E1E), Color(0xFF04140A)),
            accentColor = Color(0xFF43A047),
            badgeEmoji = "🟢",
            patternType = "COLOR_VERT"
        ),
        WallpaperItem(
            id = "COULEUR_ROSE",
            title = "Rose (Ross) Poudré",
            description = "Tons rose velouté, magnolia doux et éclat chaleureux",
            category = WallpaperCategory.COLORS,
            gradientColors = listOf(Color(0xFF2C091A), Color(0xFF561536), Color(0xFF1B0510)),
            accentColor = Color(0xFFEC407A),
            badgeEmoji = "🌸",
            patternType = "COLOR_ROSE"
        ),

        // FLEURS (Flowers)
        WallpaperItem(
            id = "FLEURS_ROSES",
            title = "Roses Sauvages",
            description = "Pétales de roses veloutées carmin et reflets rubis",
            category = WallpaperCategory.FLOWERS,
            gradientColors = listOf(Color(0xFF2A0812), Color(0xFF4A1020), Color(0xFF1F060D)),
            accentColor = Color(0xFFFF4081),
            badgeEmoji = "🌹",
            patternType = "FLOWER_ROSE"
        ),
        WallpaperItem(
            id = "FLEURS_SAKURA",
            title = "Cerisier Sakura",
            description = "Fleurs de cerisier japonais printanières et brise rose pastel",
            category = WallpaperCategory.FLOWERS,
            gradientColors = listOf(Color(0xFF2E1220), Color(0xFF501E36), Color(0xFF1E0A14)),
            accentColor = Color(0xFFFF80AB),
            badgeEmoji = "🌸",
            patternType = "FLOWER_SAKURA"
        ),
        WallpaperItem(
            id = "FLEURS_LAVANDE",
            title = "Champs de Lavande",
            description = "Champs violets de Provence parfumés au crépuscule",
            category = WallpaperCategory.FLOWERS,
            gradientColors = listOf(Color(0xFF1A102E), Color(0xFF331E54), Color(0xFF120B20)),
            accentColor = Color(0xFFB388FF),
            badgeEmoji = "🪻",
            patternType = "FLOWER_LAVENDER"
        ),
        WallpaperItem(
            id = "FLEURS_TOURNESOL",
            title = "Tournesols d'Or",
            description = "Pétales jaunes étincelants et lumière d'été éclatante",
            category = WallpaperCategory.FLOWERS,
            gradientColors = listOf(Color(0xFF2E2008), Color(0xFF543B0E), Color(0xFF1A1204)),
            accentColor = Color(0xFFFFD700),
            badgeEmoji = "🌻",
            patternType = "FLOWER_SUNFLOWER"
        ),
        WallpaperItem(
            id = "FLEURS_LOTUS",
            title = "Lotus Zen Sacré",
            description = "Fleurs de lotus sur étang paisible de nénuphars",
            category = WallpaperCategory.FLOWERS,
            gradientColors = listOf(Color(0xFF0D2520), Color(0xFF19443B), Color(0xFF091A16)),
            accentColor = Color(0xFF64FFDA),
            badgeEmoji = "🪷",
            patternType = "FLOWER_LOTUS"
        ),

        // ARBRES (Trees)
        WallpaperItem(
            id = "ARBRE_FORET",
            title = "Forêt Boréale Émeraude",
            description = "Pins majestueux dans la brume matinale nordique",
            category = WallpaperCategory.TREES,
            gradientColors = listOf(Color(0xFF071F18), Color(0xFF0F382C), Color(0xFF051410)),
            accentColor = Color(0xFF00E676),
            badgeEmoji = "🌲",
            patternType = "TREE_FOREST"
        ),
        WallpaperItem(
            id = "ARBRE_CHENE",
            title = "Chêne Millénaire",
            description = "Force ancestrale des racines et ramure protectrice",
            category = WallpaperCategory.TREES,
            gradientColors = listOf(Color(0xFF1F170C), Color(0xFF3B2D18), Color(0xFF140F08)),
            accentColor = Color(0xFFFFB74D),
            badgeEmoji = "🌳",
            patternType = "TREE_OAK"
        ),
        WallpaperItem(
            id = "ARBRE_BAMBOU",
            title = "Bambouseraie Zen",
            description = "Tiges de bambou élancées filtrant la lumière verte",
            category = WallpaperCategory.TREES,
            gradientColors = listOf(Color(0xFF0A2214), Color(0xFF144227), Color(0xFF06180E)),
            accentColor = Color(0xFF69F0AE),
            badgeEmoji = "🎋",
            patternType = "TREE_BAMBOO"
        ),
        WallpaperItem(
            id = "ARBRE_AUTOMNE",
            title = "Forêt d'Automne",
            description = "Feuilles d'érable flamboyantes rougeoyantes et cuivrées",
            category = WallpaperCategory.TREES,
            gradientColors = listOf(Color(0xFF2C1109), Color(0xFF542212), Color(0xFF1C0A05)),
            accentColor = Color(0xFFFF7043),
            badgeEmoji = "🍁",
            patternType = "TREE_AUTUMN"
        ),

        // VOITURES (Cars)
        WallpaperItem(
            id = "VOITURE_SUPERCAR",
            title = "Supercar Néon Nocturne",
            description = "Lignes fuselées et sillages lumineux néon en pleine ville",
            category = WallpaperCategory.CARS,
            gradientColors = listOf(Color(0xFF0D0E1F), Color(0xFF1B1D3D), Color(0xFF080914)),
            accentColor = Color(0xFF00E5FF),
            badgeEmoji = "🏎️",
            patternType = "CAR_SUPERCAR"
        ),
        WallpaperItem(
            id = "VOITURE_SPORT_GT",
            title = "Bolide GT Rouge",
            description = "Carrosserie rouge vermillon et esprit circuit grande vitesse",
            category = WallpaperCategory.CARS,
            gradientColors = listOf(Color(0xFF260A0A), Color(0xFF4A1414), Color(0xFF170606)),
            accentColor = Color(0xFFFF1744),
            badgeEmoji = "🚗",
            patternType = "CAR_SPORT"
        ),
        WallpaperItem(
            id = "VOITURE_CLASSIC",
            title = "Classique Vintage Rétro",
            description = "Élégance chromée des légendes automobiles des années 60",
            category = WallpaperCategory.CARS,
            gradientColors = listOf(Color(0xFF191B1D), Color(0xFF2C3034), Color(0xFF101213)),
            accentColor = Color(0xFFECEFF1),
            badgeEmoji = "🚙",
            patternType = "CAR_CLASSIC"
        ),
        WallpaperItem(
            id = "VOITURE_HYPERCAR",
            title = "Hypercar Carbone",
            description = "Fibre de carbone tissée et signature optique LED moderne",
            category = WallpaperCategory.CARS,
            gradientColors = listOf(Color(0xFF121417), Color(0xFF20252A), Color(0xFF0B0D0F)),
            accentColor = Color(0xFFFFD600),
            badgeEmoji = "⚡",
            patternType = "CAR_HYPERCAR"
        ),

        // ÉTOILES (Stars)
        WallpaperItem(
            id = "ETOILE_GALAXY",
            title = "Voie Lactée & Nébuleuse",
            description = "Spirale cosmique étincelante et nuages de poussière stellaire",
            category = WallpaperCategory.STARS,
            gradientColors = listOf(Color(0xFF110724), Color(0xFF241047), Color(0xFF0A0317)),
            accentColor = Color(0xFFE040FB),
            badgeEmoji = "🌌",
            patternType = "STAR_GALAXY"
        ),
        WallpaperItem(
            id = "ETOILE_FILANTE",
            title = "Ciel d'Étoiles Filantes",
            description = "Traînées de lumière féeriques traversant la voûte céleste",
            category = WallpaperCategory.STARS,
            gradientColors = listOf(Color(0xFF071226), Color(0xFF0F264C), Color(0xFF040A17)),
            accentColor = Color(0xFF82B1FF),
            badgeEmoji = "🌠",
            patternType = "STAR_SHOOTING"
        ),
        WallpaperItem(
            id = "ETOILE_CONSTELLATION",
            title = "Constellations Célestes",
            description = "Tracés stellaires dorés reliant les étoiles mythologiques",
            category = WallpaperCategory.STARS,
            gradientColors = listOf(Color(0xFF0A101D), Color(0xFF142036), Color(0xFF060A12)),
            accentColor = Color(0xFFFFEA00),
            badgeEmoji = "✨",
            patternType = "STAR_CONSTELLATION"
        ),

        // LUNE (Moon)
        WallpaperItem(
            id = "LUNE_PLEINE",
            title = "Pleine Lune d'Argent",
            description = "Disque lunaire immaculé brillant au-dessus des nuages sombres",
            category = WallpaperCategory.MOON,
            gradientColors = listOf(Color(0xFF0B131D), Color(0xFF162538), Color(0xFF070B10)),
            accentColor = Color(0xFFE0E0E0),
            badgeEmoji = "🌕",
            patternType = "MOON_FULL"
        ),
        WallpaperItem(
            id = "LUNE_CROISSANT",
            title = "Croissant de Lune",
            description = "Fin croissant doré dans un ciel crépusculaire mystérieux",
            category = WallpaperCategory.MOON,
            gradientColors = listOf(Color(0xFF130E26), Color(0xFF231B45), Color(0xFF0B0817)),
            accentColor = Color(0xFFFFD54F),
            badgeEmoji = "🌙",
            patternType = "MOON_CRESCENT"
        ),
        WallpaperItem(
            id = "LUNE_OCEAN",
            title = "Clair de Lune Océanique",
            description = "Reflets argentés miroitants sur les vagues de minuit",
            category = WallpaperCategory.MOON,
            gradientColors = listOf(Color(0xFF051624), Color(0xFF0C2C47), Color(0xFF030E17)),
            accentColor = Color(0xFF80DEEA),
            badgeEmoji = "🌊",
            patternType = "MOON_OCEAN"
        ),

        // SOLEIL (Sun)
        WallpaperItem(
            id = "SOLEIL_RAYONNANT",
            title = "Soleil Levant d'Or",
            description = "Premiers rayons solaires dorés traversant l'aube naissante",
            category = WallpaperCategory.SUN,
            gradientColors = listOf(Color(0xFF2E1C0A), Color(0xFF573413), Color(0xFF1C1106)),
            accentColor = Color(0xFFFFB300),
            badgeEmoji = "🌅",
            patternType = "SUN_RISE"
        ),
        WallpaperItem(
            id = "SOLEIL_COUCHANT",
            title = "Coucher de Soleil Flamboyant",
            description = "Dégradé ardent pourpre et orange au crépuscule flamboyant",
            category = WallpaperCategory.SUN,
            gradientColors = listOf(Color(0xFF2E0E0B), Color(0xFF571C17), Color(0xFF1D0907)),
            accentColor = Color(0xFFFF5722),
            badgeEmoji = "🌇",
            patternType = "SUN_SET"
        ),
        WallpaperItem(
            id = "SOLEIL_ZENITH",
            title = "Éclat Solaire Zénith",
            description = "Soleil radieux et halos chaleureux d'énergie pure",
            category = WallpaperCategory.SUN,
            gradientColors = listOf(Color(0xFF292209), Color(0xFF4F4112), Color(0xFF1A1505)),
            accentColor = Color(0xFFFFEE58),
            badgeEmoji = "☀️",
            patternType = "SUN_ZENITH"
        ),

        // CLASSIQUES (Classic ADT)
        WallpaperItem(
            id = "EMERALD",
            title = "Émeraude ADT Pro",
            description = "Signature professionnelle vert émeraude et sécurité",
            category = WallpaperCategory.CLASSIC,
            gradientColors = listOf(Color(0xFF0B141A), Color(0xFF06231E), Color(0xFF0B141A)),
            accentColor = Color(0xFF00A884),
            badgeEmoji = "🛡️",
            patternType = "CLASSIC_EMERALD"
        ),
        WallpaperItem(
            id = "DOODLE",
            title = "Doodle Graphique",
            description = "Motifs vectoriels subtils de messagerie et connectivité",
            category = WallpaperCategory.CLASSIC,
            gradientColors = listOf(Color(0xFF101B20), Color(0xFF192A32), Color(0xFF0B141A)),
            accentColor = Color(0xFF53BDEB),
            badgeEmoji = "💬",
            patternType = "CLASSIC_DOODLE"
        ),
        WallpaperItem(
            id = "MIDNIGHT",
            title = "Minuit Sombre OLED",
            description = "Noir pur et économie d'énergie pour écrans AMOLED",
            category = WallpaperCategory.CLASSIC,
            gradientColors = listOf(Color(0xFF070B0E), Color(0xFF121B22), Color(0xFF05080A)),
            accentColor = Color(0xFF90A4AE),
            badgeEmoji = "🌑",
            patternType = "CLASSIC_MIDNIGHT"
        ),
        WallpaperItem(
            id = "SAPPHIRE",
            title = "Saphir Royal",
            description = "Bleu de nuit statutaire inspiré des télécoms d'affaires",
            category = WallpaperCategory.CLASSIC,
            gradientColors = listOf(Color(0xFF0A1828), Color(0xFF142B47), Color(0xFF07111D)),
            accentColor = Color(0xFF448AFF),
            badgeEmoji = "💎",
            patternType = "CLASSIC_SAPPHIRE"
        ),
        WallpaperItem(
            id = "SUNSET",
            title = "Crépuscule Ambré",
            description = "Atmosphère chaleureuse et feutrée",
            category = WallpaperCategory.CLASSIC,
            gradientColors = listOf(Color(0xFF241410), Color(0xFF381B16), Color(0xFF170D0B)),
            accentColor = Color(0xFFFF7043),
            badgeEmoji = "🍂",
            patternType = "CLASSIC_SUNSET"
        )
    )

    fun findById(id: String): WallpaperItem {
        return allWallpapers.find { it.id == id } ?: allWallpapers.first()
    }
}
