package com.example.gaitguardian

import android.app.NotificationChannel
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.gaitguardian.ui.theme.GaitGuardianTheme
import com.example.gaitguardian.viewmodels.ClinicianViewModel
import com.example.gaitguardian.viewmodels.PatientViewModel
import com.example.gaitguardian.viewmodels.TugDataViewModel
import com.example.gaitguardian.api.TestApiConnection

class MainActivity : ComponentActivity() {
    private val _assessmentId = mutableStateOf<Int?>(null)
    private val _destinationFromIntent = mutableStateOf<String?>(null)

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

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        TestApiConnection.testConnection(this)

        if (!hasRequiredPermissions()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, 0)
        }

        createNotificationChannel()
        requestNotificationPermissionIfNeeded()

        _assessmentId.value = intent?.getIntExtra("assessmentId", -1)?.takeIf { it != -1 }
        _destinationFromIntent.value = intent?.getStringExtra("destination")

        // ✅ handle notification tap (cold start)
//        handleNotificationIntent(intent)

        setContent {
            GaitGuardianTheme {
                val navController = rememberNavController()
                val activity = this
                // Attach listener directly to navController
                DisposableEffect(navController) {
                    val unspecifiedScreens = setOf("video_screen", "camera_screen", "camera_test", "new_cam_screen") // screens that DO NOT enforce strict orientation
                    // check the currentdestination route, and orientate screen accordingly
                    val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
                        activity.requestedOrientation = if (destination.route in unspecifiedScreens) {
                            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        } else {
                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        }
                    }
                    navController.addOnDestinationChangedListener(listener)
                    onDispose { navController.removeOnDestinationChangedListener(listener) }
                }
//                val destinationFromIntent = intent?.getStringExtra("destination")
                val destinationFromIntent by _destinationFromIntent
                NavGraph(
                    navController = navController,
                    initialId = _assessmentId.value,
                    destinationIntent = destinationFromIntent,
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
        _destinationFromIntent.value = intent.getStringExtra("destination")
//        val destination = intent.getStringExtra("destination")
//        if (destination != null) {
//            navController.navigate(destination)
//        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun hasRequiredPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
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
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        private val REQUIRED_PERMISSIONS = arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.POST_NOTIFICATIONS
        )
    }
}

