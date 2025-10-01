package com.example.gaitguardian

import android.app.NotificationChannel
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.gaitguardian.ui.theme.GaitGuardianTheme
import com.example.gaitguardian.viewmodels.ClinicianViewModel
import com.example.gaitguardian.viewmodels.PatientViewModel
import com.example.gaitguardian.viewmodels.TugDataViewModel
import com.example.gaitguardian.api.TestApiConnection
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val _assessmentId = mutableStateOf<Int?>(null)

    // Initialize ViewModels at the top
    private val patientViewModel by lazy {
        ViewModelProvider(
            this,
            PatientViewModel.PatientViewModelFactory(
                (application as GaitGuardian).patientRepository,
                (application as GaitGuardian).appPreferencesRepository
            )
        )[PatientViewModel::class.java]
    }

    private val clinicianViewModel by lazy {
        ViewModelProvider(
            this,
            ClinicianViewModel.ClinicianViewModelFactory(
                (application as GaitGuardian).clinicianRepository,
                (application as GaitGuardian).appPreferencesRepository
            )
        )[ClinicianViewModel::class.java]
    }

    private val tugDataViewModel by lazy {
        ViewModelProvider(
            this,
            TugDataViewModel.TugDataViewModelFactory(
                (application as GaitGuardian).tugRepository,
                (application as GaitGuardian).appPreferencesRepository
            )
        )[TugDataViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        TestApiConnection.testConnection(this)

        if (!hasRequiredPermissions()) {
            ActivityCompat.requestPermissions(this, CAMERAX_PERMISSIONS, 0)
        }

        createNotificationChannel()
        requestNotificationPermissionIfNeeded()

        _assessmentId.value = intent?.getIntExtra("assessmentId", -1)?.takeIf { it != -1 }

        // ✅ handle notification tap (cold start)
        handleNotificationIntent(intent)

        setContent {
            GaitGuardianTheme {
                val navController = rememberNavController()
                NavGraph(
                    navController = navController,
                    initialId = _assessmentId.value,
                    patientViewModel = patientViewModel,
                    clinicianViewModel = clinicianViewModel,
                    tugDataViewModel = tugDataViewModel
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        _assessmentId.value = intent.getIntExtra("assessmentId", -1).takeIf { it != -1 }

        // ✅ handle notification tap (warm start)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val tappedId = intent?.getIntExtra("clearNotificationId", -1) ?: -1
        if (tappedId != -1) {
            lifecycleScope.launch {
                tugDataViewModel.clearAssessmentIDsforNotifications(tappedId)
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return CAMERAX_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(applicationContext, it) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NotificationService.SEVERITY_ALERT_CHANNEL_ID,
                "GaitGuardian",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Used to notify when condition deteriorates"
            }
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                1001
            )
        }
    }

    companion object {
        private val CAMERAX_PERMISSIONS = arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO
        )
    }
}

