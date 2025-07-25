package com.example.gaitguardian.screens.patient

import android.text.Layout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gaitguardian.api.GaitAnalysisClient
import com.example.gaitguardian.api.GaitAnalysisResponse
import com.example.gaitguardian.data.roomDatabase.tug.TUGAnalysis
import com.example.gaitguardian.viewmodels.TugDataViewModel
import kotlinx.coroutines.delay
import java.io.File


@Composable
//fun LoadingScreen(navController: NavController, assessmentTitle: String) {
//fun LoadingScreen(navController: NavController, assessmentTitle: String, outputPath: String) {
fun LoadingScreen(navController: NavController, assessmentTitle: String, outputPath: String, tugDataViewModel: TugDataViewModel) {
//fun LoadingScreen(navController: NavController, recordingTime: Int) {

        val motivationalQuotes = listOf(
        "🌟 Keep going, you're doing amazing!",
        "💪 Every step matters. You’ve got this!",
        "🌈 You are stronger than you think.",
        "🕊️ Small progress is still progress.",
        "🧠 Courage doesn’t always roar.\nSometimes it’s the quiet voice that says,\n‘I’ll try again tomorrow.’"
    )

    val randomQuote = remember { motivationalQuotes.random() }
    // Navigate after delay
//    LaunchedEffect(Unit) {
//        delay(600)
////        navController.navigate("result_screen/${assessmentTitle}")
//        navController.navigate("result_screen/${assessmentTitle}")
//        {
//            popUpTo("assessment_info_screen/$assessmentTitle") {
//                inclusive = false
//            }
//        }
//    }

//    // For ML Processing
    val videoFile = File(outputPath)
    if(videoFile.exists())
    {
        Text("OK")
    }
    val scope = rememberCoroutineScope()
    var analysisState by remember { mutableStateOf<AnalysisState>(AnalysisState.Idle) }
    var analysisResult by remember { mutableStateOf<GaitAnalysisResponse?>(null) }
    val gaitClient = remember { GaitAnalysisClient() } // your API client
    LaunchedEffect(key1 = videoFile) {
        analysisState = AnalysisState.Analyzing
        try {
            val result = gaitClient.analyzeVideo(videoFile)
            result.fold(
                onSuccess = { response ->
                    analysisResult = response
                    analysisResult?.severity?.let { severity ->
                        val tugMetrics = analysisResult!!.tugMetrics
                        if (tugMetrics != null) {
                            val analysis = TUGAnalysis(
                                severity = severity,
                                timeTaken = tugMetrics.totalTime,
                                stepCount = analysisResult!!.gaitMetrics?.stepCount ?: 0,
                                sitToStand = tugMetrics.sitToStandTime,
                                walkFromChair = tugMetrics.walkFromChairTime,
                                turnFirst = tugMetrics.turnFirstTime,
                                walkToChair = tugMetrics.walkToChairTime,
                                turnSecond = tugMetrics.turnSecondTime,
                                standToSit = tugMetrics.standToSitTime
                            )
                            tugDataViewModel.insertTugAnalysis(analysis)
                        }
                    }

                    tugDataViewModel.setResponse(response)
                    analysisState = AnalysisState.Success
                    // Optionally navigate to results screen here
                },
                onFailure = { error ->
                    analysisState = AnalysisState.Error(error.message ?: "Failed")
                }
            )
        } catch (e: Exception) {
            analysisState = AnalysisState.Error("Unexpected: ${e.message}")
        }
    }
// 🔒 Trigger navigation only once
    LaunchedEffect(analysisState) {
        if (analysisState == AnalysisState.Success) {
            navController.navigate("result_screen/${assessmentTitle}") {
                popUpTo("loading_screen/${assessmentTitle}/${outputPath}") {
                    inclusive = true
                }
            }
        }
    }
//    when (analysisState) {
//        AnalysisState.Idle -> Text("Waiting...")
//        AnalysisState.Analyzing -> CircularProgressIndicator()
//        is AnalysisState.Error -> Text("Error: ${(analysisState as AnalysisState.Error).message}")
////        AnalysisState.Success -> Text("Analysis Complete! Severity: ${analysisResult?.severity}")
//    AnalysisState.Success -> navController.navigate("result_screen/${assessmentTitle}")
//    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFEDE7F6), Color(0xFFF3E5F5)))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Processing your results...",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A148C)
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = randomQuote,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF6A1B9A)
                        )
                    )
                }
            }
        }
    }
}

