package com.example.util

import java.util.Calendar

/**
 * Gestionnaire intelligent de programmation automatique du Mode Sombre (Dark Mode) pour ADT Pro.
 * Permet la bascule automatique entre mode clair et mode sombre en fonction de l'heure système
 * ou d'horaires personnalisés définis par l'utilisateur (nuit, crépuscule/aube).
 */
object ThemeScheduleManager {

    const val MODE_DARK = "DARK"
    const val MODE_LIGHT = "LIGHT"
    const val MODE_SYSTEM = "SYSTEM"
    const val MODE_AUTO_SCHEDULE = "AUTO_SCHEDULE"

    const val SCHEDULE_SUNSET_TO_SUNRISE = "SUNSET_TO_SUNRISE"
    const val SCHEDULE_CUSTOM = "CUSTOM"

    const val DEFAULT_SUNSET_HOUR = 20
    const val DEFAULT_SUNSET_MINUTE = 0
    const val DEFAULT_SUNRISE_HOUR = 7
    const val DEFAULT_SUNRISE_MINUTE = 0

    data class PresetSchedule(
        val label: String,
        val description: String,
        val startHour: Int,
        val startMinute: Int,
        val endHour: Int,
        val endMinute: Int
    )

    val PRESET_SCHEDULES = listOf(
        PresetSchedule(
            label = "20:00 - 07:00",
            description = "Crépuscule standard (Recommandé)",
            startHour = 20,
            startMinute = 0,
            endHour = 7,
            endMinute = 0
        ),
        PresetSchedule(
            label = "21:30 - 06:30",
            description = "Nuit tardive & Réveil matinal",
            startHour = 21,
            startMinute = 30,
            endHour = 6,
            endMinute = 30
        ),
        PresetSchedule(
            label = "22:00 - 08:00",
            description = "Grasse matinée",
            startHour = 22,
            startMinute = 0,
            endHour = 8,
            endMinute = 0
        ),
        PresetSchedule(
            label = "19:00 - 07:00",
            description = "Hiver & Crépuscule précoce",
            startHour = 19,
            startMinute = 0,
            endHour = 7,
            endMinute = 0
        )
    )

    data class ScheduleTransitionInfo(
        val isCurrentlyDark: Boolean,
        val nextSwitchHour: Int,
        val nextSwitchMinute: Int,
        val nextModeIsDark: Boolean,
        val hoursRemaining: Int,
        val minutesRemaining: Int,
        val statusSummary: String,
        val nextActionSummary: String
    )

    /**
     * Évalue si l'instant donné (heure & minute) se trouve dans la période de nuit (Mode Sombre).
     * Gère les plages chevauchant minuit (ex: 20h00 -> 07h00).
     */
    fun isNightTime(
        currentHour: Int,
        currentMinute: Int,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int
    ): Boolean {
        val currentMin = currentHour * 60 + currentMinute
        val startMin = startHour * 60 + startMinute
        val endMin = endHour * 60 + endMinute

        return if (startMin < endMin) {
            // Plage dans la même journée (ex: 13h00 à 18h00)
            currentMin in startMin until endMin
        } else if (startMin > endMin) {
            // Plage nocturne chevauchant minuit (ex: 20h00 à 07h00)
            currentMin >= startMin || currentMin < endMin
        } else {
            // Début et fin identiques => actif 24h/24
            true
        }
    }

