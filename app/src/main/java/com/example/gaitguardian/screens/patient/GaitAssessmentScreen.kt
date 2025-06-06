package com.example.gaitguardian.screens.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController


// GaitAssessmentScreen
@Composable
fun GaitAssessmentScreen(navController: NavController, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Gait Assessment Screen")

        Button(
            onClick = { navController.navigate("assessment_info_screen/Timed%20Up%20and%20Go") },
            modifier = modifier.fillMaxWidth()
        ) {
            Text("Timed Up and Go")
        }
        Button(
            onClick = { navController.navigate("assessment_info_screen/Five%20Times%20Sit%20to%20Stand") },
            modifier = modifier.fillMaxWidth()
        ) {
            Text("Five Times Sit to Stand")
        }
        Button(
            onClick = { navController.popBackStack() },
            modifier = modifier.fillMaxWidth()
        ) {
            Text("Go Back")
        }
    }
}

