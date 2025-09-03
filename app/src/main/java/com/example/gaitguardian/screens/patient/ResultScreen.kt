package com.example.gaitguardian.screens.patient

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight.Companion.ExtraBold
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gaitguardian.api.TugMetrics
import com.example.gaitguardian.data.roomDatabase.tug.TUGAssessment
import com.example.gaitguardian.ui.theme.*
import com.example.gaitguardian.viewmodels.PatientViewModel
import com.example.gaitguardian.viewmodels.TugDataViewModel

@Composable
fun ResultScreen(
    navController: NavController,
    assessmentTitle: String,
    patientViewModel: PatientViewModel,
    tugViewModel: TugDataViewModel,
    modifier: Modifier = Modifier
) {

LaunchedEffect(Unit){ // fetch latest TUG assessment (aka the one that was just recorded)
//    patientViewModel.getLatestTUGAssessment()
//    patientViewModel.getLatestTwoDurations()
//    patientViewModel.setAssessmentComment("")
    tugViewModel.getLatestTUGAssessment()
    tugViewModel.getLatestTwoDurations()
    tugViewModel.setAssessmentComment("")
}
//    val medicationStatus by patientViewModel.medicationStatus.collectAsState()
//    val previousTiming by patientViewModel.previousDuration.collectAsState()
//    val latestTiming by patientViewModel.latestDuration.collectAsState()
    var previousTiming by remember { mutableFloatStateOf(0f) }
    var latestTiming by remember { mutableFloatStateOf(0f) }
//    val comment by patientViewModel.assessmentComment.collectAsState()

    // Local state for toggle, initialized from ViewModel medicationStatus
//    var isMedicationOn by remember { mutableStateOf(medicationStatus == "ON") }

//    val onMedication by patientViewModel.onMedication.collectAsState()
//    val latestTugAssessment by patientViewModel.latestAssessment.collectAsState()
//    val latestTwoDurations by patientViewModel.latestTwoDurations.collectAsState()
    val onMedication by tugViewModel.onMedication.collectAsState()
    val latestTugAssessment by tugViewModel.latestAssessment.collectAsState()
    val latestTwoDurations by tugViewModel.latestTwoDurations.collectAsState()
    val analysisResult by tugViewModel.response.collectAsState()
    val severity = analysisResult?.severity ?: "-"
    val totalTime = analysisResult?.tugMetrics?.totalTime ?: 0.0

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
            severity = severity,
            tugMetrics = analysisResult?.tugMetrics,
            totalTime = totalTime.toFloat(), // cast to Float if needed
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
//                    patientViewModel.setOnMedication(!onMedication)
//                    patientViewModel.updatePostAssessmentOnMedicationStatus(true)
//                    patientViewModel.getLatestTUGAssessment()
                    tugViewModel.setOnMedication(!onMedication)
                    tugViewModel.updatePostAssessmentOnMedicationStatus(true)
                    tugViewModel.getLatestTUGAssessment()
                }
            },
            enabled = !hasUpdatedMedication, // Use local state
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonBackgroundColor,
                contentColor = DefaultColor,
                disabledContainerColor = Color(0xFFE0E0E0),
                disabledContentColor = Color.DarkGray
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (hasUpdatedMedication) "Medication Status Updated" else "Update Medication Status",
                fontWeight = FontWeight.Bold
            )

        }
    }
}

@Composable
fun LatestAssessmentResultsCard(
    modifier: Modifier = Modifier,
    latestAssessment: TUGAssessment?,
    previousTiming: Float = 13f,
    latestTiming: Float,
    severity: String,
    totalTime: Float,
    tugMetrics: TugMetrics? = null,
    medicationOn: Boolean? = null,
    showComments: Boolean = true,
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

//
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Type: ${if (latestAssessment != null) "TUG" else "-"}",
                    color = Color.Black,
                    fontSize = body,
                    fontWeight = FontWeight.Medium
                )
                VerticalDivider()
                Text(
                    text = "${latestAssessment?.dateTime ?: "-"}",
                    color = Color.Black,
                    fontSize = body,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

//
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                StatusBox(
                    title = "Severity",
                    value = severity,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                )

                if (!showMedicationToggle) {
                    StatusBox(
                        title = "Medication",
                        value = if (latestAssessment != null) {
                            val isOn = latestAssessment.onMedication
                            val wasUpdated = latestAssessment.updateMedication
                            val displayStatus = if (wasUpdated) !isOn else isOn
                            if (displayStatus) "ON" else "OFF"
                        } else {
                            "-"
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalProgressBar(
                previousValue = previousTiming,
                latestValue = totalTime.toFloat(),
                maxValue = maxVal,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            if (showDivider) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(thickness = 1.dp, color = Color.LightGray)
                Spacer(modifier = Modifier.height(20.dp))
            }

            // TUG Breakdown Section
            Text(
                text = "TUG Breakdown",
                fontSize = subheading1,
                fontWeight = FontWeight.Bold,
                color = DefaultColor
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                TugPhaseRow("Sit-to-stand", tugMetrics?.sitToStandTime ?: 0.0)
                TugPhaseRow("Walk-from-chair", tugMetrics?.walkFromChairTime ?: 0.0)
                TugPhaseRow("Turn-first", tugMetrics?.turnFirstTime ?: 0.0)
                TugPhaseRow("Walk-to-chair", tugMetrics?.walkToChairTime ?: 0.0)
                TugPhaseRow("Turn-second", tugMetrics?.turnSecondTime ?: 0.0)
                TugPhaseRow("Stand-to-sit", tugMetrics?.standToSitTime ?: 0.0)
            }

            Spacer(modifier = Modifier.height(16.dp))

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

            if (showComments) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Comments:",
                    fontSize = subheading1,
                    fontWeight = FontWeight.Bold,
                    color = DefaultColor
                )

                Spacer(modifier = Modifier.height(8.dp))

                (if (latestAssessment?.patientComments.isNullOrEmpty()) "No comment provided" else latestAssessment?.patientComments)?.let {
                    Text(
                        text = it,
                        color = Color.Black,
                        fontSize = body
                    )
                }
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
    previousValue: Float?,
    latestValue: Float?,
    maxValue: Float = 30f,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (previousValue == null || latestValue == null || (previousValue == 0f && latestValue == 0f)) {
                // No data found case
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No data found",
                        color = Color.White,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                val difference = latestValue - previousValue
                val (barColor, statusText) = when {
                    difference < 0f -> {
                        Color(0xFF4CAF50) to "Improve by: ${"%.1f".format(kotlin.math.abs(difference))}s"
                    }
                    difference > 0f -> {
                        Color(0xFFE53935) to "Worsen by: ${"%.1f".format(kotlin.math.abs(difference))}s"
                    }
                    else -> {
                        Color(0xFFFFCC80) to "Stable performance"
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(barColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = statusText,
                        color = Color.White,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Only show prev/now row if data is available
        if (previousValue != null && latestValue != null && !(previousValue == 0f && latestValue == 0f)) {
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Prev: ${"%.1f".format(previousValue)}s",
                    fontSize = 16.sp,
                    color = Color.Black
                )
                Text(
                    text = "Now: ${"%.1f".format(latestValue)}s",
                    fontSize = 16.sp,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun TugPhaseRow(
    phaseName: String,
    duration: Double,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$phaseName:",
            fontSize = body,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${String.format("%.1f", duration)}s",
            fontSize = body,
            fontWeight = FontWeight.Bold,
            color = DefaultColor
        )
    }
}
