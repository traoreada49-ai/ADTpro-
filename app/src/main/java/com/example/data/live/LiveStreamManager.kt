package com.example.data.live

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import kotlin.random.Random

class LiveStreamManager(
    private val coroutineScope: CoroutineScope
) {

    private val sampleCommentsPool = listOf(
        Pair("Awa Ndiaye", "Trop fière de ce live ! Continue comme ça 🔥"),
        Pair("Mamadou Diallo", "Bonsoir la famille ADT Pro ! 🇸🇳"),
        Pair("Koffi Mensah", "Salutations depuis Abidjan ! Force à toi 👏"),
        Pair("Fatou Camara", "Appuyez tous deux fois sur l'écran pour les likes ❤️"),
        Pair("Ibrahima Sarr", "La qualité du direct est incroyable en 1080p !"),
        Pair("Mariam Traoré", "On adore le contenu, grand respect 🌟"),
        Pair("Cheikh Tidiane", "Envoyez des cadeaux pour monter le classement 🎁"),
        Pair("Bintou Cissé", "Tu réponds à ma question s'il te plaît ? 🙏"),
        Pair("Oumar Koné", "Dédicace spéciale depuis Bamako !"),
        Pair("Ndeye Fall", "C'est validé à 100% ce live direct direct ! 🚀"),
        Pair("Alain Kouassi", "Le son et l'image sont ultra nets."),
        Pair("Aminata Sow", "Let's go la team ! Tapez sur l'écran 💖"),
        Pair("Sidy Barry", "La musique déchire tout ce soir 🎵"),
        Pair("Kadiatou Bah", "Magnifique décor de studio ! ✨"),
        Pair("Papa Demba", "Partagez le direct les amis, objectif 10K likes !")
    )

    private val userBadges = listOf(
        "👑 Top Fan",
        "🛡️ Modérateur",
        "⭐ Abonné",
        "🌟 VIP",
        "💎 Diamant",
        "✨ Nouveau"
    )

    private val _availableStreams = MutableStateFlow<List<LiveStreamItem>>(getDefaultStreams())
    val availableStreams: StateFlow<List<LiveStreamItem>> = _availableStreams.asStateFlow()

    private val _activeSession = MutableStateFlow<LiveStreamSessionState?>(null)
    val activeSession: StateFlow<LiveStreamSessionState?> = _activeSession.asStateFlow()

    private val _creatorAuthorization = MutableStateFlow(LiveCreatorAuthorization())
    val creatorAuthorization: StateFlow<LiveCreatorAuthorization> = _creatorAuthorization.asStateFlow()

    private var liveJob: Job? = null

    private fun getDefaultStreams(): List<LiveStreamItem> {
        return listOf(
            LiveStreamItem(
                id = "live_1",
                hostId = "host_awa",
                hostName = "Awa Diop",
                hostHandle = "@awadiop_pro",
                hostAvatar = "Awa",
                title = "🔴 Débat Business & Entrepreneuriat Numérique en Afrique",
                category = LiveCategory.BUSINESS,
                viewerCount = 2840,
                likesCount = 38400,
                diamondsCount = 1420,
                isAuthorized = true,
                authorizationTier = "Direct Autorisé ADT",
                startedAt = System.currentTimeMillis() - 900_000,
                coverGradientStart = 0xFF7C3AED,
                coverGradientEnd = 0xFF1E1B4B,
                tags = listOf("Business", "Tech", "Direct Autorisé"),
                musicTrack = "Son original - Afrobeat Lo-Fi Chill 🎶"
            ),
            LiveStreamItem(
                id = "live_2",
                hostId = "host_moussa",
                hostName = "Moussa DJ Set",
                hostHandle = "@moussa_mix",
                hostAvatar = "Moussa",
                title = "🎵 Session Afrobeat & Mbalax Live Mix • Ambiance Dakar",
                category = LiveCategory.MUSIQUE,
                viewerCount = 4210,
                likesCount = 92600,
                diamondsCount = 3890,
                isAuthorized = true,
                authorizationTier = "Direct Autorisé ADT",
                startedAt = System.currentTimeMillis() - 1_800_000,
                coverGradientStart = 0xFF0D9488,
                coverGradientEnd = 0xFF042F2E,
                tags = listOf("Afrobeat", "DJ Set", "Ambiance"),
                musicTrack = "DJ Moussa - Live Mix Club Dakar 2026 🔥"
            ),
            LiveStreamItem(
                id = "live_3",
                hostId = "host_fatou",
                hostName = "Chef Fatou Sall",
                hostHandle = "@fatou_cuisine",
                hostAvatar = "Fatou",
                title = "🍲 Recette Thiéboudienne Penda Mbaye en direct live !",
                category = LiveCategory.CUISINE,
                viewerCount = 1680,
                likesCount = 24100,
                diamondsCount = 740,
                isAuthorized = true,
                authorizationTier = "Direct Autorisé ADT",
                startedAt = System.currentTimeMillis() - 420_000,
                coverGradientStart = 0xFFEA580C,
                coverGradientEnd = 0xFF431407,
                tags = listOf("Cuisine", "Gourmet", "Direct"),
                musicTrack = "Acoustic Kora - Saveurs Africaines 🍲"
            ),
            LiveStreamItem(
                id = "live_4",
                hostId = "host_cheikh",
                hostName = "Cheikh Tech",
                hostHandle = "@cheikh_tech",
                hostAvatar = "Cheikh",
                title = "📱 Test en direct : Nouvelle mise à jour WebRTC & IA Pro",
                category = LiveCategory.EDUCATION,
                viewerCount = 950,
                likesCount = 15300,
                diamondsCount = 520,
                isAuthorized = true,
                authorizationTier = "Direct Autorisé ADT",
                startedAt = System.currentTimeMillis() - 600_000,
                coverGradientStart = 0xFF2563EB,
                coverGradientEnd = 0xFF0F172A,
                tags = listOf("Tech", "Android", "Innovation"),
                musicTrack = "Synthwave Tech Pulse ⚡"
            ),
            LiveStreamItem(
                id = "live_5",
                hostId = "host_mariam",
                hostName = "Mariam Fashion",
                hostHandle = "@mariam_style",
                hostAvatar = "Mariam",
                title = "💄 Look du Jour & Vente Flash Wax Moderne",
                category = LiveCategory.BEAUTE,
                viewerCount = 3120,
                likesCount = 54200,
                diamondsCount = 2100,
                isAuthorized = true,
                authorizationTier = "Direct Autorisé ADT",
                startedAt = System.currentTimeMillis() - 1_200_000,
                coverGradientStart = 0xFFDB2777,
                coverGradientEnd = 0xFF500724,
                tags = listOf("Mode", "Beauté", "Wax"),
                musicTrack = "Afro Pop Glow - Mariam Style ✨"
            ),
            LiveStreamItem(
                id = "live_6",
                hostId = "host_ibrahim",
                hostName = "Ibrahim Comedy",
                hostHandle = "@ibrahim_show",
                hostAvatar = "Ibrahima",
                title = "🎭 Stand-up & Improvisation en direct avec les abonnés !",
                category = LiveCategory.HUMOUR,
                viewerCount = 5890,
                likesCount = 112000,
                diamondsCount = 4920,
                isAuthorized = true,
                authorizationTier = "Direct Autorisé ADT",
                startedAt = System.currentTimeMillis() - 2_400_000,
                coverGradientStart = 0xFF9333EA,
                coverGradientEnd = 0xFF3B0764,
                tags = listOf("Humour", "StandUp", "Impro"),
                musicTrack = "Comédie Club Rires & Vibes 🎭"
            )
        )
    }

    /**
     * Start hosting a live broadcast (Creator mode)
     */
    fun startHosting(
        title: String,
        category: LiveCategory,
        hostName: String,
        hostAvatar: String,
        isFrontCamera: Boolean = true
    ) {
        val streamId = "live_host_${UUID.randomUUID()}"
        val newStream = LiveStreamItem(
            id = streamId,
            hostId = "current_user",
            hostName = hostName,
            hostHandle = "@${hostName.lowercase().replace(" ", "_")}",
            hostAvatar = hostAvatar,
            title = title.ifBlank { "🔴 En Direct avec $hostName" },
            category = category,
            viewerCount = 1,
            likesCount = 0,
            diamondsCount = 0,
            isAuthorized = _creatorAuthorization.value.isAuthorized,
            authorizationTier = _creatorAuthorization.value.authorizedDirectBadge,
            startedAt = System.currentTimeMillis(),
            isHost = true,
            coverGradientStart = 0xFFFE2C55,
            coverGradientEnd = 0xFF121212
        )

        // Add to available streams at the top
        _availableStreams.value = listOf(newStream) + _availableStreams.value

        _activeSession.value = LiveStreamSessionState(
            stream = newStream,
            isHost = true,
            durationSeconds = 0,
            viewerCount = 14,
            likesCount = 25,
            diamondsEarned = 0,
            isFrontCamera = isFrontCamera,
            comments = listOf(
                LiveComment(
                    id = UUID.randomUUID().toString(),
                    senderId = "system",
                    senderName = "Système ADT Pro",
                    senderAvatar = "ADT",
                    badge = "🛡️ Sécurité",
                    badgeColorHex = 0xFF00A884,
                    text = "Votre Direct Autorisé a démarré. Les échanges respectent les règles communautaires et sont sécurisés.",
                    isSystem = true
                )
            )
        )

        startSimulationLoop(isHost = true)
    }

    /**
     * Watch an existing live stream (Viewer mode)
     */
    fun watchStream(streamId: String) {
        val stream = _availableStreams.value.find { it.id == streamId } ?: _availableStreams.value.first()
        val currentCoins = _activeSession.value?.userCoinsBalance ?: 1250

        _activeSession.value = LiveStreamSessionState(
            stream = stream,
            isHost = false,
            durationSeconds = ((System.currentTimeMillis() - stream.startedAt) / 1000).toInt().coerceAtLeast(30),
            viewerCount = stream.viewerCount,
            likesCount = stream.likesCount,
            diamondsEarned = stream.diamondsCount,
            isFollowing = stream.isFollowing,
            userCoinsBalance = currentCoins,
            comments = listOf(
                LiveComment(
                    id = UUID.randomUUID().toString(),
                    senderId = "system",
                    senderName = "ADT Pro Direct",
                    senderAvatar = "ADT",
                    badge = "🔒 Direct Certifié",
                    badgeColorHex = 0xFF00A884,
                    text = "Bienvenue dans le direct de ${stream.hostName}. Glissez vers le haut pour le direct suivant, ou tapez 2 fois pour aimer !",
                    isSystem = true
                )
            )
        )

        startSimulationLoop(isHost = false)
    }

    /**
     * Switch to next live stream (Swipe UP in TikTok)
     */
    fun switchToNextStream() {
        val current = _activeSession.value ?: return
        if (current.isHost) return // Host does not swipe to other streams
        val streams = _availableStreams.value
        if (streams.isEmpty()) return
        val currentIndex = streams.indexOfFirst { it.id == current.stream.id }
        val nextIndex = if (currentIndex != -1 && currentIndex + 1 < streams.size) currentIndex + 1 else 0
        watchStream(streams[nextIndex].id)
    }

    /**
     * Switch to previous live stream (Swipe DOWN in TikTok)
     */
    fun switchToPreviousStream() {
        val current = _activeSession.value ?: return
        if (current.isHost) return
        val streams = _availableStreams.value
        if (streams.isEmpty()) return
        val currentIndex = streams.indexOfFirst { it.id == current.stream.id }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else streams.size - 1
        watchStream(streams[prevIndex].id)
    }

    /**
     * Recharge free trial coins
     */
    fun rechargeCoins(amount: Int = 500) {
        _activeSession.value?.let { current ->
            val updatedCoins = current.userCoinsBalance + amount
            val rechargeComment = LiveComment(
                id = UUID.randomUUID().toString(),
                senderId = "system",
                senderName = "Portefeuille ADT",
                senderAvatar = "🪙",
                badge = "🪙 Pièces",
                badgeColorHex = 0xFFFFD700,
                text = "Recharge réussie ! +$amount pièces créditées (Solde : $updatedCoins 🪙)",
                isSystem = true
            )
            _activeSession.value = current.copy(
                userCoinsBalance = updatedCoins,
                comments = current.comments + rechargeComment
            )
        }
    }

    private fun startSimulationLoop(isHost: Boolean) {
        liveJob?.cancel()
        liveJob = coroutineScope.launch {
            var counter = 0
            while (isActive) {
                delay(1200)
                counter++

                _activeSession.value?.let { current ->
                    // 1. Update duration and stats
                    val newDuration = current.durationSeconds + 1
                    val viewerDrift = if (isHost) {
                        (current.viewerCount + Random.nextInt(2, 9)).coerceAtMost(5500)
                    } else {
                        (current.viewerCount + Random.nextInt(-3, 6)).coerceAtLeast(40)
                    }
                    val likesIncrement = Random.nextInt(3, 18)

                    // 2. Incoming comments periodically
                    val updatedComments = current.comments.toMutableList()
                    if (counter % 2 == 0) {
                        val (randName, randMsg) = sampleCommentsPool.random()
                        val randBadge = if (Random.nextBoolean()) userBadges.random() else null
                        updatedComments.add(
                            LiveComment(
                                id = UUID.randomUUID().toString(),
                                senderId = "user_$counter",
                                senderName = randName,
                                senderAvatar = randName,
                                badge = randBadge,
                                badgeColorHex = when (randBadge) {
                                    "👑 Top Fan" -> 0xFFFFD700
                                    "🛡️ Modérateur" -> 0xFF25D366
                                    "🌟 VIP" -> 0xFFFE2C55
                                    else -> 0xFF38BDF8
                                },
                                text = randMsg
                            )
                        )
                        // Keep last 40 comments for performance
                        if (updatedComments.size > 40) {
                            updatedComments.removeAt(0)
                        }
                    }

                    // 3. Spontaneous viewers joining
                    if (counter % 7 == 0) {
                        val joiner = listOf("Awa", "Ibrahim", "Mariam", "Mamadou", "Ousmane", "Khady", "Samba", "Fanta").random()
                        updatedComments.add(
                            LiveComment(
                                id = UUID.randomUUID().toString(),
                                senderId = "system_join",
                                senderName = joiner,
                                senderAvatar = joiner,
                                text = "$joiner a rejoint le direct 👋",
                                isSystem = true
                            )
                        )
                    }

                    // 4. Random like bursts
                    val hearts = current.floatingHearts.toMutableList()
                    if (Random.nextInt(10) > 3) {
                        hearts.add(
                            LiveHeartBurst(
                                id = UUID.randomUUID().toString(),
                                xOffset = 0.7f + Random.nextFloat() * 0.25f,
                                colorHex = listOf(0xFFFE2C55, 0xFFFF007F, 0xFFFFD700, 0xFF00F2FE, 0xFF25D366).random(),
                                emoji = listOf("❤️", "💖", "🔥", "✨", "💎", "👑").random()
                            )
                        )
                    }
                    if (hearts.size > 15) {
                        hearts.removeAt(0)
                    }

                    // 5. Spontaneous gifts from fans
                    var giftEvent = current.activeGiftEvent
                    var newDiamonds = current.diamondsEarned
                    if (counter % 10 == 0) {
                        val gift = DEFAULT_TIKTOK_GIFTS.take(6).random()
                        val gifter = listOf("Ndeye", "Modou", "Fatou VIP", "Abdou", "Salif", "Koffi").random()
                        giftEvent = LiveGiftEvent(
                            id = UUID.randomUUID().toString(),
                            gift = gift,
                            senderName = gifter,
                            senderAvatar = gifter,
                            combo = Random.nextInt(1, 6)
                        )
                        newDiamonds += gift.coins
                    }

                    // 6. PK Battle progress
                    val newPkSec = if (current.isPkBattleActive && current.pkRemainingSeconds > 0) current.pkRemainingSeconds - 1 else current.pkRemainingSeconds
                    val newPkHost = if (current.isPkBattleActive) current.pkHostScore + Random.nextInt(0, 15) else current.pkHostScore
                    val newPkOpp = if (current.isPkBattleActive) current.pkOpponentScore + Random.nextInt(0, 12) else current.pkOpponentScore

                    _activeSession.value = current.copy(
                        durationSeconds = newDuration,
                        viewerCount = viewerDrift,
                        likesCount = current.likesCount + likesIncrement,
                        diamondsEarned = newDiamonds,
                        comments = updatedComments,
                        floatingHearts = hearts,
                        activeGiftEvent = giftEvent,
                        pkRemainingSeconds = newPkSec,
                        pkHostScore = newPkHost,
                        pkOpponentScore = newPkOpp
                    )
                }
            }
        }
    }

    /**
     * User sends a comment in the live chat
     */
    fun sendComment(senderName: String, senderAvatar: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        _activeSession.value?.let { current ->
            val newComment = LiveComment(
                id = UUID.randomUUID().toString(),
                senderId = "me",
                senderName = senderName,
                senderAvatar = senderAvatar,
                badge = if (current.isHost) "⭐ Hôte" else "💬 Vous",
                badgeColorHex = if (current.isHost) 0xFFFE2C55 else 0xFF25D366,
                text = trimmed
            )
            val updated = current.comments + newComment
            _activeSession.value = current.copy(comments = updated)
        }
    }

    /**
     * Send a TikTok style like burst (on tapping screen)
     */
    fun sendLike(xPercent: Float = 0.8f) {
        _activeSession.value?.let { current ->
            val heart = LiveHeartBurst(
                id = UUID.randomUUID().toString(),
                xOffset = xPercent.coerceIn(0.6f, 0.95f),
                colorHex = listOf(0xFFFE2C55, 0xFFFF0055, 0xFFFFC107, 0xFF00E5FF).random(),
                emoji = listOf("❤️", "💖", "🔥", "✨", "🌟", "🎉").random()
            )
            val updatedHearts = (current.floatingHearts + heart).takeLast(20)
            _activeSession.value = current.copy(
                likesCount = current.likesCount + 1,
                floatingHearts = updatedHearts
            )
        }
    }

    /**
     * Send a gift with coins
     */
    fun sendGift(gift: LiveGift, senderName: String, senderAvatar: String) {
        _activeSession.value?.let { current ->
            if (current.userCoinsBalance >= gift.coins) {
                val updatedBalance = current.userCoinsBalance - gift.coins
                val prevCombo = if (current.activeGiftEvent?.gift?.id == gift.id) current.activeGiftEvent.combo + 1 else 1
                val event = LiveGiftEvent(
                    id = UUID.randomUUID().toString(),
                    gift = gift,
                    senderName = senderName,
                    senderAvatar = senderAvatar,
                    combo = prevCombo
                )
                val giftComment = LiveComment(
                    id = UUID.randomUUID().toString(),
                    senderId = "me",
                    senderName = senderName,
                    senderAvatar = senderAvatar,
                    badge = "🎁 Cadeau",
                    badgeColorHex = 0xFFFFD700,
                    text = "a envoyé ${gift.name} ${gift.icon} (x$prevCombo) !",
                    isSystem = true
                )
                _activeSession.value = current.copy(
                    userCoinsBalance = updatedBalance,
                    diamondsEarned = current.diamondsEarned + gift.coins,
                    activeGiftEvent = event,
                    comments = current.comments + giftComment
                )
            }
        }
    }

    /**
     * Follow/Unfollow host
     */
    fun toggleFollowHost() {
        _activeSession.value?.let { current ->
            val newFollowing = !current.isFollowing
            _activeSession.value = current.copy(isFollowing = newFollowing)
            if (newFollowing) {
                val followComment = LiveComment(
                    id = UUID.randomUUID().toString(),
                    senderId = "me",
                    senderName = "Vous",
                    senderAvatar = "Moi",
                    badge = "⭐ Nouvel abonné",
                    badgeColorHex = 0xFF25D366,
                    text = "Vous vous êtes abonné(e) à ${current.stream.hostName} !",
                    isSystem = true
                )
                _activeSession.value = _activeSession.value?.copy(
                    comments = current.comments + followComment
                )
            }
        }
    }

    /**
     * Host camera toggle (front / back)
     */
    fun toggleCamera() {
        _activeSession.value?.let { current ->
            _activeSession.value = current.copy(isFrontCamera = !current.isFrontCamera)
        }
    }

    /**
     * Host mic toggle
     */
    fun toggleMic() {
        _activeSession.value?.let { current ->
            _activeSession.value = current.copy(isMicMuted = !current.isMicMuted)
        }
    }

    /**
     * Set visual filter
     */
    fun setFilter(filterName: String) {
        _activeSession.value?.let { current ->
            _activeSession.value = current.copy(activeFilter = filterName)
        }
    }

    /**
     * Set beauty level
     */
    fun setBeautyLevel(level: Float) {
        _activeSession.value?.let { current ->
            _activeSession.value = current.copy(beautyLevel = level.coerceIn(0f, 1f))
        }
    }

    /**
     * Start / Stop PK Battle challenge
     */
    fun togglePkBattle() {
        _activeSession.value?.let { current ->
            _activeSession.value = current.copy(
                isPkBattleActive = !current.isPkBattleActive,
                pkRemainingSeconds = 120,
                pkHostScore = 800,
                pkOpponentScore = 750
            )
        }
    }

    /**
     * Trigger end of stream summary
     */
    fun showEndSummaryDialog() {
        _activeSession.value?.let { current ->
            liveJob?.cancel()
            _activeSession.value = current.copy(showEndSummary = true)
        }
    }

    /**
     * Close and reset session
     */
    fun closeSession() {
        liveJob?.cancel()
        liveJob = null
        _activeSession.value = null
    }
}
