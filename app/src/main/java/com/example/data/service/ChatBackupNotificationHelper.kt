package com.example.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

/**
 * Helper to manage Notification Channels and Notifications for Chat Backup Background Service.
 */
object ChatBackupNotificationHelper {

    const val CHANNEL_ID = "adt_chat_backup_channel"
    const val NOTIFICATION_ID_PROGRESS = 4001
    const val NOTIFICATION_ID_RESULT = 4002

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Sauvegarde des discussions"
            val descriptionText = "Notifications de synchronisation et sauvegarde automatique des discussions chiffrées sur Firebase Storage"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun buildProgressNotification(
        context: Context,
        progressPercent: Int,
        stageMessage: String
    ): android.app.Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_SCREEN", "CHAT_BACKUP")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(context, ChatBackupBackgroundService::class.java).apply {
            action = ChatBackupBackgroundService.ACTION_CANCEL_BACKUP
        }
        val cancelPendingIntent = PendingIntent.getService(
            context,
            1,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Sauvegarde chiffrée ADT Pro")
            .setContentText(stageMessage)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Annuler", cancelPendingIntent)

        if (progressPercent in 0..100) {
            builder.setProgress(100, progressPercent, false)
        } else {
            builder.setProgress(100, 0, true)
        }

        return builder.build()
    }

    fun showSuccessNotification(
        context: Context,
        formattedSize: String,
        messagesCount: Int,
        chatsCount: Int
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_SCREEN", "CHAT_BACKUP")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Sauvegarde chiffrée réussie")
            .setContentText("$chatsCount discussions ($messagesCount messages) sauvegardées sur Firebase Storage ($formattedSize)")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Vos discussions chiffrées de bout en bout (AES-256-GCM) ont été synchronisées avec succès sur Firebase Cloud Storage.\n" +
                            "Taille: $formattedSize • $chatsCount discussions • $messagesCount messages."
                )
            )
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(NOTIFICATION_ID_RESULT, notification)
        } catch (e: SecurityException) {
            // Notification permission might not be granted
        }
    }

    fun showErrorNotification(context: Context, errorMessage: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_SCREEN", "CHAT_BACKUP")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Échec de la sauvegarde en arrière-plan")
            .setContentText(errorMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText("La synchronisation des discussions sur Firebase Storage a échoué: $errorMessage"))
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(NOTIFICATION_ID_RESULT, notification)
        } catch (e: SecurityException) {
            // Notification permission not granted
        }
    }

    fun cancelProgressNotification(context: Context) {
        try {
            val manager = NotificationManagerCompat.from(context)
            manager.cancel(NOTIFICATION_ID_PROGRESS)
        } catch (e: Exception) {
            // Ignored
        }
    }
}
