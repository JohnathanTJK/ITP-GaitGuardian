package com.example.gaitguardian

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
//import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.gaitguardian.ui.theme.GaitGuardianTheme
import com.example.gaitguardian.viewmodels.ClinicianViewModel
import com.example.gaitguardian.viewmodels.PatientViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize ViewModel with the Factory
        val patientViewModelFactory = PatientViewModel.PatientViewModelFactory((application as GaitGuardian).patientRepository, (application as GaitGuardian).tugRepository, (application as GaitGuardian).appPreferencesRepository)
        val patientViewModel = ViewModelProvider(this, patientViewModelFactory)[PatientViewModel::class.java]
        //TODO: Clinician ViewModel
        val clinicianViewModelFactory = ClinicianViewModel.ClinicianViewModelFactory((application as GaitGuardian).clinicianRepository,(application as GaitGuardian).tugRepository, (application as GaitGuardian).appPreferencesRepository)
        val clinicianViewModel = ViewModelProvider(this, clinicianViewModelFactory)[ClinicianViewModel::class.java]
        setContent {
            GaitGuardianTheme {
//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val navController = rememberNavController()
                    NavGraph(
                        navController = navController,
//                        modifier = Modifier.padding(innerPadding),
                        patientViewModel = patientViewModel,
                        clinicianViewModel = clinicianViewModel
                    )
//                }
            }
        }
    }
}
