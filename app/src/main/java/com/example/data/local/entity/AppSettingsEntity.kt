package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val language: String = "FR", // "FR" (France), "FRANGLAIS" (Franglais), "EN" (USA), "RU" (Russia), "ES" (Español), "AR" (العربية)
    val themeMode: String = "DARK", // "DARK", "LIGHT", "SYSTEM", "AUTO_SCHEDULE"
    val darkScheduleType: String = "SUNSET_TO_SUNRISE", // "SUNSET_TO_SUNRISE" (20h00 - 07h00 / Heure système), "CUSTOM"
    val darkScheduleStartHour: Int = 20, // 20:00 (Activer mode sombre)
    val darkScheduleStartMinute: Int = 0,
    val darkScheduleEndHour: Int = 7,   // 07:00 (Désactiver mode sombre / Repasser clair)
    val darkScheduleEndMinute: Int = 0,
    val wallpaperTheme: String = "EMERALD", // "EMERALD", "DOODLE", "MIDNIGHT", "SUNSET", "SAPPHIRE"
    val privacyLastSeen: String = "EVERYONE", // "EVERYONE", "CONTACTS", "NOBODY"
    val privacyPhoto: String = "EVERYONE",
    val privacyStatus: String = "CONTACTS",
    val readReceipts: Boolean = true,
    val appLockEnabled: Boolean = false,
    val operatorDataSaver: Boolean = false,
    val activeStickerPack: String = "PRO", // "PRO", "FUN", "TECH"
    val autoBackupFrequency: String = "DAILY", // "DAILY", "WEEKLY", "MONTHLY", "OFF"
    val backupOnlyWifi: Boolean = true,
    val backupIncludeMedia: Boolean = false,
    val lastBackupTimestamp: Long = 0L,
    val lastBackupSizeFormatted: String = "",
    val lastBackupMessagesCount: Int = 0
)
