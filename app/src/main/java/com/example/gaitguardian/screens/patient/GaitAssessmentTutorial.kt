package com.example.gaitguardian.screens.patient

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gaitguardian.ui.theme.ButtonActive
import com.example.gaitguardian.ui.theme.Heading1
import com.example.gaitguardian.ui.theme.body
import java.util.Locale


@Composable
fun GaitAssessmentTutorial(onClose: () -> Unit) {
    var page by remember { mutableStateOf(1) }
    val context = LocalContext.current
    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    var ttsReady by remember { mutableStateOf(false) }

    // Setup TTS
    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(0.6f)
                ttsReady = true
            }
        }
    }

    // Speak text when page changes
    LaunchedEffect(page, ttsReady) {
        if (!ttsReady) return@LaunchedEffect
        val speechText = when (page) {
            1 -> "Choose an assessment to begin the test. Stand-Walk Test or Five Times Sit to Stand"
            2 -> "Tag your medication status and select any additional comments if necessary. When done, click continue."
            3 -> "Camera requirement tutorial. Follow the instructions to position the camera correctly for the test."
            else -> ""
        }
        if (speechText.isNotEmpty()) tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, "tutorialTTS")
    }


    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(15.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = when (page) {
                        1 -> "📋"
                        2 -> "📝"
                        else -> "📷"
                    },
                    fontSize = Heading1 * 1.5f, // slightly bigger for icon
                    textAlign = TextAlign.Center
                )
                Text(
                    text = when (page) {
                        1 -> "Gait Assessment"
                        2 -> "Assessment Info"
                        else -> "Camera Requirement"
                    },
                    fontSize = Heading1 * 1.3f,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // --- Page Content ---
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (page) {
                        1 -> {
                            Text("Select an assessment", fontSize = body * 1.2f, color = Color.Black)

                            // Fake buttons (unclickable)
                            listOf("Stand-Walk Test", "Five Times Sit to Stand").forEach { text ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .background(ButtonActive.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text, color = Color.Black, fontSize = Heading1 * 1.0f, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                }
                            }
                        }
                        2 -> {
                            Text("1. Tag your medication status (ON / OFF).", fontSize = body * 1.2f, color = Color.Black)
                            Text("2. Select any additional comments.", fontSize = body * 1.2f, color = Color.Black)
                            Text("3. Click Continue when done.", fontSize = body * 1.2f, color = Color.Black)

                            // Fake buttons (unclickable)
                            listOf("ON", "OFF").forEach { text ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .background(ButtonActive.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text, color = Color.Black, fontSize = Heading1 * 1.0f, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                }
                            }
                        }
                        3 -> {
                            Text("1. Ensure your camera is set up.", fontSize = body * 1.2f, color = Color.Black)
                            Text("2. Make sure lighting is good.", fontSize = body * 1.2f, color = Color.Black)
                            Text("3. Click Start Assessment.", fontSize = body * 1.2f, color = Color.Black)
                            // No fake buttons for camera page
                        }
                    }
                }

                // --- Next / Finish Button ---
                Button(
                    onClick = {
                        if (page < 3) page++ else {
                            // 👇 Stop speaking before closing
                            tts?.stop()
                            tts?.shutdown()
                            onClose()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonActive),
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(70.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = if (page < 3) "Next" else "Finish",
                        fontSize = Heading1 * 1.2f,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}