package com.example.data.service

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.util.Log
import com.example.data.firebase.FirebaseBackupManager
import com.example.data.firebase.FirestoreSyncManager
import com.example.data.local.AppDatabase
import com.example.data.model.SyncEnergyMode
import kotlinx.coroutines.*

/**
 * Android JobService executed periodically by JobScheduler to synchronize and back up
 * chat histories automatically in the background on Firebase Storage.
 */
class ChatBackupJobService : JobService() {

    private val jobScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var activeJob: Job? = null

    companion object {
        private const val TAG = "ChatBackupJobService"
        const val JOB_ID = 10042
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.i(TAG, "ChatBackupJobService triggered by JobScheduler")

        activeJob = jobScope.launch {
            try {
                com.example.data.firebase.FirebaseInitHelper.ensureInitialized(applicationContext)
                val database = AppDatabase.getDatabase(applicationContext)

                // 1. Observe battery mode preferences & execute background network sync poll
                val syncPrefs = applicationContext.getSharedPreferences("adt_sync_energy_prefs", Context.MODE_PRIVATE)
                val energyMode = SyncEnergyMode.fromId(
                    syncPrefs.getString("sync_energy_mode", SyncEnergyMode.HIGH_PERFORMANCE.id)
                )
                val user = database.userDao().getUser()
                val phone = user?.phone ?: "+221770001122"

                try {
                    val syncManager = FirestoreSyncManager(applicationContext, database, this)
                    Log.i(TAG, "ChatBackupJobService executing background network sync poll (mode: ${energyMode.name})")
                    syncManager.triggerManualSync(phone)
                } catch (se: Exception) {
                    Log.d(TAG, "Background network sync poll note: ${se.message}")
                }

                val settings = database.settingsDao().getSettings()

                if (settings == null) {
                    Log.w(TAG, "No settings found, skipping scheduled backup")
                    jobFinished(params, false)
                    return@launch
                }

                val frequency = settings.autoBackupFrequency
                if (frequency == "OFF") {
                    Log.i(TAG, "Automatic backup is turned OFF, skipping job")
                    jobFinished(params, false)
                    return@launch
                }

                val now = System.currentTimeMillis()
                val lastBackup = settings.lastBackupTimestamp
                val intervalMillis = when (frequency) {
                    "DAILY" -> 24 * 60 * 60 * 1000L
                    "WEEKLY" -> 7 * 24 * 60 * 60 * 1000L
                    "MONTHLY" -> 30 * 24 * 60 * 60 * 1000L
                    else -> 24 * 60 * 60 * 1000L
                }

                // Check if sufficient time has passed since last backup
                val shouldRun = (lastBackup == 0L) || ((now - lastBackup) >= (intervalMillis - 60_000L))

                if (!shouldRun) {
                    Log.i(TAG, "Interval not reached yet ($lastBackup vs $now). Skipping until next cycle.")
                    jobFinished(params, false)
                    return@launch
                }

                Log.i(TAG, "Executing scheduled background backup for $phone (freq: $frequency)")

                val result = FirebaseBackupManager.executeDirectBackup(
                    context = applicationContext,
                    database = database,
                    userPhone = phone,
                    includeMedia = settings.backupIncludeMedia,
                    wifiOnly = settings.backupOnlyWifi,
                    onProgress = { progress, stage ->
                        Log.d(TAG, "Scheduled backup progress: ${(progress * 100).toInt()}% - $stage")
                    }
                )

                if (result.isSuccess) {
                    val meta = result.getOrThrow()
                    Log.i(TAG, "Scheduled background backup completed successfully: ${meta.formattedSize}")
                    ChatBackupNotificationHelper.showSuccessNotification(
                        applicationContext,
                        formattedSize = meta.formattedSize,
                        messagesCount = meta.messagesCount,
                        chatsCount = meta.chatsCount
                    )
                } else {
                    Log.w(TAG, "Scheduled background backup failed: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: CancellationException) {
                Log.i(TAG, "ChatBackupJobService coroutine was cancelled normally")
            } catch (e: Exception) {
                Log.e(TAG, "Error during scheduled job execution: ${e.message}", e)
            } finally {
                jobFinished(params, false)
            }
        }

        // Return true to indicate background work is continuing asynchronously
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Log.i(TAG, "ChatBackupJobService stopped prematurely by system")
        activeJob?.cancel()
        // Reschedule if interrupted
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        jobScope.cancel()
    }
}
