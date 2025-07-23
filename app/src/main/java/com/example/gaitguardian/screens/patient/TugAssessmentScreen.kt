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

@Composable
fun TugAssessmentScreen(
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),  // Add this line!
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "TUG Assessment",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Instructions Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Instructions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "1. Sit in the chair with back against backrest\n" +
                            "2. Stand up and walk 3 meters\n" +
                            "3. Turn around and walk back\n" +
                            "4. Sit down with back against backrest",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Video Recording Section (keep your existing implementation)
        VideoRecordingSection(
            onVideoRecorded = { videoFile ->
                recordedVideoFile = videoFile
                analysisState = AnalysisState.Idle
                analysisResult = null
            },
            isEnabled = analysisState != AnalysisState.Analyzing
        )

        // Show video recorded confirmation
        recordedVideoFile?.let { videoFile ->
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Video recorded",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Video Recorded",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "File: ${videoFile.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Medication State Selection
            MedicationStateCard(
                selectedState = medicationState,
                onStateSelected = { medicationState = it },
                onShowDialog = { showMedicationDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Comments Section
            OutlinedTextField(
                value = comments,
                onValueChange = { comments = it },
                label = { Text("Comments (Optional)") },
                placeholder = { Text("Any additional observations...") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                enabled = analysisState != AnalysisState.Analyzing
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Analysis Section
        when (analysisState) {
            is AnalysisState.Idle -> {
                if (recordedVideoFile != null) {
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
                            .height(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Analyze Movement",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            is AnalysisState.Analyzing -> {
                AnalysisProgressCard()
            }

            is AnalysisState.Success -> {
                analysisResult?.let { result ->
                    AnalysisResultsCard(
                        result = result,
                        onSaveResults = {
                            // TODO: Save to your existing database
                            // Navigate to results screen or back to patient list
                            navController.popBackStack()
                        },
                        onAnalyzeAgain = {
                            analysisState = AnalysisState.Idle
                            analysisResult = null
                            recordedVideoFile = null
                            medicationState = "N/A"
                            comments = ""
                        }
                    )
                }
            }

            is AnalysisState.Error -> {
                val errorState = analysisState as AnalysisState.Error  // Extract the error state
                AnalysisErrorCard(
                    error = errorState.message,  // Now we can access the message
                    onRetry = {
                        // Retry analysis
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
                    onStartNew = {
                        analysisState = AnalysisState.Idle
                        analysisResult = null
                        recordedVideoFile = null
                    }
                )
            }
        }
    }

    // Medication State Dialog
    if (showMedicationDialog) {
        MedicationStateDialog(
            onDismiss = { showMedicationDialog = false },
            onStateSelected = { state ->
                medicationState = state
                showMedicationDialog = false
            }
        )
    }
}

@Composable
fun AnalysisProgressCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                strokeWidth = 6.dp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Analyzing Movement",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "AI is processing your video...\nThis may take 1-2 minutes",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AnalysisResultsCard(
    result: GaitAnalysisResponse,
    onSaveResults: () -> Unit,
    onAnalyzeAgain: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            // Header
            Text(
                text = "Assessment Results",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Severity Rating
            SeverityRatingCard(severity = result.severity ?: "Unknown")

            Spacer(modifier = Modifier.height(16.dp))

            // Key Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(
                    label = "Total Time",
                    value = "${String.format("%.1f", result.tugMetrics?.totalTime ?: 0.0)}s",
                    modifier = Modifier.weight(1f)
                )

                MetricItem(
                    label = "Steps",
                    value = "${result.gaitMetrics?.stepCount ?: 0}",
                    modifier = Modifier.weight(1f)
                )

                MetricItem(
                    label = "Cadence",
                    value = "${String.format("%.0f", result.gaitMetrics?.cadence ?: 0.0)}/min",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onSaveResults,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Results")
                }

                OutlinedButton(
                    onClick = onAnalyzeAgain,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Test")
                }
            }
        }
    }
}

@Composable
fun SeverityRatingCard(severity: String) {
    val severityColor = when (severity.lowercase()) {
        "normal" -> Color.Green
        "slight" -> Color(0xFFFFB74D) // Orange
        "mild" -> Color(0xFFFF8A65) // Light red
        "moderate" -> Color(0xFFFF7043) // Red orange
        "severe" -> Color.Red
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = severityColor.copy(alpha = 0.1f)
        ),
        border = BorderStroke(2.dp, severityColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Severity Level",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = severity,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = severityColor
            )
        }
    }
}

@Composable
fun MetricItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AnalysisErrorCard(
    error: String,
    onRetry: () -> Unit,
    onStartNew: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Analysis Failed",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Retry")
                }

                OutlinedButton(
                    onClick = onStartNew,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("New Video")
                }
            }
        }
    }
}


@Composable
fun MedicationStateCard(
    selectedState: String,
    onStateSelected: (String) -> Unit,
    onShowDialog: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Medication State",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("On", "Off", "N/A").forEach { state ->
                    FilterChip(
                        onClick = { onStateSelected(state) },
                        label = { Text(state) },
                        selected = selectedState == state,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onShowDialog,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("What does this mean?")
            }
        }
    }
}

@Composable
fun MedicationStateDialog(
    onDismiss: () -> Unit,
    onStateSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Medication State")
        },
        text = {
            Column {
                Text(
                    text = "Select the patient's current medication state:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "• ON: Patient has taken their Parkinson's medication",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "• OFF: Patient has not taken medication or it has worn off",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "• N/A: Not applicable (no Parkinson's medication)",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        }
    )
}

// Keep your existing VideoRecordingSection component
// This is just a placeholder - use your actual implementation
@Composable
fun VideoRecordingSection(
    onVideoRecorded: (File) -> Unit,
    isEnabled: Boolean
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = "Video Recording",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Video Recording",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Your existing camera/recording button
            Button(
                onClick = {
                    // Your existing video recording logic
                },
                enabled = isEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Recording")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ADD THIS TEST BUTTON FOR DEVELOPMENT
            OutlinedButton(
                onClick = {
                    try {
                        // Copy test video from assets to cache directory
                        val inputStream = context.assets.open("001_color.mp4")
                        val testFile = File(context.cacheDir, "test_video_${System.currentTimeMillis()}.mp4")

                        testFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }

                        onVideoRecorded(testFile)
                    } catch (e: Exception) {
                        // Handle error
                        println("Error loading test video: ${e.message}")
                    }
                },
                enabled = isEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Use Test Video")
            }
        }
    }
}