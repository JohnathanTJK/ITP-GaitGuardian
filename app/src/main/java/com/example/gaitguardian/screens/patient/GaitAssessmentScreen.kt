package com.example.gaitguardian.screens.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gaitguardian.ui.theme.bgColor
import com.example.gaitguardian.ui.theme.ButtonActive
import com.example.gaitguardian.ui.theme.screenPadding
import com.example.gaitguardian.ui.theme.spacerLarge

@Composable
fun GaitAssessmentScreen(navController: NavController, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(screenPadding)
            .navigationBarsPadding(),

        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Spacer(modifier = Modifier.height(spacerLarge))

            Text(
                text = "Gait Assessments",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Choose an assessment to begin:",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(32.dp))

            // First Button
            Button(
                onClick = { navController.navigate("assessment_info_screen/Timed%20Up%20and%20Go") },
                colors = ButtonDefaults.buttonColors(containerColor = ButtonActive),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(vertical = 16.dp)
            ) {
                Text("Timed Up and Go", fontSize = 20.sp, color = Color.Black)
            }

            // Second Button
            Button(
                onClick = { navController.navigate("assessment_info_screen/Five%20Times%20Sit%20to%20Stand") },
                colors = ButtonDefaults.buttonColors(containerColor = ButtonActive),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(vertical = 16.dp)
            ) {
                Text("Five Times Sit to Stand", fontSize = 20.sp, color = Color.Black)
            }
        }

        // Third Button (Go Back) with same height & padding as above
        Button(
            onClick = { navController.popBackStack() },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(vertical = 16.dp)
        ) {
            Text("Go Back", fontSize = 20.sp, color = Color.Black)
        }
    }
}
