package com.example.gaitguardian.screens.clinician

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gaitguardian.data.roomDatabase.patient.Patient
import com.example.gaitguardian.data.roomDatabase.tug.TUGAssessment
import com.example.gaitguardian.ui.theme.bgColor
import com.example.gaitguardian.ui.theme.buttonBackgroundColor
import com.example.gaitguardian.viewmodels.ClinicianViewModel
import com.example.gaitguardian.viewmodels.PatientViewModel

@Composable
fun ClinicianHomeScreen(
    navController: NavController,
    clinicianViewModel: ClinicianViewModel,
    patientViewModel: PatientViewModel,
    modifier: Modifier = Modifier
) {

//    var tugVideos by remember { mutableStateOf(listOf(
//        //TODO: Replace with actual data
//        TUGVideo(1, "Today, 1:30PM", "ON", "High", true),
//        TUGVideo(2, "Today, 2:00PM", "OFF", "Low", true),
//        TUGVideo(3, "Yesterday, 10:15AM", "ON", "Medium", false),
//        TUGVideo(4, "May 5, 3:45PM", "OFF", "High", true),
//        TUGVideo(5, "April 30, 9:00AM", "ON", "Low", false),
//        TUGVideo(6, "April 28, 11:20AM", "OFF", "High", false),
//        TUGVideo(7, "April 25, 12:15PM", "ON", "Medium", true),
//        TUGVideo(8, "April 20, 4:45PM", "OFF", "Low", true),
//        TUGVideo(9, "April 18, 2:30PM", "ON", "High", true),
//        TUGVideo(10, "April 15, 1:00PM", "OFF", "Medium", false),
//    )) }

    // Start: Patient ViewModel testing
    val patientInfo by patientViewModel.patient.collectAsState()

    // End: Patient ViewModel testing
    // Start: Clinician ViewModel testing
    val clinicianInfo by clinicianViewModel.clinician.collectAsState()
    val uploadedAssesssments by clinicianViewModel.allTUGAssessments.collectAsState()

    val pendingReviews =
        uploadedAssesssments.count { !it.watchStatus } // Calculate number of videos that are not watched
    // tugVideos.count { !it.watchStatus } // Calculate number of videos that are not watched


    // For the multiple mark-as-reviewed functionality
    var selectedVideoIds by remember { mutableStateOf(setOf<Int>()) }

    // To display either ALL/ Pending videos only
    var showPendingVideos by remember { mutableStateOf(false) }

    val filteredVideos = if (showPendingVideos) {
        uploadedAssesssments.filter { !it.watchStatus }
//        tugVideos.filter { !it.watchStatus }
    } else {
        uploadedAssesssments
    }

    Spacer(Modifier.height(30.dp))

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(bottom = if (selectedVideoIds.isNotEmpty()) 80.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                ClinicianHeader(
                    clinicianName = "Dr. ${clinicianInfo?.name ?: "Clinician"}",
                    patient = patientInfo ?: Patient(id = 2, name = "Benny", age = 18),
                    pendingReviews = pendingReviews
                )
            }

            item {
                VideoReviewsSummaryCard(
//                    totalTests = tugVideos.count(),
                    totalTests = uploadedAssesssments.count(),
                    pendingTests = pendingReviews,
                    showOnlyPending = showPendingVideos,
                    onFilterToggle = { showPendingVideos = it }
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Patient's Assessment Records", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color(0xFF2D3748))
                    HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF718096))
                }
            }

//            items(tugVideos) { video ->
            if(filteredVideos.isEmpty()){
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (showPendingVideos) "No pending assessments to review." else "No assessments available.",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
            else{
                items(filteredVideos.reversed()) { video -> // show latest first
                    val finalMedicationState = video.onMedication != video.updateMedication

                    TUGVideoItem(
                        navController = navController,
                        testId = video.testId,
                        dateTime = video.dateTime,
//                    medication = video.medication,
//                    severity = video.severity,
                        medication = finalMedicationState,
                        patientcomments = video.patientComments,
                        severity = "2",
                        watchStatus = if (video.watchStatus) "Reviewed" else "Pending",
                        isSelected = selectedVideoIds.contains(video.testId),
                        onSelectionChanged = { isSelected ->
                            selectedVideoIds = if (isSelected) {
                                selectedVideoIds + video.testId
                            } else {
                                selectedVideoIds - video.testId
                            }
                        }
                    )
                }

            }
        }

        if (selectedVideoIds.isNotEmpty()) {
            MultiSelectControls(
                selectedCount = selectedVideoIds.size,
                onMarkAsWatched = {
//                    // TODO: To update the database
                    // TODO: something likeclinicianViewModel.markVideoAsWatched(videoId)
                    selectedVideoIds.forEach { videoId ->
                        clinicianViewModel.markMultiAsReviewed(videoId)
                    }
                    // For Testing Only
//                    tugVideos = tugVideos.map { video ->
//                        if (video.testId in selectedVideoIds) {
//                            video.copy(watchStatus = true)
//                        } else {
//                            video
//                        }
//                    }

                    // After update then clear the selection
                    selectedVideoIds = setOf()
                },
                onCancel = {
                    selectedVideoIds = setOf()
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
fun MultiSelectControls(
    selectedCount: Int,
    onMarkAsWatched: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4299E1)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
            top = 8.dp,
            end = 16.dp,
            bottom = 8.dp
        )        ) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$selectedCount video${if (selectedCount != 1) "s" else ""} selected",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Row(modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround) {
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = SolidColor(Color.White)
                )
            ) {
                Text("Cancel", color = Color.White)
            }

            Button(
                onClick = onMarkAsWatched,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                )
            ) {
                Text("Mark as Reviewed", color = Color(0xFF4299E1))
            }
        }
    }
}


