package com.example.data.service

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Utility to schedule and manage periodic and on-demand background backups of encrypted chats
 * to Firebase Storage using Android's native JobScheduler and Background Service.
 */
object ChatBackupScheduler {

    private const val TAG = "ChatBackupScheduler"
    const val JOB_ID = ChatBackupJobService.JOB_ID

    /**
     * Schedule periodic background backup according to user settings
     */
    fun schedulePeriodicBackup(
        context: Context,
        frequency: String,
        wifiOnly: Boolean
    ) {
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler
        if (jobScheduler == null) {
            Log.e(TAG, "JobScheduler service unavailable")
            return
        }

        if (frequency == "OFF") {
            Log.i(TAG, "Cancelling scheduled backup (frequency is OFF)")
            jobScheduler.cancel(JOB_ID)
            return
        }

        val componentName = ComponentName(context, ChatBackupJobService::class.java)

        // Convert frequency to milliseconds
        val intervalMillis: Long = when (frequency) {
            "DAILY" -> 24 * 60 * 60 * 1000L
            "WEEKLY" -> 7 * 24 * 60 * 60 * 1000L
            "MONTHLY" -> 30 * 24 * 60 * 60 * 1000L
            else -> 24 * 60 * 60 * 1000L
        }

        val networkType = if (wifiOnly) {
            JobInfo.NETWORK_TYPE_UNMETERED
        } else {
            JobInfo.NETWORK_TYPE_ANY
        }

        val builder = JobInfo.Builder(JOB_ID, componentName)
            .setRequiredNetworkType(networkType)
            .setPeriodic(intervalMillis)
            .setPersisted(true) // Persist across device reboots (requires RECEIVE_BOOT_COMPLETED)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setRequiresBatteryNotLow(true)
        }

        try {
            val resultCode = jobScheduler.schedule(builder.build())
            if (resultCode == JobScheduler.RESULT_SUCCESS) {
                Log.i(TAG, "Successfully scheduled periodic backup: freq=$frequency, wifiOnly=$wifiOnly, interval=${intervalMillis / (1000 * 60 * 60)}h")
            } else {
                Log.w(TAG, "Failed to schedule periodic backup job: code=$resultCode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling job with JobScheduler: ${e.message}", e)
        }
    }

    /**
     * Cancel periodic background backup job
     */
    fun cancelPeriodicBackup(context: Context) {
        try {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler
            jobScheduler?.cancel(JOB_ID)
            Log.i(TAG, "Cancelled JobScheduler backup job")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling backup job: ${e.message}")
        }
    }

    /**
     * Trigger an immediate background backup via ChatBackupBackgroundService
     */
    fun triggerImmediateBackgroundBackup(
        context: Context,
        userPhone: String? = null,
        includeMedia: Boolean? = null,
        wifiOnly: Boolean? = null
    ) {
        Log.i(TAG, "Triggering immediate background backup service...")
        ChatBackupBackgroundService.startBackup(
            context = context,
            userPhone = userPhone,
            includeMedia = includeMedia,
            wifiOnly = wifiOnly
        )
    }

    /**
     * Read current app settings from Room and configure JobScheduler accordingly
     */
    fun scheduleFromSettings(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val settings = database.settingsDao().getSettings()
                if (settings != null) {
                    Log.i(TAG, "Configuring scheduler from settings: freq=${settings.autoBackupFrequency}, wifi=${settings.backupOnlyWifi}")
                    schedulePeriodicBackup(
                        context = context,
                        frequency = settings.autoBackupFrequency,
                        wifiOnly = settings.backupOnlyWifi
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to schedule from settings: ${e.message}")
            }
        }
    }

    /**
     * Adjust periodic JobScheduler background sync according to battery energy mode and polling interval
     */
    fun adjustBackgroundScheduleForBatteryMode(
        context: Context,
        energyMode: com.example.data.model.SyncEnergyMode,
        intervalMinutes: Int
    ) {
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
        try {
            val componentName = ComponentName(context, ChatBackupJobService::class.java)
            // Android JobScheduler requires a minimum of 15 minutes for periodic jobs
            val periodicMillis = (intervalMinutes.coerceAtLeast(15) * 60 * 1000L)

            val builder = JobInfo.Builder(JOB_ID, componentName)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(periodicMillis)
                .setPersisted(true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setRequiresBatteryNotLow(energyMode == com.example.data.model.SyncEnergyMode.BATTERY_SAVER)
            }

            val resultCode = jobScheduler.schedule(builder.build())
            Log.i(TAG, "JobScheduler background sync adjusted for ${energyMode.name} at ${periodicMillis / 60000}m: result=$resultCode")
        } catch (e: Exception) {
            Log.w(TAG, "Error adjusting background sync schedule for battery mode: ${e.message}")
        }
    }

    /**
     * Check if the backup job is currently scheduled with JobScheduler
     */
    fun isJobScheduled(context: Context): Boolean {
        return try {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler
            jobScheduler?.allPendingJobs?.any { it.id == JOB_ID } ?: false
        } catch (e: Exception) {
            false
        }
    }
}
