package com.example.gaitguardian.screens.patient

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.gaitguardian.ui.theme.bgColor
import com.example.gaitguardian.ui.theme.ButtonActive
import com.example.gaitguardian.ui.theme.Heading1
import com.example.gaitguardian.ui.theme.screenPadding
import com.example.gaitguardian.ui.theme.spacerLarge

@Composable
fun GaitAssessmentScreen(navController: NavController, modifier: Modifier = Modifier) {

    var showTutorial by remember { mutableStateOf(false) }
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
            var assessmentChoice by remember { mutableStateOf("") }
            // First Button
            Button(
                onClick = {
                    val encoded = Uri.encode("Timed Up and Go")
                    navController.navigate("assessment_info_screen/$encoded")
                },
                colors = ButtonDefaults.buttonColors(containerColor = ButtonActive),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(vertical = 16.dp)
            ) {
                Text("Stand-Walk Test", fontSize = 20.sp, color = Color.Black)
            }

            Button(
                onClick = {
                    val encoded = Uri.encode("Sit-to-Stand x5")
                    navController.navigate("assessment_info_screen/$encoded")
                },
                colors = ButtonDefaults.buttonColors(containerColor = ButtonActive),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(vertical = 16.dp)
            ) {
                Text("Five Times Sit to Stand", fontSize = 20.sp, color = Color.Black)
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
            // --- Need help button ---
            Button(
                onClick = { showTutorial = true },
                colors = ButtonDefaults.buttonColors(containerColor = ButtonActive),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "How to use",
                    color = Color.Black,
                    fontSize = Heading1,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
// --- Full-screen tutorial overlay using Dialog ---
    if (showTutorial) {
        Dialog(onDismissRequest = { showTutorial = false }) {
            GaitAssessmentTutorial(
                onClose = { showTutorial = false }
            )
        }
    }
}