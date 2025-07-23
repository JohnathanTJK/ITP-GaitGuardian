package com.example.gaitguardian

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
//import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.gaitguardian.ui.theme.GaitGuardianTheme
import com.example.gaitguardian.viewmodels.ClinicianViewModel
import com.example.gaitguardian.viewmodels.PatientViewModel
import com.example.gaitguardian.viewmodels.TugDataViewModel
import com.example.gaitguardian.api.TestApiConnection

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Test API connection
        TestApiConnection.testConnection()

        if (!hasRequiredPermissions()) {
            ActivityCompat.requestPermissions(
                this, CAMERAX_PERMISSIONS, 0
            )
        }
//        enableEdgeToEdge()

        // Initialize ViewModel with the Factory
        val patientViewModelFactory = PatientViewModel.PatientViewModelFactory(
            (application as GaitGuardian).patientRepository,
            (application as GaitGuardian).appPreferencesRepository
        )
        val patientViewModel =
            ViewModelProvider(this, patientViewModelFactory)[PatientViewModel::class.java]
        //TODO: Clinician ViewModel
        val clinicianViewModelFactory = ClinicianViewModel.ClinicianViewModelFactory(
            (application as GaitGuardian).clinicianRepository,
            (application as GaitGuardian).appPreferencesRepository
        )
        val clinicianViewModel =
            ViewModelProvider(this, clinicianViewModelFactory)[ClinicianViewModel::class.java]
        // TUG ViewModel
        val TugViewModelFactory = TugDataViewModel.TugDataViewModelFactory(
            (application as GaitGuardian).tugRepository
        )
        val tugDataViewModel =
            ViewModelProvider(this, TugViewModelFactory)[TugDataViewModel::class.java]
        setContent {
            GaitGuardianTheme {
//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                val navController = rememberNavController()
                NavGraph(
                    navController = navController,
//                        modifier = Modifier.padding(innerPadding),
                    patientViewModel = patientViewModel,
                    clinicianViewModel = clinicianViewModel,
                    tugDataViewModel = tugDataViewModel
                )
//                }
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return CAMERAX_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                applicationContext,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        private val CAMERAX_PERMISSIONS = arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO
        )
    }
}
