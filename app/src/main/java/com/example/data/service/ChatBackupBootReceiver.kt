package com.example.data.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver to restore automatic background backup schedule when the device reboots
 * or after an app update.
 */
class ChatBackupBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val action = intent.action
        Log.i("ChatBackupBootReceiver", "Received broadcast: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            ChatBackupNotificationHelper.createNotificationChannel(context)
            ChatBackupScheduler.scheduleFromSettings(context)
        }
    }
}
