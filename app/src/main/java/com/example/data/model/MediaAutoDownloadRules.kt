package com.example.data.model

/**
 * Types de connexion réseau pour les règles de téléchargement.
 */
enum class NetworkConnectionType(val label: String, val icon: String) {
    WIFI("Wi-Fi", "📶"),
    MOBILE_DATA("Données mobiles", "📱"),
    ROAMING("En itinérance (Roaming)", "✈️")
}

/**
 * Préréglages recommandés pour le téléchargement automatique des médias.
 */
enum class MediaDownloadPreset(val id: String, val title: String, val description: String) {
    WIFI_ONLY("WIFI_ONLY", "Uniquement en Wi-Fi", "Télécharge les photos, vidéos et documents lourds uniquement en Wi-Fi pour économiser le forfait."),
    BATTERY_SAVER_OFF("BATTERY_SAVER_OFF", "Désactivé en mode économie", "Suspend automatiquement tout téléchargement dès que le mode économiseur d'énergie est actif."),
    NEVER("NEVER", "Toujours manuel (Éco Max)", "Aucun média n'est téléchargé sans un clic explicite de l'utilisateur."),
    ALL_NETWORKS("ALL_NETWORKS", "Toutes connexions", "Téléchargement immédiat sur Wi-Fi et réseau cellulaire."),
    CUSTOM("CUSTOM", "Personnalisé", "Règles ajustées sur mesure par média et par réseau.")
}

/**
 * Qualité vidéo autorisée en téléchargement automatique pour brider la bande passante et l'énergie.
 */
enum class VideoQualityThrottle(val id: String, val label: String, val badge: String, val description: String) {
    THUMBNAIL_ONLY("THUMBNAIL_ONLY", "Aperçu léger uniquement", "Aperçu <200Ko", "Précharge uniquement une miniature très légère pour préserver la batterie."),
    STANDARD_DEF("STANDARD_DEF", "Définition standard (SD 480p)", "SD 480p", "Compresse et limite la résolution pour un bon compromis."),
    FULL_HD("FULL_HD", "Haute Définition (HD / 1080p)", "HD 1080p", "Télécharge les vidéos en pleine qualité sans bridage.")
}

/**
 * Concurrence et bridage des flux de téléchargement pour limiter les pics de puissance radio et CPU.
 */
enum class DownloadConcurrencyThrottle(val id: String, val label: String, val maxParallel: Int, val description: String) {
    SEQUENTIAL_ECO("SEQUENTIAL_ECO", "Séquentiel Éco (1 par 1)", 1, "Télécharge un seul fichier à la fois pour éviter les pics de puissance radio et CPU."),
    PARALLEL_FAST("PARALLEL_FAST", "Parallèle Rapide (3 simultanés)", 3, "Télécharge jusqu'à 3 fichiers en simultané pour une vitesse maximale.")
}

/**
 * Configuration complète des règles de téléchargement automatique des médias avec bridage dynamique.
 */
