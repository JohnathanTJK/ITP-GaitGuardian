package com.example.gaitguardian.screens.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gaitguardian.ui.theme.ButtonActive
import com.example.gaitguardian.ui.theme.bgColor
import com.example.gaitguardian.viewmodels.PatientViewModel
import kotlinx.coroutines.delay

@Composable
fun ManageVideoPrivacyScreen(navController: NavController, patientViewModel: PatientViewModel) {
    var selectedPreference by remember { mutableStateOf<Boolean?>(null) }
    var showConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(showConfirmation) {
        if (showConfirmation) {
            delay(2000)
            navController.navigate("new_cam_screen") {
                popUpTo("video_privacy_screen") { inclusive = true }
            }
        }
    }
    Column(modifier = Modifier.background(bgColor).fillMaxSize().padding(16.dp)) {
        Text("Manage Video Privacy", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Black)
        Spacer(modifier = Modifier.padding(8.dp))

        Text("Since it is your first time recording a video using this application, please indicate your video privacy preference.",
            fontWeight = FontWeight.Medium, color = Color.Black)
        Spacer(modifier = Modifier.padding(8.dp))

        Text("Would you like to view your videos in your device after each assessment?",
            fontWeight = FontWeight.Medium, color = Color.Black)

        Spacer(modifier = Modifier.padding(16.dp))

        // Privacy preference selection
        Text("Select your preference:", fontWeight = FontWeight.Medium, color = Color.Black)
        Spacer(modifier = Modifier.padding(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Enable videos button
            Button(
                onClick = { selectedPreference = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedPreference == true) ButtonActive else Color(0xFFFFE299)
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("Enable Video Hiding")
            }

            // Disable videos button
            Button(
                onClick = { selectedPreference = false },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedPreference == false) ButtonActive else Color(0xFFFFE299)
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("Disable Video Hiding")
            }
        }

        Spacer(modifier = Modifier.padding(16.dp))

        // Confirmation section
        if (selectedPreference != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        buildAnnotatedString {
                            append("You have selected to ")
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(if (selectedPreference == true) "ENABLE" else "DISABLE")
                            }
                            append(" video hiding.")
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.padding(8.dp))
                    if(!showConfirmation)
                    {
                        Button(
                            onClick = {
                                selectedPreference?.let { preference ->
                                    patientViewModel.setSaveVideos(preference)
                                    patientViewModel.setFirstPrivacyCheck(true)
                                    showConfirmation = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ButtonActive)
                        ) {
                            Text("Confirm Selection")
                        }
                    } else { // Pressed button / success message here
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFD1FAE5)
                            ),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Preference saved successfully!",
                                    color = Color(0xFF38A169),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Redirecting you to record your assessment",
                                    color = Color(0xFF38A169),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
