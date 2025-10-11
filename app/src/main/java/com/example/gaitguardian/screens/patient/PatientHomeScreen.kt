package com.example.gaitguardian.screens.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight.Companion.ExtraBold
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gaitguardian.ui.theme.DefaultColor
import com.example.gaitguardian.ui.theme.Heading1
import com.example.gaitguardian.ui.theme.bgColor
import com.example.gaitguardian.ui.theme.body
import com.example.gaitguardian.viewmodels.PatientViewModel
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import com.example.gaitguardian.data.roomDatabase.tug.TUGAnalysis
import com.example.gaitguardian.ui.theme.ButtonActive
import com.example.gaitguardian.viewmodels.TugDataViewModel


@Composable
fun PatientHomeScreen(
    navController: NavController,
    patientViewModel: PatientViewModel,
    tugViewModel: TugDataViewModel,
    showButton: Boolean = true, // make sure this flag exists
    modifier: Modifier = Modifier
) {
    val patientInfo by patientViewModel.patient.collectAsState()
    val latestTwoDurations by tugViewModel.latestTwoDurations.collectAsState()
    val latestAssessment by tugViewModel.latestAssessment.collectAsState()
    val onMedication by tugViewModel.onMedication.collectAsState()

    var previousTiming by remember { mutableFloatStateOf(0f) }
    var latestTiming by remember { mutableFloatStateOf(0f) }
    var latestAnalysis by remember { mutableStateOf<TUGAnalysis?>(null) }

    val severity = latestAnalysis?.severity ?: "-"
    val totalTime = latestAnalysis?.timeTaken?.toFloat() ?: 0f

    var showTutorial by remember { mutableStateOf(false) } // <-- control overlay

    LaunchedEffect(Unit) {
        tugViewModel.getLatestTwoDurations()
        tugViewModel.getLatestTUGAssessment()
        latestAnalysis = tugViewModel.getLatestTugAnalysis()
    }

    if (latestTwoDurations.size >= 2) {
        latestTiming = latestTwoDurations[0]
        previousTiming = latestTwoDurations[1]
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(16.dp),
        contentAlignment = Alignment.Center // center everything vertically & horizontally
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Welcome Text
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Welcome back,",
                    fontSize = body,
                    color = Color(0xFF718096)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = patientInfo?.name ?: "Sophia Tan",
                    fontWeight = ExtraBold,
                    fontSize = Heading1,
                    color = Color.Black
                )
            }

            // Core Results Card
            CoreResultsCard(
                latestAssessment = latestAssessment,
                previousTiming = previousTiming,
                latestTiming = latestTiming,
                severity = severity,
                totalTime = totalTime,
                medicationOn = onMedication,
                hasUpdatedMedication = latestAssessment?.updateMedication == true,
                showUpdateButton = false,
                onUpdateMedication = {}
            )

            if (showButton) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { navController.navigate("gait_assessment_screen") },
                        //onClick = { navController.navigate("result_screen/TestAssessment") },
                        colors = ButtonDefaults.buttonColors(containerColor = ButtonActive),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Record Video",
                            color = DefaultColor,
                            fontSize = Heading1,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (showButton) {
                        Button(
                            onClick = { showTutorial = true }, // <-- show overlay
                            colors = ButtonDefaults.buttonColors(containerColor = ButtonActive),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "How to use",
                                color = DefaultColor,
                                fontSize = Heading1,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

            }
        }
        // --- Full-screen tutorial overlay using Dialog ---
        if (showTutorial) {
            Dialog(onDismissRequest = { showTutorial = false }) {
                PatientTutorialScreen(
                    onClose = { showTutorial = false }
                )
            }
        }
    }
}