data class MediaAutoDownloadRules(
    val preset: MediaDownloadPreset = MediaDownloadPreset.WIFI_ONLY,
    val photosOnMobile: Boolean = false,
    val photosOnWifi: Boolean = true,
    val audioOnMobile: Boolean = true,
    val audioOnWifi: Boolean = true,
    val videosOnMobile: Boolean = false,
    val videosOnWifi: Boolean = true,
    val docsOnMobile: Boolean = false,
    val docsOnWifi: Boolean = true,
    val disableWhenBatterySaverActive: Boolean = true,
    val disableOnRoaming: Boolean = true,
    val simulatedNetwork: NetworkConnectionType = NetworkConnectionType.MOBILE_DATA,
    val dataSavedMb: Int = 184,

    // NOUVEAUX THROTTLES DE BRIDAGE CONFIGURABLES
    val autoAdjustThrottlesWithEnergyMode: Boolean = true,
    val maxFileSizeMbThrottle: Int = 50,
    val videoQualityThrottle: VideoQualityThrottle = VideoQualityThrottle.FULL_HD,
    val concurrencyThrottle: DownloadConcurrencyThrottle = DownloadConcurrencyThrottle.PARALLEL_FAST,
    val highPerformanceMaxFileSizeMb: Int = 50,
    val batterySaverMaxFileSizeMb: Int = 3,
    val highPerformanceVideoQuality: VideoQualityThrottle = VideoQualityThrottle.FULL_HD,
    val batterySaverVideoQuality: VideoQualityThrottle = VideoQualityThrottle.THUMBNAIL_ONLY,
    val highPerformanceConcurrency: DownloadConcurrencyThrottle = DownloadConcurrencyThrottle.PARALLEL_FAST,
    val batterySaverConcurrency: DownloadConcurrencyThrottle = DownloadConcurrencyThrottle.SEQUENTIAL_ECO
) {
    fun getActiveMaxFileSizeMb(isBatterySaver: Boolean): Int {
        return if (autoAdjustThrottlesWithEnergyMode) {
            if (isBatterySaver) batterySaverMaxFileSizeMb else highPerformanceMaxFileSizeMb
        } else {
            maxFileSizeMbThrottle
        }
    }

    fun getActiveVideoQuality(isBatterySaver: Boolean): VideoQualityThrottle {
        return if (autoAdjustThrottlesWithEnergyMode) {
            if (isBatterySaver) batterySaverVideoQuality else highPerformanceVideoQuality
        } else {
            videoQualityThrottle
        }
    }

    fun getActiveConcurrency(isBatterySaver: Boolean): DownloadConcurrencyThrottle {
        return if (autoAdjustThrottlesWithEnergyMode) {
            if (isBatterySaver) batterySaverConcurrency else highPerformanceConcurrency
        } else {
            concurrencyThrottle
        }
    }

    /**
     * Détermine si un média doit être téléchargé automatiquement
     * en fonction du réseau, du mode économie d'énergie, de la taille et des règles.
     */
    fun shouldAutoDownload(
        mediaType: String,
        isBatterySaverActive: Boolean,
        currentNetwork: NetworkConnectionType = simulatedNetwork,
        isScheduledFullSyncActive: Boolean = false,
        fileSizeMb: Float? = null,
        isHdVideo: Boolean = false
    ): Boolean {
        // 0. Si la synchronisation complète planifiée (période de charge) est active et autorisée :
        if (isScheduledFullSyncActive) {
            return true
        }

        // 1. Si le mode économie d'énergie est actif et que l'utilisateur a activé la coupure :
        if (isBatterySaverActive && disableWhenBatterySaverActive) {
            return false
        }

        // 2. Vérification du seuil de taille de fichier bridé :
        val effectiveMaxMb = getActiveMaxFileSizeMb(isBatterySaverActive)
        if (fileSizeMb != null && fileSizeMb > effectiveMaxMb) {
            return false
        }

        // 3. Vérification de la qualité vidéo :
        val effectiveVideoQuality = getActiveVideoQuality(isBatterySaverActive)
        if (isHdVideo && effectiveVideoQuality == VideoQualityThrottle.THUMBNAIL_ONLY && mediaType.equals("VIDEO", ignoreCase = true)) {
            return false
        }

        // 4. Si le réseau est en itinérance (roaming) et bloqué :
        if (currentNetwork == NetworkConnectionType.ROAMING && disableOnRoaming) {
            return false
        }

        // 5. Préréglage manuel total :
        if (preset == MediaDownloadPreset.NEVER) {
            return false
        }

        val isWifi = currentNetwork == NetworkConnectionType.WIFI

        // 6. Règles par type de média :
        return when (mediaType.uppercase()) {
            "PHOTO", "IMAGE" -> if (isWifi) photosOnWifi else photosOnMobile
            "AUDIO", "VOICE" -> if (isWifi) audioOnWifi else audioOnMobile
            "VIDEO" -> if (isWifi) videosOnWifi else videosOnMobile
            "DOCUMENT", "DOC", "PDF" -> if (isWifi) docsOnWifi else docsOnMobile
            "STICKER" -> true
            else -> true
        }
    }

    /**
     * Retourne la raison pour laquelle un média n'est pas auto-téléchargé.
     */
    fun getReasonHeldBack(
        isBatterySaverActive: Boolean,
        currentNetwork: NetworkConnectionType = simulatedNetwork,
        scheduledWindowHint: String? = null,
        fileSizeMb: Float? = null
    ): String {
        val effectiveMaxMb = getActiveMaxFileSizeMb(isBatterySaverActive)
        if (fileSizeMb != null && fileSizeMb > effectiveMaxMb) {
            return "Bridé : Taille ($fileSizeMb Mo) > seuil auto ($effectiveMaxMb Mo) ⚡"
        }
        if (!scheduledWindowHint.isNullOrBlank()) {
            return "Reporté : Synchronisation complète planifiée ($scheduledWindowHint) en charge 🔌"
        }
        if (isBatterySaverActive && disableWhenBatterySaverActive) {
            return "Suspendu : Mode Économie d'Énergie actif 🔋"
        }
        if (currentNetwork == NetworkConnectionType.ROAMING && disableOnRoaming) {
            return "Suspendu : En itinérance (Roaming) ✈️"
        }
        if (currentNetwork == NetworkConnectionType.MOBILE_DATA) {
            return "En attente du Wi-Fi pour préserver votre batterie et forfait 📶"
        }
        if (preset == MediaDownloadPreset.NEVER) {
            return "Téléchargement manuel au clic (Éco max) 👆"
        }
        return "Touchez pour télécharger"
    }

    fun getMobileSummary(): String {
        val list = mutableListOf<String>()
        if (photosOnMobile) list.add("Photos")
        if (audioOnMobile) list.add("Audio")
        if (videosOnMobile) list.add("Vidéos")
        if (docsOnMobile) list.add("Documents")
        return when {
            list.isEmpty() -> "Aucun média (Éco Max)"
            list.size == 4 -> "Tous les médias"
            else -> list.joinToString(", ")
        }
    }

    fun getWifiSummary(): String {
        val list = mutableListOf<String>()
        if (photosOnWifi) list.add("Photos")
        if (audioOnWifi) list.add("Audio")
        if (videosOnWifi) list.add("Vidéos")
        if (docsOnWifi) list.add("Documents")
        return when {
            list.isEmpty() -> "Aucun média"
            list.size == 4 -> "Tous les médias"
            else -> list.joinToString(", ")
        }
    }

    fun getBatteryHoursGained(): String {
        return when {
            !videosOnMobile && !docsOnMobile && disableWhenBatterySaverActive -> "+4h d'autonomie"
            !videosOnMobile && !docsOnMobile -> "+3h d'autonomie"
            !videosOnMobile -> "+2h d'autonomie"
            else -> "+45min d'autonomie"
        }
    }

    fun getEnergyConsumptionLabel(): String {
        return when {
            !videosOnMobile && !docsOnMobile && !photosOnMobile -> "Consommation : Ultra-faible (Préservation maximale) 🔋"
            !videosOnMobile && !docsOnMobile -> "Consommation : Équilibrée (Recommandé) ⚡"
            else -> "Consommation : Élevée (Vidéos/Docs sur réseau mobile) ⚠️"
        }
    }

    fun getThrottleSummary(isBatterySaver: Boolean): String {
        val maxMb = getActiveMaxFileSizeMb(isBatterySaver)
        val qual = getActiveVideoQuality(isBatterySaver)
        val conc = getActiveConcurrency(isBatterySaver)
        return "Max $maxMb Mo • ${qual.badge} • ${conc.label.substringBefore(" (")}"
    }
}
