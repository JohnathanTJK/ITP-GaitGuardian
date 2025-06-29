package com.example.gaitguardian.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.gaitguardian.viewmodels.ClinicianViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavHostController, clinicianViewModel: ClinicianViewModel) {
    val currentUserView by clinicianViewModel.getCurrentUserView.collectAsState(initial = "")

    LaunchedEffect(currentUserView) {
        delay(4000)  // 4 seconds splash
        when (currentUserView) {
            "clinician" -> {
                navController.navigate("clinician_graph") {
                    popUpTo("splash_screen") { inclusive = true }
                }
            }
            "patient" -> {
                navController.navigate("patient_graph") {
                    popUpTo("splash_screen") { inclusive = true }
                }
            }
            else -> {
                navController.navigate("start_screen") {
                    popUpTo("splash_screen") { inclusive = true }
                }
            }
        }
    }

    // UI Content of SplashScreen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF6DD)),  // light yellowish bg
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "GaitGuardian",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE18F00)
        )
    }
}
