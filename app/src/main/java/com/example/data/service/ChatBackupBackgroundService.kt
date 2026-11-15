package com.example.data.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.data.firebase.BackgroundBackupStatus
import com.example.data.firebase.FirebaseBackupManager
import com.example.data.local.AppDatabase
import kotlinx.coroutines.*

/**
 * Android Foreground / Background Service to securely synchronize and back up
 * end-to-end encrypted chat histories to Firebase Cloud Storage.
 *
 * Runs seamlessly in the background even when the user leaves or closes the app.
 */
class ChatBackupBackgroundService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentBackupJob: Job? = null

    companion object {
        private const val TAG = "ChatBackupService"
        const val ACTION_START_BACKUP = "com.example.action.START_CHAT_BACKUP"
        const val ACTION_CANCEL_BACKUP = "com.example.action.CANCEL_CHAT_BACKUP"

        const val EXTRA_USER_PHONE = "extra_user_phone"
        const val EXTRA_INCLUDE_MEDIA = "extra_include_media"
        const val EXTRA_WIFI_ONLY = "extra_wifi_only"

        fun startBackup(
            context: Context,
            userPhone: String? = null,
            includeMedia: Boolean? = null,
            wifiOnly: Boolean? = null
        ) {
            val intent = Intent(context, ChatBackupBackgroundService::class.java).apply {
                action = ACTION_START_BACKUP
                if (userPhone != null) putExtra(EXTRA_USER_PHONE, userPhone)
                if (includeMedia != null) putExtra(EXTRA_INCLUDE_MEDIA, includeMedia)
                if (wifiOnly != null) putExtra(EXTRA_WIFI_ONLY, wifiOnly)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun cancelBackup(context: Context) {
            val intent = Intent(context, ChatBackupBackgroundService::class.java).apply {
                action = ACTION_CANCEL_BACKUP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        ChatBackupNotificationHelper.createNotificationChannel(this)
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_BACKUP

        when (action) {
            ACTION_CANCEL_BACKUP -> {
                Log.i(TAG, "Cancel backup requested")
                currentBackupJob?.cancel()
                FirebaseBackupManager.updateBackgroundStatus(
                    FirebaseBackupManager.backgroundStatus.value.copy(
                        isRunning = false,
                        stage = "Sauvegarde annulée",
                        lastSyncResult = "Annulée par l'utilisateur"
                    )
                )
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_START_BACKUP -> {
                val userPhoneExtra = intent?.getStringExtra(EXTRA_USER_PHONE)
                val includeMediaExtra = intent?.getBooleanExtra(EXTRA_INCLUDE_MEDIA, false)
                val wifiOnlyExtra = intent?.getBooleanExtra(EXTRA_WIFI_ONLY, true)
                startBackgroundBackup(userPhoneExtra, includeMediaExtra, wifiOnlyExtra)
            }
        }

        return START_NOT_STICKY
    }

    private fun startBackgroundBackup(
        userPhoneExtra: String?,
        includeMediaExtra: Boolean?,
        wifiOnlyExtra: Boolean?
    ) {
        // Initial Notification
        val initialNotification = ChatBackupNotificationHelper.buildProgressNotification(
            this,
            5,
            "Initialisation du service de sauvegarde chiffrée..."
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    ChatBackupNotificationHelper.NOTIFICATION_ID_PROGRESS,
                    initialNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(ChatBackupNotificationHelper.NOTIFICATION_ID_PROGRESS, initialNotification)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not startForeground with dataSync: ${e.message}")
        }

        currentBackupJob = serviceScope.launch {
            try {
                val database = AppDatabase.getDatabase(applicationContext)
                val settings = database.settingsDao().getSettings()
                val user = database.userDao().getUser()

                val phone = userPhoneExtra
                    ?: user?.phone
                    ?: "+221770001122"

                val syncPrefs = applicationContext.getSharedPreferences("adt_sync_energy_prefs", Context.MODE_PRIVATE)
                val energyMode = com.example.data.model.SyncEnergyMode.fromId(
                    syncPrefs.getString("sync_energy_mode", com.example.data.model.SyncEnergyMode.HIGH_PERFORMANCE.id)
                )
                val isBatterySaver = energyMode == com.example.data.model.SyncEnergyMode.BATTERY_SAVER

                // In Battery Saver mode, preserve battery and radio by omitting heavy media unless explicit or on Wi-Fi
                val includeMedia = if (isBatterySaver && !FirebaseBackupManager.isWifiConnected(applicationContext)) {
                    false
                } else {
                    includeMediaExtra ?: settings?.backupIncludeMedia ?: false
                }

                val wifiOnly = wifiOnlyExtra
                    ?: settings?.backupOnlyWifi
                    ?: false

                Log.i(TAG, "Executing background service for phone: $phone (energyMode: ${energyMode.name}, wifiOnly: $wifiOnly, media: $includeMedia)")

                // Perform background network message sync poll
                try {
                    val syncManager = com.example.data.firebase.FirestoreSyncManager(applicationContext, database, this)
                    syncManager.triggerManualSync(phone)
                } catch (e: Exception) {
                    Log.d(TAG, "Network poll in background service note: ${e.message}")
                }

                val notificationManager = NotificationManagerCompat.from(applicationContext)

                val result = FirebaseBackupManager.executeDirectBackup(
                    context = applicationContext,
                    database = database,
                    userPhone = phone,
                    includeMedia = includeMedia,
                    wifiOnly = wifiOnly,
                    onProgress = { progressFraction, stage ->
                        val percent = (progressFraction * 100).toInt()
                        try {
                            val notif = ChatBackupNotificationHelper.buildProgressNotification(
                                applicationContext,
                                percent,
                                stage
                            )
                            notificationManager.notify(
                                ChatBackupNotificationHelper.NOTIFICATION_ID_PROGRESS,
                                notif
                            )
                        } catch (e: Exception) {
                            Log.d(TAG, "Notification update skipped: ${e.message}")
                        }
                    }
                )

                if (result.isSuccess) {
                    val metadata = result.getOrThrow()
                    Log.i(TAG, "Background backup successful: ${metadata.formattedSize}, ${metadata.messagesCount} messages")
                    ChatBackupNotificationHelper.showSuccessNotification(
                        applicationContext,
                        formattedSize = metadata.formattedSize,
                        messagesCount = metadata.messagesCount,
                        chatsCount = metadata.chatsCount
                    )
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Erreur de synchronisation"
                    Log.e(TAG, "Background backup failed: $error")
                    ChatBackupNotificationHelper.showErrorNotification(applicationContext, error)
                }
            } catch (e: CancellationException) {
                Log.i(TAG, "Backup coroutine was cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Fatal error in background backup service: ${e.message}", e)
                ChatBackupNotificationHelper.showErrorNotification(
                    applicationContext,
                    e.message ?: "Erreur inconnue"
                )
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "adtpro:ChatBackupWakeLock")?.apply {
                // 10-minute safe timeout
                acquire(10 * 60 * 1000L)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to acquire wake lock: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing wake lock: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        releaseWakeLock()
        ChatBackupNotificationHelper.cancelProgressNotification(this)
        Log.i(TAG, "ChatBackupBackgroundService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
