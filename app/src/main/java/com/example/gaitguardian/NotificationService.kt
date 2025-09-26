package com.example.gaitguardian

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.example.gaitguardian.viewmodels.TugDataViewModel
import kotlinx.coroutines.launch

class NotificationService(
    private val context: Context,
    private val tugDataViewModel: TugDataViewModel
) {

    fun showNotification() {
        // Launch a coroutine to read the StateFlow
        tugDataViewModel.viewModelScope.launch {
            tugDataViewModel.pendingAssessmentIds.collect { testIds ->
                testIds.forEach { testId ->
                    val intent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("assessmentId", testId) // tag the corresponding testId to the notification
                    }

                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        testId, // unqiue notification id
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
            }
        }

    }

    companion object {
        const val SEVERITY_ALERT_CHANNEL_ID = "severity_alert_channel"
    }
}