    /**
     * Détermine si le thème sombre doit être appliqué en tenant compte du mode choisi,
     * de la planification et de l'état système.
     */
    fun shouldUseDarkTheme(
        themeMode: String?,
        scheduleType: String? = SCHEDULE_SUNSET_TO_SUNRISE,
        startHour: Int = DEFAULT_SUNSET_HOUR,
        startMinute: Int = DEFAULT_SUNSET_MINUTE,
        endHour: Int = DEFAULT_SUNRISE_HOUR,
        endMinute: Int = DEFAULT_SUNRISE_MINUTE,
        systemInDarkTheme: Boolean = false,
        calendar: Calendar = Calendar.getInstance()
    ): Boolean {
        return when (themeMode) {
            MODE_LIGHT -> false
            MODE_DARK -> true
            MODE_SYSTEM -> systemInDarkTheme
            MODE_AUTO_SCHEDULE -> {
                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                val currentMinute = calendar.get(Calendar.MINUTE)
                val (effectiveStartH, effectiveStartM, effectiveEndH, effectiveEndM) = if (scheduleType == SCHEDULE_CUSTOM) {
                    ScheduleWindow(startHour, startMinute, endHour, endMinute)
                } else {
                    ScheduleWindow(DEFAULT_SUNSET_HOUR, DEFAULT_SUNSET_MINUTE, DEFAULT_SUNRISE_HOUR, DEFAULT_SUNRISE_MINUTE)
                }
                isNightTime(currentHour, currentMinute, effectiveStartH, effectiveStartM, effectiveEndH, effectiveEndM)
            }
            else -> systemInDarkTheme
        }
    }

    /**
     * Calcule le statut en direct et la prochaine bascule d'horaire pour l'affichage dans l'interface utilisateur.
     */
    fun computeTransitionInfo(
        scheduleType: String?,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        calendar: Calendar = Calendar.getInstance()
    ): ScheduleTransitionInfo {
        val currentH = calendar.get(Calendar.HOUR_OF_DAY)
        val currentM = calendar.get(Calendar.MINUTE)

        val (sH, sM, eH, eM) = if (scheduleType == SCHEDULE_CUSTOM) {
            ScheduleWindow(startHour, startMinute, endHour, endMinute)
        } else {
            ScheduleWindow(DEFAULT_SUNSET_HOUR, DEFAULT_SUNSET_MINUTE, DEFAULT_SUNRISE_HOUR, DEFAULT_SUNRISE_MINUTE)
        }

        val isDark = isNightTime(currentH, currentM, sH, sM, eH, eM)

        val currentTotalMinutes = currentH * 60 + currentM
        val targetTotalMinutes = if (isDark) {
            // Prochaine transition : bascule au mode clair à endHour:endMinute
            eH * 60 + eM
        } else {
            // Prochaine transition : bascule au mode sombre à startHour:startMinute
            sH * 60 + sM
        }

        val diffMinutes = if (targetTotalMinutes >= currentTotalMinutes) {
            targetTotalMinutes - currentTotalMinutes
        } else {
            (24 * 60 - currentTotalMinutes) + targetTotalMinutes
        }

        val remHours = diffMinutes / 60
        val remMins = diffMinutes % 60

        val nextH = if (isDark) eH else sH
        val nextM = if (isDark) eM else sM
        val nextIsDark = !isDark

        val formattedNextTime = formatTime(nextH, nextM)
        val timeRemainingStr = if (remHours > 0) "${remHours}h ${remMins}m" else "${remMins}m"

        val statusSummary = if (isDark) {
            "Mode sombre actif (Nuit)"
        } else {
            "Mode clair actif (Jour)"
        }

        val nextActionSummary = if (isDark) {
            "Repasse en mode clair à $formattedNextTime (dans $timeRemainingStr)"
        } else {
            "Bascule en mode sombre à $formattedNextTime (dans $timeRemainingStr)"
        }

        return ScheduleTransitionInfo(
            isCurrentlyDark = isDark,
            nextSwitchHour = nextH,
            nextSwitchMinute = nextM,
            nextModeIsDark = nextIsDark,
            hoursRemaining = remHours,
            minutesRemaining = remMins,
            statusSummary = statusSummary,
            nextActionSummary = nextActionSummary
        )
    }

    fun formatTime(hour: Int, minute: Int): String {
        return "%02d:%02d".format(hour, minute)
    }

    private data class ScheduleWindow(
        val startH: Int,
        val startM: Int,
        val endH: Int,
        val endM: Int
    )
}
