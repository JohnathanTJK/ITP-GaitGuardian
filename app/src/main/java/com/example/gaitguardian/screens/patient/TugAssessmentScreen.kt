package com.example.gaitguardian.screens.patient

// Core Android and Compose imports
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background

// Coroutines
import kotlinx.coroutines.launch

// Java File
import java.io.File

// Your API classes
import com.example.gaitguardian.api.GaitAnalysisClient
import com.example.gaitguardian.api.GaitAnalysisResponse
sealed class AnalysisState {
    object Idle : AnalysisState()
    object Analyzing : AnalysisState()
    object Success : AnalysisState()
    data class Error(val message: String) : AnalysisState()
}

// Updated TugAssessmentScreen.kt with patient-friendly design

@Composable
fun PatientFriendlyTugAssessmentScreen(
    navController: NavController,
    patientId: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gaitClient = remember { GaitAnalysisClient() }

    // State variables
    var recordedVideoFile by remember { mutableStateOf<File?>(null) }
    var medicationState by remember { mutableStateOf("N/A") }
    var comments by remember { mutableStateOf("") }
    var showMedicationDialog by remember { mutableStateOf(false) }

    // Analysis state
    var analysisState by remember { mutableStateOf<AnalysisState>(AnalysisState.Idle) }
    var analysisResult by remember { mutableStateOf<GaitAnalysisResponse?>(null) }

    // Patient-friendly color scheme
    val patientOrange = Color(0xFFFF9800)
    val patientYellow = Color(0xFFFFF3E0)
    val softGray = Color(0xFFF5F5F5)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(patientYellow) // Warm yellow background
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "Movement Assessment",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Simple Instructions Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📋 Simple Steps",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = patientOrange
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Simplified instructions with bigger text
                listOf(
                    "1. 🪑 Sit in chair",
                    "2. 🚶 Stand and walk 3 meters",
                    "3. 🔄 Turn around",
                    "4. 🚶 Walk back to chair",
                    "5. 🪑 Sit down"
                ).forEach { instruction ->
                    Text(
                        text = instruction,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 4.dp),
                        fontSize = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Video Recording Section - Patient Friendly
        PatientVideoRecordingSection(
            onVideoRecorded = { videoFile ->
                recordedVideoFile = videoFile
                analysisState = AnalysisState.Idle
                analysisResult = null
            },
            isEnabled = analysisState != AnalysisState.Analyzing,
            patientOrange = patientOrange
        )

        // Show video recorded confirmation
        recordedVideoFile?.let { videoFile ->
            Spacer(modifier = Modifier.height(24.dp))

            // Big confirmation card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Video recorded",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "✅ Video Recorded",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                        Text(
                            text = "Ready for analysis",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF558B2F)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Simple Medication State Selection
            PatientMedicationCard(
                selectedState = medicationState,
                onStateSelected = { medicationState = it },
                patientOrange = patientOrange
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Analysis Section - Patient Friendly
        when (analysisState) {
            is AnalysisState.Idle -> {
                if (recordedVideoFile != null) {
                    // Big Analysis Button
                    Button(
                        onClick = {
                            scope.launch {
                                analysisState = AnalysisState.Analyzing
                                try {
                                    val result = gaitClient.analyzeVideo(recordedVideoFile!!)
                                    result.fold(
                                        onSuccess = { response ->
                                            analysisResult = response
                                            analysisState = AnalysisState.Success
                                        },
                                        onFailure = { exception ->
                                            analysisState = AnalysisState.Error(
                                                exception.message ?: "Analysis failed"
                                            )
                                        }
                                    )
                                } catch (e: Exception) {
                                    analysisState = AnalysisState.Error(
                                        "Unexpected error: ${e.message}"
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp), // Bigger button
                        colors = ButtonDefaults.buttonColors(
                            containerColor = patientOrange
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Start Analysis",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                }
            }

            is AnalysisState.Analyzing -> {
                PatientAnalysisProgressCard(patientOrange)
            }

            is AnalysisState.Success -> {
                analysisResult?.let { result ->
                    PatientFriendlyResultsCard(
                        result = result,
                        patientOrange = patientOrange,
                        onTakeNew = {
                            analysisState = AnalysisState.Idle
                            analysisResult = null
                            recordedVideoFile = null
                            medicationState = "N/A"
                        },
                        onGoHome = {
                            navController.popBackStack()
                        }
                    )
                }
            }

            is AnalysisState.Error -> {
                val errorState = analysisState as AnalysisState.Error
                PatientErrorCard(
                    error = errorState.message,
                    patientOrange = patientOrange,
                    onTryAgain = {
                        analysisState = AnalysisState.Idle
                        analysisResult = null
                        recordedVideoFile = null
                    }
                )
            }
        }
    }
}

@Composable
fun PatientVideoRecordingSection(
    onVideoRecorded: (File) -> Unit,
    isEnabled: Boolean,
    patientOrange: Color
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Big camera icon
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = "Video Recording",
                modifier = Modifier.size(64.dp),
                tint = patientOrange
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Record Your Movement",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Big Record Button
            Button(
                onClick = {
                    // Your existing video recording logic
                },
                enabled = isEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = patientOrange
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Start Recording",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Test Video Button (for development)
            OutlinedButton(
                onClick = {
                    try {
                        val inputStream = context.assets.open("001_color.mp4")
                        val testFile = File(context.cacheDir, "test_video_${System.currentTimeMillis()}.mp4")
                        testFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                        onVideoRecorded(testFile)
                    } catch (e: Exception) {
                        println("Error loading test video: ${e.message}")
                    }
                },
                enabled = isEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                border = BorderStroke(2.dp, patientOrange),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = null,
                    tint = patientOrange
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Use Test Video",
                    color = patientOrange,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun PatientMedicationCard(
    selectedState: String,
    onStateSelected: (String) -> Unit,
    patientOrange: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "💊 Medication Status",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Have you taken your medication today?",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Big medication buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(
                    "On" to "✅ Yes, I took my medication",
                    "Off" to "❌ No, I haven't taken it",
                    "N/A" to "🚫 I don't take medication"
                ).forEach { (state, description) ->
                    Button(
                        onClick = { onStateSelected(state) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedState == state) patientOrange else Color(0xFFF5F5F5),
                            contentColor = if (selectedState == state) Color.White else Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = state,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = description,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PatientAnalysisProgressCard(patientOrange: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Big loading indicator
            CircularProgressIndicator(
                modifier = Modifier.size(80.dp),
                strokeWidth = 8.dp,
                color = patientOrange
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "🤖 Analyzing Your Movement",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Please wait while our AI reviews your video.\nThis will take about 1-2 minutes.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                lineHeight = 24.sp
            )
        }
    }
}

@Composable
fun PatientFriendlyResultsCard(
    result: GaitAnalysisResponse,
    patientOrange: Color,
    onTakeNew: () -> Unit,
    onGoHome: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Results Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎉 Your Results",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Big Severity Display
                PatientSeverityCard(
                    severity = result.severity ?: "Unknown",
                    patientOrange = patientOrange
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Simple metrics in big cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PatientMetricCard(
                        icon = "⏱️",
                        label = "Time",
                        value = "${String.format("%.0f", result.tugMetrics?.totalTime ?: 0.0)}s",
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    PatientMetricCard(
                        icon = "👣",
                        label = "Steps",
                        value = "${result.gaitMetrics?.stepCount ?: 0}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }



        // Big action buttons
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onGoHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Go to Home",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = onTakeNew,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                border = BorderStroke(2.dp, patientOrange),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = patientOrange,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Take New Test",
                    color = patientOrange,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun PatientSeverityCard(
    severity: String,
    patientOrange: Color
) {
    val (emoji, color, message) = when (severity.lowercase()) {
        "normal" -> Triple("😊", Color(0xFF4CAF50), "Great job! Your movement looks good.")
        "slight" -> Triple("🙂", Color(0xFFFFB74D), "Good movement with minor concerns.")
        "mild" -> Triple("😐", Color(0xFFFF9800), "Some movement difficulties noticed.")
        "moderate" -> Triple("😟", Color(0xFFFF7043), "Movement needs attention.")
        "severe" -> Triple("😰", Color(0xFFF44336), "Please see your doctor soon.")
        else -> Triple("🤔", Color.Gray, "Results analyzed.")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        border = BorderStroke(3.dp, color)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = emoji,
                fontSize = 48.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = severity,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                fontSize = 28.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun PatientMetricCard(
    icon: String,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FA)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = icon,
                fontSize = 24.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 14.sp
            )
        }
    }
}


@Composable
fun PatientErrorCard(
    error: String,
    patientOrange: Color,
    onTryAgain: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "😔",
                fontSize = 48.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Oops! Something went wrong",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Don't worry, let's try again!",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onTryAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = patientOrange
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Try Again",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}