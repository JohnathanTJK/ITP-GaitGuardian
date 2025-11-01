package com.example.gaitguardian

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class NotificationService(private val context: Context) {

    fun showNotification(testId: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("assessmentId", testId)
            putExtra("clearNotificationId", testId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            testId, // unique per notification
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, SEVERITY_ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Condition Change Detected")
            .setContentText("Needs review for Assessment #$testId. Tap to view details.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(testId, notification)
        }
    }

    companion object {
        const val SEVERITY_ALERT_CHANNEL_ID = "severity_alert_channel"
    }

    fun showCompleteVideoNotification(assessmentTitle: String, isSuccess: Boolean)
    {
        val destination = if (isSuccess) {
            "result_screen/$assessmentTitle"
        } else {
            "loading_screen"
        }
        val contentText = if (isSuccess) {
            "Your video for $assessmentTitle has been uploaded. Tap to view results."
        } else {
            "There was an error processing your video. Tap to try again."
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
//            putExtra("destination", "result_screen/$assessmentTitle")
            putExtra("destination", destination)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0, // unique per notification
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, SEVERITY_ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Video Upload Complete")
//            .setContentText("Your video for $assessmentTitle has been uploaded. Tap to view results.")
            .setContentText(contentText)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(1001, notification)
        }
    }
}
