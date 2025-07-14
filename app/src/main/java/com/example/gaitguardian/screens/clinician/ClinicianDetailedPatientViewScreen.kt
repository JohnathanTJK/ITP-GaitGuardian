package com.example.gaitguardian.screens.clinician

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.example.gaitguardian.data.roomDatabase.patient.Patient
import com.example.gaitguardian.ui.theme.bgColor
import com.example.gaitguardian.ui.theme.buttonBackgroundColor
import com.example.gaitguardian.viewmodels.ClinicianViewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.point
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.shapeComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.component.TextComponent
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ClinicianDetailedPatientViewScreen(
    navController: NavController,
    clinicianViewModel: ClinicianViewModel,
    testId: Int,
    modifier: Modifier = Modifier
) {

    LaunchedEffect(testId) { // pre-load with the testId from backStackEntry
        clinicianViewModel.loadAssessmentById(testId)
    }


    val assessment by clinicianViewModel.selectedTUGAssessment.collectAsState()


    val patient = Patient(2, "Benny", 18)

    var tugDateTime by remember { mutableStateOf("") }
    var tugVideo by remember { mutableStateOf("") }
    var tugDuration by remember { mutableFloatStateOf(0f) }
    var onMedication by remember { mutableStateOf(false) }
    var medicationUpdated by remember { mutableStateOf(false) }
    var clinicianComments by remember { mutableStateOf("") }
    var patientComments by remember { mutableStateOf("") }
    var isReviewed by remember { mutableStateOf(false) }
    var markAsReviewed by remember { mutableStateOf(false) }
    var statusUpdateMsg by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    // Updating of database
    val scope = rememberCoroutineScope()
    var isUpdating by remember { mutableStateOf(false) }

    LaunchedEffect(assessment?.testId) {
        assessment?.let {
            tugDateTime = it.dateTime
            tugVideo = it.videoTitle.orEmpty()
            tugDuration = it.videoDuration
            onMedication = it.onMedication
            medicationUpdated = it.updateMedication
            patientComments = it.patientComments
            clinicianComments = it.notes.orEmpty()
            isReviewed = it.watchStatus
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(16.dp)
            .verticalScroll(scrollState),
//        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.fillMaxWidth()
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

            Text(
                "Assessment Details: ${tugDateTime}", fontSize = 14.sp,
                color = Color(0xFF718096)
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "TUG TEST #${testId}",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    color = Color.Black
                )
                VideoWatchStatus((if (isReviewed) "Reviewed" else "Pending"))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Medication
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Medication:",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (onMedication != medicationUpdated) Color(0xFFDCFCE7) else Color(
                                    0xFFFFE4E6
                                ),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (onMedication != medicationUpdated) "ON" else "OFF",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (onMedication != medicationUpdated) Color(0xFF166534) else Color(
                                0xFF9F1239
                            )
                        )
                    }
                }

                // Patient Updated
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Patient Updated:",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (medicationUpdated) Color(0xFFDBEAFE) else Color(
                                    0xFFF0F0F0
                                ),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (medicationUpdated) "YES" else "NO",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (medicationUpdated) Color(0xFF1D4ED8) else Color.DarkGray
                        )
                    }
                }
            }
            Row()
            {
                OutlinedTextField(
                    value = if(patientComments.isEmpty()) "No Comments" else patientComments,
                    onValueChange = {},
                    readOnly = true, // make it non-editable textfield
                    modifier = modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                    ),
                    label = { Text("Patient Comments", color = Color.Black) }
                )
            }
            // Assessment Recording Button
            VideoButton(tugVideo, tugDuration)

        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Overall Performance Graph", fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = Color.Black
                )
            }

            JetpackComposeBasicLineChart()
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { navController.navigate("performance_screen") },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonBackgroundColor
                    )
                ) {
                    Text("View Detailed Graph", color = Color.Black)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        TUGsubTasksList()
        Spacer(modifier = Modifier.height(16.dp))

        Column {
            Text(
                "Clinician Notes:",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
            OutlinedTextField(
                value = clinicianComments,
                onValueChange = { clinicianComments = it },
                modifier = modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.DarkGray,
                    focusedContainerColor = Color(0xFFF5F5F5),
                ),
                label = { Text("Enter observations here...", color = Color.Black) }
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
            ) {

                if (statusUpdateMsg.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFDFF0D8), shape = RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = statusUpdateMsg,
                            color = Color(0xFF3C763D),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    if (!isReviewed) {
//                            Checkbox(
//                                checked = isReviewed,
//                                onCheckedChange = { isReviewed = it }
//                            )
                        Checkbox(
                            checked = markAsReviewed,
                            onCheckedChange = { markAsReviewed = it }
                        )
                        Text(text = "Mark as Reviewed", color = Color.Black)
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        onClick = {
                            val finalReviewed = isReviewed || markAsReviewed
                            isUpdating = true
                            statusUpdateMsg = ""
                            scope.launch {
                                try {
                                    val success = clinicianViewModel.updateTUGReview(
                                        testId,
                                        finalReviewed,
                                        clinicianComments
                                    )

                                    if (success) {
                                        isReviewed = finalReviewed
                                        statusUpdateMsg = "Status Updated Successfully."
                                        // Reload fresh data
                                        clinicianViewModel.loadAssessmentById(testId)
                                    } else {
                                        statusUpdateMsg = "Update failed. Please try again."
                                    }
                                } catch (e: Exception) {
                                    statusUpdateMsg = "Error: ${e.message}"
                                } finally {
                                    isUpdating = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = buttonBackgroundColor,
                            contentColor = Color.Black,
                            disabledContainerColor = Color.LightGray,
                            disabledContentColor = Color.DarkGray
                        ),
                        // If no comments/ not reviewed, don't allow update.
                        enabled = (!clinicianComments.isEmpty() || isReviewed) && !isUpdating,
                    ) {
                        if (isUpdating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.Black
                            )
                        } else {
                            Text("Update Status", color = Color.Black)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}


// Taken from Vico's Sample Code https://github.com/patrykandpatrick/vico/blob/master/sample/compose/src/main/kotlin/com/patrykandpatrick/vico/sample/compose/BasicLineChart.kt
// Added axis labels + circular points on each datapoint
@Composable
private fun JetpackComposeBasicLineChart(
    modelProducer: CartesianChartModelProducer,
    modifier: Modifier = Modifier,
) {
    val line = LineCartesianLayer.rememberLine(
        fill = LineCartesianLayer.LineFill.single(fill(Color.Blue)),
        pointProvider = LineCartesianLayer.PointProvider.single(
            LineCartesianLayer.point(
                shapeComponent(fill(Color.Blue), CorneredShape.Pill)
            )
        )
    )

    // Set colour text value for axis
    val axisValueColor = 0xFF000000.toInt() // Black (ARGB format)
    val valueComponent = TextComponent(
        color = axisValueColor, // since they only allow Int
    )

    val xAxis = HorizontalAxis.rememberBottom(
        title = "TUG Test Video Assessment",
        titleComponent = TextComponent(),
        label = valueComponent
    )

    val yAxis = VerticalAxis.rememberStart(
        title = "Time Taken",
        titleComponent = TextComponent(),
        label = valueComponent
    )

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(listOf(line))
            ),
            startAxis = yAxis,
            bottomAxis = xAxis,
        ),
        modelProducer = modelProducer,
        modifier = modifier,
    )
}

val tugSubTasks = listOf(
    "Sit-To-Stand", "Walk-From-Chair", "Turn-First", "Walk-To-Chair", "Turn-Second", "Stand-To-Sit"
)

@Composable
fun TUGsubTasksList(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)

    )
    {
        tugSubTasks.forEach { task ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    )
                    {
                        Text(task, color = Color.Black)
                        Card(
                            modifier = Modifier
                                .height(24.dp)
                                .width(40.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.DarkGray
                            ),
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("15s", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JetpackComposeBasicLineChart(modifier: Modifier = Modifier) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(Unit) {
        modelProducer.runTransaction {
            lineSeries {
                series(
                    listOf(1, 2, 3, 4, 5, 6),  // TUG Tests # / X values
                    listOf(13, 8, 7, 12, 0, 1)       // Time Taken / Y Values
                )
            }
        }
    }
    JetpackComposeBasicLineChart(modelProducer, modifier)
}


@Composable
fun VideoButton(videoTitle: String, videoDuration: Float) {
    val context = LocalContext.current
    val videoFolder = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
    val videoFile = videoFolder?.listFiles()?.find { it.name == videoTitle }
    if (videoFile != null && videoFile.exists()) {
        VideoListItem(context, videoFile, videoDuration)
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFF5F5), shape = RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Text(
                text = "No Video Available [${videoDuration}s]",
                color = Color(0xFFE53E3E),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun VideoListItem(context: Context, file: File, videoDuration: Float) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Button(
            onClick = {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "video/mp4")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFC9E4F),
                contentColor = Color.Black
            )
        ) {
            Text("Watch Assessment Recording [${videoDuration}s]", fontWeight = FontWeight.Bold)
        }
    }
}
