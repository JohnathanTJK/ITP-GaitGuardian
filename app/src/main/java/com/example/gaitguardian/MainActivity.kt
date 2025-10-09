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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.gaitguardian.ui.theme.GaitGuardianTheme
import com.example.gaitguardian.viewmodels.ClinicianViewModel
import com.example.gaitguardian.viewmodels.PatientViewModel
import com.example.gaitguardian.viewmodels.TugDataViewModel
import com.example.gaitguardian.api.TestApiConnection

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
                val activity = this
                // Attach listener directly to navController
                DisposableEffect(navController) {
//                    val routeOrientations = mapOf(
//                        "video_screen" to ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
//                        "clinician_home_screen" to ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
//                        "patient_screen" to ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
//                        "camera_screen" to ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
//                        "login" to ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
//                    )
                    val unspecifiedScreens = setOf("video_screen", "camera_screen") // screens that DO NOT enforce strict orientation
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
        intent.getIntExtra("assessmentId", -1)
            .takeIf { it != -1 }
            ?.let { tugDataViewModel.onNotificationReceived(it) } // if not -1, update VM
        // ✅ handle notification tap (warm start)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val tappedId = intent?.getIntExtra("clearNotificationId", -1) ?: -1
        // TODO: Uncomment when notification flow confirm works
//        if (tappedId != -1) {
//            lifecycleScope.launch {
//                tugDataViewModel.clearAssessmentIDsforNotifications(tappedId)
//            }
//        }
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

