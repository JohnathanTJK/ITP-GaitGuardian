package com.example.gaitguardian.screens.patient

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun PatientHomeScreen(navController: NavController, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Patient Home Screen")
        Button(
            onClick = { navController.navigate("gait_assessment_screen") },
            modifier = modifier.fillMaxWidth()
        ) {
            Text("Do Gait Assessment")
        }
        Button(
            onClick = { navController.popBackStack() },
            modifier = modifier.fillMaxWidth()
        ) {
            Text("Go Back")
        }
    }
}