package com.example.data.live

enum class LiveCategory(val label: String, val icon: String) {
    DISCUSSION("Discussion", "💬"),
    MUSIQUE("Musique & DJ", "🎵"),
    BUSINESS("Business & Vente", "💼"),
    GAMING("Gaming", "🎮"),
    BEAUTE("Mode & Beauté", "💄"),
    EDUCATION("Éducation & Tech", "💡"),
    HUMOUR("Humour & Show", "🎭"),
    CUISINE("Cuisine & Recettes", "🍲")
}

data class LiveGift(
    val id: String,
    val name: String,
    val icon: String,
    val coins: Int,
    val soundDesc: String,
    val colorHex: Long
)

val DEFAULT_TIKTOK_GIFTS = listOf(
    LiveGift("rose", "Rose", "🌹", 1, "Chime", 0xFFFF2D55),
    LiveGift("heart", "Cœur Enflammé", "💖", 5, "Sparkle", 0xFFFF4081),
    LiveGift("the_touba", "Thé Touba", "☕", 10, "Warmth", 0xFFD48B38),
    LiveGift("fire", "Flamme Royale", "🔥", 20, "Whoosh", 0xFFFF6D00),
    LiveGift("diamond", "Diamant Pur", "💎", 50, "Shine", 0xFF00E5FF),
    LiveGift("galaxy", "Fusée Galaxie", "🚀", 100, "Blast", 0xFF7C4DFF),
    LiveGift("crown", "Couronne d'Or", "👑", 250, "Fanfare", 0xFFFFD700),
    LiveGift("lion", "Lion Majestueux", "🦁", 500, "Roar", 0xFFFF9100),
    LiveGift("adt_box", "Coffre ADT Pro", "🎁", 1000, "Jackpot", 0xFF00A884)
)

data class LiveComment(
    val id: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String,
    val badge: String? = null,
    val badgeColorHex: Long = 0xFF25D366,
    val text: String,
    val isSystem: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class LiveGiftEvent(
    val id: String,
    val gift: LiveGift,
    val senderName: String,
    val senderAvatar: String,
    val combo: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)

data class LiveHeartBurst(
    val id: String,
    val xOffset: Float, // 0.6f to 0.95f
    val colorHex: Long,
    val emoji: String = "❤️"
)

data class LiveCreatorAuthorization(
    val isAuthorized: Boolean = true,
    val creatorTier: String = "Créateur Officiel Certifié",
    val identityVerified: Boolean = true,
    val communityCharterAccepted: Boolean = true,
    val authorizedDirectBadge: String = "Direct Autorisé ADT",
    val maxDurationMinutes: Int = 180,
    val authorizedFeatures: List<String> = listOf(
        "Diffusion Full HD 1080p 60fps",
        "Réception Cadeaux & Monétisation Diamants",
        "Duo en direct & PK Battle en direct",
        "Filtres Beauté IA & Décors Studio",
        "Modération automatique des commentaires",
        "Chiffrement WebRTC certifié"
    )
)

data class LiveStreamItem(
    val id: String,
    val hostId: String,
    val hostName: String,
    val hostHandle: String,
    val hostAvatar: String,
    val title: String,
    val category: LiveCategory,
    val isLive: Boolean = true,
    val viewerCount: Int,
    val likesCount: Int,
    val diamondsCount: Int = 0,
    val isAuthorized: Boolean = true,
    val authorizationTier: String = "Direct Autorisé",
    val startedAt: Long = System.currentTimeMillis() - 120_000,
    val coverGradientStart: Long = 0xFF1E293B,
    val coverGradientEnd: Long = 0xFF0F172A,
    val tags: List<String> = listOf("En Direct", "Certifié", "Interactif"),
    val musicTrack: String = "Son original - Ambiance Live Studio 🎵",
    val isFollowing: Boolean = false,
    val isHost: Boolean = false
)

data class LiveStreamSessionState(
    val stream: LiveStreamItem,
    val isHost: Boolean = false,
    val durationSeconds: Int = 45,
    val viewerCount: Int = 1240,
    val likesCount: Int = 18450,
    val diamondsEarned: Int = 340,
    val comments: List<LiveComment> = emptyList(),
    val activeGiftEvent: LiveGiftEvent? = null,
    val floatingHearts: List<LiveHeartBurst> = emptyList(),
    val isMicMuted: Boolean = false,
    val isFrontCamera: Boolean = true,
    val activeFilter: String = "Naturel",
    val beautyLevel: Float = 0.5f,
    val isPkBattleActive: Boolean = false,
    val pkHostScore: Int = 1680,
    val pkOpponentScore: Int = 1420,
    val pkOpponentName: String = "DJ Samba (Dakar)",
    val pkRemainingSeconds: Int = 120,
    val showEndSummary: Boolean = false,
    val isFollowing: Boolean = false,
    val userCoinsBalance: Int = 1250,
    val pinnedNotice: String? = "Bienvenue dans le Direct Autorisé ! Respectez la charte communautaire."
)
