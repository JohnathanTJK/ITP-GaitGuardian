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

    companion object {
        const val SEVERITY_ALERT_CHANNEL_ID = "severity_alert_channel"
    }

    fun showCompleteVideoNotification(isSuccess: Boolean, errorMessage: String? = null)
    {
        val destination = if (isSuccess) {
            "result_screen"
        } else {
            "loading_screen?errorMessage=$errorMessage"
        }
        val contentText = if (isSuccess) {
            "Your video has been processed successfully. Tap to view results."
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
            .setContentTitle("Video Processing Complete")
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
