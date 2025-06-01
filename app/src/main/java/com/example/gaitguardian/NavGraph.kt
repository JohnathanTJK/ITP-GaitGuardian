package com.example.gaitguardian

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.example.gaitguardian.screens.StartScreen
import com.example.gaitguardian.screens.clinician.ClinicianHomeScreen
import com.example.gaitguardian.screens.patient.PatientHomeScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "start_screen",
        modifier = modifier
    ) {
        // General Screens here
        composable("start_screen")
        {
            StartScreen(navController)
        }

        // Clinician-Specific Screens here
        navigation(
            startDestination = "clinician_home_screen",  route = "clinician_graph")
        {
            composable("clinician_home_screen") {
                ClinicianHomeScreen(navController)
            }
        }

        // Patient-Specific Screens here
        navigation(
            startDestination = "patient_home_screen",  route = "patient_graph")
        {
            composable("patient_home_screen") {
                PatientHomeScreen(navController)
            }
        }
    }
}