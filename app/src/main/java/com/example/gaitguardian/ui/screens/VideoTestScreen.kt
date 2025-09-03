package com.example.gaitguardian.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gaitguardian.api.GaitAnalysisClient
import com.example.gaitguardian.data.models.TugResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoTestScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var processingStatus by remember { mutableStateOf("Ready to process video") }
    var results by remember { mutableStateOf<TugResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val gaitAnalysisClient = remember { GaitAnalysisClient(context) }
    
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedVideoUri = uri
        results = null
        errorMessage = null
        processingStatus = if (uri != null) "Video selected: ${uri.lastPathSegment}" else "No video selected"
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Video Gait Analysis Test",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "1. Select Video",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Button(
                    onClick = { videoPickerLauncher.launch("video/*") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing
                ) {
                    Text("Choose Video File")
                }
                
                if (selectedVideoUri != null) {
                    Text(
                        text = "Selected: ${selectedVideoUri?.lastPathSegment ?: "Unknown"}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "2. Process Video",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Button(
                    onClick = {
                        selectedVideoUri?.let { uri ->
                            coroutineScope.launch {
                                android.util.Log.d("VideoTestScreen", "🎯 BUTTON CLICKED - Starting analysis for URI: $uri")
                                android.util.Log.e("VideoTestScreen", "🚨 VIDEO_TEST_SCREEN: ANALYSIS BUTTON CLICKED")
                                isProcessing = true
                                errorMessage = null
                                processingStatus = "Processing video..."
                                
                                try {
                                    android.util.Log.d("VideoTestScreen", "🎯 Calling gaitAnalysisClient.analyzeVideo()...")
                                    android.util.Log.e("VideoTestScreen", "🚨 ABOUT TO CALL gaitAnalysisClient.analyzeVideo()")
                                    processingStatus = "Extracting poses..."
                                    val analysisResult = gaitAnalysisClient.analyzeVideo(uri)
                                    android.util.Log.e("VideoTestScreen", "🚨 analyzeVideo RETURNED: $analysisResult")
                                    
                                    android.util.Log.d("VideoTestScreen", "🎯 Analysis completed successfully!")
                                    results = analysisResult
                                    processingStatus = "Processing completed successfully!"
                                } catch (e: Exception) {
                                    android.util.Log.e("VideoTestScreen", "🎯 Analysis failed: ${e.message}", e)
                                    errorMessage = "Error: ${e.message}"
                                    processingStatus = "Processing failed"
                                } finally {
                                    android.util.Log.d("VideoTestScreen", "🎯 Analysis finished, setting isProcessing = false")
                                    isProcessing = false
                                }
                            }
                        } ?: run {
                            android.util.Log.w("VideoTestScreen", "🎯 Button clicked but no video URI selected!")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing && selectedVideoUri != null
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isProcessing) "Processing..." else "Analyze Video")
                }
                
                Text(
                    text = processingStatus,
                    fontSize = 12.sp,
                    color = if (errorMessage != null) MaterialTheme.colorScheme.error 
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Error Message
        errorMessage?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        
        // Results Display
        results?.let { result ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Analysis Results",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Divider()
                    
                    Text(
                        text = "Total Duration: ${String.format("%.2f", result.totalDuration)} seconds",
                        fontSize = 14.sp
                    )
                    
                    Text(
                        text = "Sit-to-Stand Duration: ${String.format("%.2f", result.sitToStandDuration)} seconds",
                        fontSize = 14.sp
                    )
                    
                    Text(
                        text = "Walking Duration: ${String.format("%.2f", result.walkingDuration)} seconds",
                        fontSize = 14.sp
                    )
                    
                    Text(
                        text = "Stand-to-Sit Duration: ${String.format("%.2f", result.standToSitDuration)} seconds",
                        fontSize = 14.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Risk Assessment: ${result.riskAssessment}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = when (result.riskAssessment.lowercase()) {
                            "low" -> MaterialTheme.colorScheme.primary
                            "moderate" -> MaterialTheme.colorScheme.tertiary
                            "high" -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                    
                    Text(
                        text = "Analysis Date: ${result.analysisDate}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Phase-by-phase breakdown
                    if (result.phaseBreakdown.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Phase Breakdown:",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        
                        result.phaseBreakdown.forEach { (phase, duration) ->
                            Text(
                                text = "• $phase: ${String.format("%.2f", duration)}s",
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
