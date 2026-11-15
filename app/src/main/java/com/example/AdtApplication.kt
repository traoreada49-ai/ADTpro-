package com.example

import android.app.Application
import android.util.Log
import com.example.data.service.ChatBackupNotificationHelper
import com.example.data.service.ChatBackupScheduler
import com.google.firebase.FirebaseApp

class AdtApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initFirebase()
        initBackupService()
    }

    private fun initBackupService() {
        try {
            ChatBackupNotificationHelper.createNotificationChannel(this)
            ChatBackupScheduler.scheduleFromSettings(this)
            Log.i("AdtApplication", "ChatBackupNotificationHelper and Scheduler initialized")
        } catch (e: Exception) {
            Log.w("AdtApplication", "Could not initialize backup scheduler: ${e.message}")
        }
    }

    private fun initFirebase() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                Log.i("AdtApplication", "FirebaseApp initialized with default resources")
            } else {
                Log.i("AdtApplication", "FirebaseApp already initialized automatically")
            }
        } catch (e: Exception) {
            Log.e("AdtApplication", "Failed to initialize FirebaseApp: ${e.message}", e)
        }
    }
}

