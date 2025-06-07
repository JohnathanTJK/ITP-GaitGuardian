package com.example.gaitguardian

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.example.gaitguardian.screens.StartScreen
import com.example.gaitguardian.screens.clinician.ClinicianDetailedPatientViewScreen
import com.example.gaitguardian.screens.clinician.ClinicianHomeScreen
import com.example.gaitguardian.screens.patient.FtfsAssessmentScreen
import com.example.gaitguardian.screens.patient.GaitAssessmentScreen
import com.example.gaitguardian.screens.patient.PatientHomeScreen
import com.example.gaitguardian.screens.patient.TugAssessmentScreen
import com.example.gaitguardian.screens.patient.AssessmentInfoScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.gaitguardian.screens.patient.VideoCaptureScreen


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
            composable("clinician_detailed_patient_view_screen") {
                ClinicianDetailedPatientViewScreen(navController)
            }
        }

        // Patient-Specific Screens here
        navigation(
            startDestination = "patient_home_screen",  route = "patient_graph")
        {
            composable("patient_home_screen") {
                PatientHomeScreen(navController)
            }
            composable(
                route = "assessment_info_screen/{assessmentTitle}",
                arguments = listOf(navArgument("assessmentTitle") { type = NavType.StringType })
            ) { backStackEntry ->
                AssessmentInfoScreen(
                    navController = navController,
                    modifier = Modifier,
                    assessmentTitle = backStackEntry.arguments?.getString("assessmentTitle")
                        ?: "Assessment"
                )
            }
            composable("gait_assessment_screen") {
                GaitAssessmentScreen(navController)
            }
            composable("tug_assessment_screen") {
                TugAssessmentScreen(navController)
            }
            composable("ftfs_assessment_screen") {
                FtfsAssessmentScreen(navController)
            }
            composable("video_capture_screen") {
                VideoCaptureScreen(navController)

            }
        }


    }
}