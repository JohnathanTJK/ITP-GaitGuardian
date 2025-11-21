package com.example.gaitguardian.screens.patient

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gaitguardian.ui.theme.ButtonActive
import com.example.gaitguardian.ui.theme.Heading1
import com.example.gaitguardian.ui.theme.body
import java.util.Locale

@Composable
fun PatientTutorialScreen(onClose: () -> Unit) {
    var page by remember { mutableStateOf(1) }
    val context = LocalContext.current
    // Hold TTS instance
    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    var ttsReady by remember { mutableStateOf(false) }

    // Initialize TTS once
    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(0.6f)
                ttsReady = true
            }
        }
    }

    // Speak whenever page changes (only when TTS is ready)
    LaunchedEffect(page, ttsReady) {
        if (!ttsReady) return@LaunchedEffect
        val speechText = if (page == 1) {
            "Tap on the Record Video button to start recording your gait assessment. Follow the instructions"
        } else {
            "Tap on settings to adjust your preferences on video privacy"
        }
        tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, "tutorialTTS")
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
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                if (page == 1) {
                    Text(
                        "📹 Record Video",
                        fontSize = Heading1 * 1.3f,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Tap on the Record Video button to start recording your gait assessment. Follow the instructions", fontSize = body * 1.2f, color = Color.Black)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp)
                            .background(ButtonActive.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Record Video", color = Color.Black, fontSize = Heading1 * 1.2f, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        "⚙️ Settings",
                        fontSize = Heading1 * 1.3f,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Tap on settings to adjust your preferences on video privacy", fontSize = body * 1.2f, color = Color.Black)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp)
                            .background(ButtonActive.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier.size(28.dp),
                            tint = Color.Black
                        )
                    }
                }

                Button(
                    onClick = {
                        if (page == 1) page = 2 else {
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
                    Text(if (page == 1) "Next" else "Finish", fontSize = Heading1 * 1.2f, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
