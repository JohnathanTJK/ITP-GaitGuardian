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
import com.example.gaitguardian.viewmodels.PatientViewModel
import com.example.gaitguardian.viewmodels.TugDataViewModel
import kotlinx.coroutines.delay
import java.io.File


@Composable
fun LoadingScreen(
    navController: NavController,
    assessmentTitle: String,
    outputPath: String,
    tugDataViewModel: TugDataViewModel,
    patientViewModel: PatientViewModel
) {
    val videoFile = File(outputPath)
    val scope = rememberCoroutineScope()
    var analysisState by remember { mutableStateOf<AnalysisState>(AnalysisState.Idle) }
    var analysisResult by remember { mutableStateOf<GaitAnalysisResponse?>(null) }
    val gaitClient = remember { GaitAnalysisClient() }

    val motivationalQuotes = listOf(
        "🌟 Keep going, you're doing amazing!",
        "💪 Every step matters. You’ve got this!",
        "🌈 You are stronger than you think.",
        "🕊️ Small progress is still progress.",
        "🧠 Courage doesn’t always roar.\nSometimes it’s the quiet voice that says,\n‘I’ll try again tomorrow.’"
    )
    val randomQuote = remember { motivationalQuotes.random() }

    // 🔁 Start ML analysis
    LaunchedEffect(key1 = videoFile) {
        analysisState = AnalysisState.Analyzing
        try {
            val result = gaitClient.analyzeVideo(videoFile)
            result.fold(
                onSuccess = { response ->
                    analysisResult = response
                    tugDataViewModel.setResponse(response)

                    val tugMetrics = response.tugMetrics
                    val analysis = TUGAnalysis(
                        severity = response.severity ?: "Unknown",
                        timeTaken = tugMetrics?.totalTime ?: 0.0,
                        stepCount = response.gaitMetrics?.stepCount ?: 0,
                        sitToStand = tugMetrics?.sitToStandTime ?: 0.0,
                        walkFromChair = tugMetrics?.walkFromChairTime ?: 0.0,
                        turnFirst = tugMetrics?.turnFirstTime ?: 0.0,
                        walkToChair = tugMetrics?.walkToChairTime ?: 0.0,
                        turnSecond = tugMetrics?.turnSecondTime ?: 0.0,
                        standToSit = tugMetrics?.standToSitTime ?: 0.0
                    )
                    tugDataViewModel.insertTugAnalysis(analysis)

                    if (!patientViewModel.saveVideos.value && videoFile.exists()) {
                        videoFile.delete()
                    }

                    analysisState = AnalysisState.Success
                },
                onFailure = { error ->
                    analysisState = AnalysisState.Error(error.message ?: "Analysis failed.")
                }
            )
        } catch (e: Exception) {
            analysisState = AnalysisState.Error("Unexpected error: ${e.message}")
        }
    }

    // ✅ Navigate on success
    LaunchedEffect(analysisState) {
        if (analysisState == AnalysisState.Success) {
            navController.navigate("result_screen/${assessmentTitle}") {
                popUpTo("loading_screen") { inclusive = true }
            }
        }
    }

    // 🧠 UI
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
            Text(
                text = "Processing your results...",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A148C)
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (analysisState is AnalysisState.Analyzing) {
                CircularProgressIndicator(color = Color(0xFF6A1B9A))
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (analysisState is AnalysisState.Error) {
                Text(
                    text = "❌ ${(analysisState as AnalysisState.Error).message}",
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

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
