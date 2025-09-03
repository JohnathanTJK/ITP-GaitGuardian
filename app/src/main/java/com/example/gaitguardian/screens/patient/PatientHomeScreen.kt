package com.example.gaitguardian.screens.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gaitguardian.ui.theme.DefaultColor
import com.example.gaitguardian.ui.theme.Heading1
import com.example.gaitguardian.ui.theme.bgColor
import com.example.gaitguardian.ui.theme.body
import com.example.gaitguardian.ui.theme.boxShape
import com.example.gaitguardian.ui.theme.buttonBackgroundColor
import com.example.gaitguardian.ui.theme.cardBackgroundColor
import com.example.gaitguardian.ui.theme.homeIconSize
import com.example.gaitguardian.ui.theme.severityBoxColor
import com.example.gaitguardian.ui.theme.subheading1
import com.example.gaitguardian.viewmodels.PatientViewModel
import java.time.LocalDate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import com.example.gaitguardian.data.roomDatabase.tug.TUGAnalysis
import com.example.gaitguardian.screens.patient.LatestAssessmentResultsCard
import com.example.gaitguardian.viewmodels.TugDataViewModel


@Composable
fun PatientHomeScreen(
    navController: NavController,
    patientViewModel: PatientViewModel,
    tugViewModel: TugDataViewModel,
    modifier: Modifier = Modifier
) {
    val patientInfo by patientViewModel.patient.collectAsState()
//    val previousTiming by patientViewModel.previousDuration.collectAsState()
//    val latestTiming by patientViewModel.latestDuration.collectAsState()

    // Recreated previousTiming and latestTiming to track state from database fetch instead
    var latestAnalysis by remember { mutableStateOf<TUGAnalysis?>(null) }
    var previousTiming by remember { mutableFloatStateOf(0f) }
    var latestTiming by remember { mutableFloatStateOf(0f) }
    val severity = latestAnalysis?.severity ?: "-"
    val totalTime = latestAnalysis?.timeTaken?.toFloat() ?: 0f


    LaunchedEffect(Unit) {
        tugViewModel.getLatestTwoDurations()
        tugViewModel.getLatestTUGAssessment()
        latestAnalysis = tugViewModel.getLatestTugAnalysis()  // ← NEW
    }

//    val latestTwoDurations by patientViewModel.latestTwoDurations.collectAsState()
//
//    val latestAssessment by patientViewModel.latestAssessment.collectAsState()
    val latestTwoDurations by tugViewModel.latestTwoDurations.collectAsState()

    val latestAssessment by tugViewModel.latestAssessment.collectAsState()

    if (latestTwoDurations.size >= 2) { // Ensure there are at least two values fetched
        latestTiming = latestTwoDurations[0] // Sorted by testId DESC
        previousTiming = latestTwoDurations[1]
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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

            MissedAssessmentCard(navController)

            LatestAssessmentResultsCard(
                latestAssessment = latestAssessment,
                previousTiming = previousTiming,
                latestTiming = latestTiming,
                medicationOn = true, // or from ViewModel if you want
                showMedicationToggle = true,
                severity = severity,
                totalTime = totalTime,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

        }

        // Optional: HomeIcon(navController)
    }
}


@Composable
fun MissedAssessmentCard(navController: NavController) {
    val currentDate = LocalDate.now()

    // Example 1: Missed assessment (before today)
    val assessmentDate = LocalDate.parse("2025-06-10")

    // Example 2: Assessment due today
    // val assessmentDate = LocalDate.now()

    // Example 3: Upcoming assessment (after today)
    // val assessmentDate = LocalDate.parse("2025-06-20")

    val (titleText, dateText, showButton) = when {
        assessmentDate.isBefore(currentDate) -> Triple(
            "Missed Assessment",
            "on 10 June 2025",
            true
        )
        assessmentDate.isEqual(currentDate) -> Triple(
            "Assessment Due",
            "Today",
            true
        )
        else -> Triple(
            "Next Assessment",
            "on 20 June 2025",
            false
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor)
    ) {
        Column {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(25.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = titleText,
                    fontSize = subheading1,
                    color = DefaultColor,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateText,
                    fontSize = Heading1,
                    color = DefaultColor,
                    fontWeight = ExtraBold,
                    textAlign = TextAlign.Center
                )
            }

            if (showButton) {
                Button(
                    onClick = { navController.navigate("gait_assessment_screen") },
                    colors = ButtonDefaults.buttonColors(containerColor = buttonBackgroundColor),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text(
                        text = "Take Assessment",
                        color = DefaultColor,
                        fontSize = Heading1,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = { navController.navigate("video_test_screen") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Test Video Analysis",
                        color = Color.White,
                        fontSize = body,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

        }
    }
//    Button(
//        onClick = {
//            navController.navigate("tug_assessment_screen")
//        }
//
//    ) {
//        Text("Test TUG Assessment")
//    }

}