@Composable
fun VideoReviewsSummaryCard(
    totalTests: Int,
    pendingTests: Int,
    showOnlyPending: Boolean,
    onFilterToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Overview", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2D3748))
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF718096))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                VideoOverviewStats(
                    value = totalTests.toString(),
                    label = "Total Tests",
                    isSelected = !showOnlyPending,
                    onClick = { onFilterToggle(false) },
                    modifier = Modifier.weight(1f)
                )
                VideoOverviewStats(
                    value = pendingTests.toString(),
                    label = "Needs Review",
                    isSelected = showOnlyPending,
                    onClick = { onFilterToggle(true) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun VideoOverviewStats(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .background(if (isSelected) Color(0xFFEEF2F6) else Color.Transparent, shape = RoundedCornerShape(8.dp))
            .padding(8.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2D3748)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF718096)
        )
    }
}


@Composable
fun TUGVideoItem(
    navController: NavController,
    testId: Int,
    dateTime: String,
    medication: Boolean,
    severity: String,
    patientcomments: String,
    watchStatus: String,
    isSelected: Boolean = false,
    onSelectionChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Log.d("tugtest", "mediciation value: ${medication}")
    Card(
        modifier = modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFEBF8FF) else Color.White
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, Color(0xFF4299E1))
        } else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if(watchStatus == "Pending")
                {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = onSelectionChanged,
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF4299E1)
                        )
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "TUG #${testId}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2D3748),
                    modifier = Modifier.weight(1f)
                )

                VideoWatchStatus(watchStatus)
            }

            Text(
                text = dateTime,
                fontSize = 12.sp,
                color = Color(0xFF2D3748),
                modifier = Modifier.padding(start = 48.dp) // Align with text above (checkbox width + spacer)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                buildAnnotatedString {
                    append("Medication: ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(if (medication) "ON" else "OFF")
                    }
                },
                fontSize = 14.sp,
                color = Color.Black,
                modifier = Modifier.padding(start = 48.dp) // Align with text above
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                buildAnnotatedString {
                    append("Video Severity Rating: ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        append(severity)
                    }
                },
                fontSize = 14.sp,
                color = Color.Black,
                modifier = Modifier.padding(start = 48.dp) // Align with text above
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Show Review button only when item is not selected
            if (!isSelected) {
                Button(
                    onClick = {
//                        navController.navigate("clinician_detailed_patient_view_screen")
                        navController.navigate("clinician_detailed_patient_view_screen/${testId}")

                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 48.dp), // Align with text above
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonBackgroundColor
                    )
                ) {
                    Text("Review Assessment")
                }
            }
        }
    }
}

@Composable
fun VideoWatchStatus(watchStatus: String) {
    Card(
        modifier = Modifier
            .height(24.dp)
            .width(80.dp),
        shape = RoundedCornerShape(5.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (watchStatus == "Reviewed") Color(0xFFC6F6D5) else Color(0xFFFEEBC8),
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = watchStatus,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (watchStatus == "Reviewed") Color(0xFF2F855A) else Color(0xFFDD6B20),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ClinicianHeader(
    clinicianName: String,
    patient: Patient,
    pendingReviews: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    )
    {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Welcome message
            Text(
                text = "Welcome back,",
                fontSize = 16.sp,
                color = Color(0xFF718096),
                fontWeight = FontWeight.Normal
            )
            Text(
                text = clinicianName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D3748),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Divider
            HorizontalDivider(
                thickness = 1.dp,
                color = Color(0xFFE2E8F0),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Current patient section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Currently reviewing",
                        fontSize = 14.sp,
                        color = Color(0xFF718096),
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = patient.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2D3748)
                        )
                        Text(
                            text = "• Age ${patient.age}",
                            fontSize = 14.sp,
                            color = Color(0xFF718096)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (pendingReviews > 0) Color(0xFFFFF5F5) else Color(0xFFF0FFF4)
                ),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Box to display pending reviews and a message accordingly
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Pending reviews",
                        tint = if (pendingReviews > 0) Color(0xFFE53E3E) else Color(0xFF38A169),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (pendingReviews > 0)
                            "$pendingReviews video assessment${if (pendingReviews != 1) "s" else ""} pending review"
                        else
                            "No pending videos assessment to review",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (pendingReviews > 0) Color(0xFFE53E3E) else Color(0xFF38A169)
                    )
                }
            }
        }
    }
}
