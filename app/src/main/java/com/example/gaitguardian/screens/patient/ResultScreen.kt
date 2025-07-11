package com.example.gaitguardian.screens.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight.Companion.ExtraBold
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gaitguardian.data.roomDatabase.tug.TUGAssessment
import com.example.gaitguardian.ui.theme.*
import com.example.gaitguardian.viewmodels.PatientViewModel

@Composable
fun ResultScreen(
    navController: NavController,
    recordingTime: Int,
    patientViewModel: PatientViewModel,
    modifier: Modifier = Modifier
) {

LaunchedEffect(Unit){ // fetch latest TUG assessment (aka the one that was just recorded)
    patientViewModel.getLatestTUGAssessment()
    patientViewModel.getLatestTwoDurations()
}
//    val medicationStatus by patientViewModel.medicationStatus.collectAsState()
//    val previousTiming by patientViewModel.previousDuration.collectAsState()
//    val latestTiming by patientViewModel.latestDuration.collectAsState()
    var previousTiming by remember { mutableFloatStateOf(0f) }
    var latestTiming by remember { mutableFloatStateOf(0f) }
    val comment by patientViewModel.assessmentComment.collectAsState()

    // Local state for toggle, initialized from ViewModel medicationStatus
//    var isMedicationOn by remember { mutableStateOf(medicationStatus == "ON") }

    val onMedication by patientViewModel.onMedication.collectAsState()
    val latestTugAssessment by patientViewModel.latestAssessment.collectAsState()
    val latestTwoDurations by patientViewModel.latestTwoDurations.collectAsState()

    if (latestTwoDurations.size >= 2) { // Ensure there are at least two values fetched
        latestTiming = latestTwoDurations[0]
        previousTiming = latestTwoDurations[1]
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor),
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        LatestAssessmentResultsCard(
            latestAssessment = latestTugAssessment,
            previousTiming = previousTiming,
            latestTiming = latestTiming,
            medicationOn = onMedication,
            showMedicationToggle = true,
//            patientcomment = comment,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

    // Use the database flag to determine if already updated
        val hasBeenUpdated = latestTugAssessment?.updateMedication == true
        var hasUpdatedMedication by remember { mutableStateOf(hasBeenUpdated) }
    // Sync local state with database state using LaunchedEffect
        LaunchedEffect(hasBeenUpdated) {
            hasUpdatedMedication = hasBeenUpdated
        }

        Button(
            onClick = {
                if (!hasUpdatedMedication) {
                    // Update local state immediately
                    hasUpdatedMedication = true

                    // Update database
                    patientViewModel.setOnMedication(!onMedication)
                    patientViewModel.updatePostAssessmentOnMedicationStatus(true)
                    patientViewModel.getLatestTUGAssessment()
                }
            },
            enabled = !hasUpdatedMedication, // Use local state
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (hasBeenUpdated) "Medication Status Updated" else "Update Medication Status",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun LatestAssessmentResultsCard(
    modifier: Modifier = Modifier,
    latestAssessment: TUGAssessment?,
    previousTiming: Float = 13f, // Updated to Float to match TUGAssessment videoDuration data type
    latestTiming: Float, // Updated to Float to match TUGAssessment videoDuration data type
    medicationOn: Boolean? = null,
//    patientcomment: String,
    showDivider: Boolean = true,
    showMedicationToggle: Boolean = false,
) {

    val maxVal = maxOf(previousTiming, latestTiming, 30f)

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Assessment Result",
                fontSize = subheading1,
                fontWeight = FontWeight.Bold,
                color = DefaultColor
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Type: TUG",
                    color = Color.Black,
                    fontSize = body,
                    fontWeight = FontWeight.Medium
                )
                VerticalDivider()
                Text(
                    text = latestAssessment?.dateTime ?: "14 June 2025",
                    color = Color.Black,
                    fontSize = body,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                StatusBox(title = "Severity", value = "2", modifier = Modifier.weight(1f))

                if (!showMedicationToggle) {
                    if(medicationOn != null) { // Shows in ResultScreen
                        StatusBox(
                            title = "Medication",
//                        value = if (onMedication) "ON" else "OFF",
                            value = if (medicationOn == true) "ON" else "OFF",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    else { // Showing in PatientHomeScreen
                        val isOn = latestAssessment?.onMedication == true
                        val wasUpdated = latestAssessment?.updateMedication == true
                        // If updated, flip the status
                        val displayStatus = if (wasUpdated) !isOn else isOn
                        StatusBox(
                            title = "Medication",
                            value =
                            if (displayStatus) "ON" else "OFF",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalProgressBar(
                previousValue = previousTiming,
                latestValue = latestTiming,
                maxValue = maxVal,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            if (showDivider) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(thickness = 1.dp, color = Color.LightGray)
                Spacer(modifier = Modifier.height(20.dp))
            }

            if (showMedicationToggle) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Medication Status",
                    fontSize = subheading1,
                    fontWeight = FontWeight.Bold,
                    color = DefaultColor
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = buttonBackgroundColor,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .padding(vertical = 12.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = if (medicationOn == true) "ON" else "OFF",
//                            text = if (latestAssessment?.onMedication == true) "ON" else "OFF",
                            color = DefaultColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Comments:",
                fontSize = subheading1,
                fontWeight = FontWeight.Bold,
                color = DefaultColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            (if (latestAssessment?.patientComments?.isEmpty() == true) "No comment provided" else latestAssessment?.patientComments)?.let {
                Text(
        //                text = if (patientcomment.isEmpty()) "No comment provided" else patientcomment,
                    text = it,
                    color = Color.Black,
                    fontSize = body
                )
            }
        }
    }
}


@Composable
fun VerticalDivider(
    color: Color = Color.LightGray,
    thickness: Dp = 1.dp,
    height: Dp = 20.dp
) {
    Box(
        modifier = Modifier
            .width(thickness)
            .height(height)
            .background(color)
    )
}


@Composable
fun StatusBox(title: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(DefaultColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontWeight = ExtraBold,
                color = DefaultColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = value,
                fontSize = Heading1,
                color = DefaultColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun HorizontalProgressBar(
    previousValue: Float,
    latestValue: Float,
    maxValue: Float = 30f,
    modifier: Modifier = Modifier
) {
    val prevFraction = previousValue / maxValue
    val latestFraction = latestValue / maxValue

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(Color.LightGray, RoundedCornerShape(8.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(prevFraction)
                .background(Color.Gray, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(latestFraction)
                .background(Color(0xFF4CAF50), RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                .align(Alignment.CenterStart)
                .offset(x = with(LocalDensity.current) { (prevFraction * (LocalConfiguration.current.screenWidthDp.dp.toPx())).toDp() })
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Prev: ${previousValue.toInt()}s", color = Color.Gray)
        Text("Now: ${latestValue.toInt()}s", color = Color(0xFF4CAF50))
    }
}
