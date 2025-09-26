package com.example.gaitguardian

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class NotificationService(
    private val context: Context
) {

    fun showNotification() {
        // Intent to open MainActivity when notification is tapped
//        val activityIntent = Intent(context, MainActivity::class.java)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("assessmentId", 2)
        }
//        val activityPendingIntent = PendingIntent.getActivity(
//            context,
//            0,
//            activityIntent,
//            PendingIntent.FLAG_IMMUTABLE
//        )

        val activityPendingIntent = PendingIntent.getActivity(
            context,
            3,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // Build the notification
        val notification = NotificationCompat.Builder(context, SEVERITY_ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Patient Condition Alert!")
            .setContentText("You have a new severity alert! Condition BAD")
            .setContentIntent(activityPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()

        // Show the notification
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(
                NOTIFICATION_ID,
                notification
            )
        }
    }

    companion object {
        const val SEVERITY_ALERT_CHANNEL_ID = "severity_alert_channel"
        private const val NOTIFICATION_ID = 1
    }
}
