package com.example.gaitguardian

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModelProvider
//import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph
import androidx.navigation.compose.rememberNavController
import com.example.gaitguardian.ui.theme.GaitGuardianTheme
import com.example.gaitguardian.viewmodels.PatientViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize ViewModel with the Factory
        val patientViewModelFactory = PatientViewModel.PatientViewModelFactory((application as GaitGuardian).patientRepository, (application as GaitGuardian).tugRepository, (application as GaitGuardian).appPreferencesRepository)
        val patientViewModel = ViewModelProvider(this, patientViewModelFactory)[PatientViewModel::class.java]
        //TODO: Clinician ViewModel
        setContent {
            GaitGuardianTheme {
//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val navController = rememberNavController()
                    NavGraph(
                        navController = navController,
//                        modifier = Modifier.padding(innerPadding),
                        patientViewModel = patientViewModel
                    )
//                }
            }
        }
    }
}
