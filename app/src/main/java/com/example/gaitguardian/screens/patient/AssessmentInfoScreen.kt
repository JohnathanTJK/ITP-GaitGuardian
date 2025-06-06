package com.example.gaitguardian.screens.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight.Companion.ExtraBold
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gaitguardian.ui.theme.*

@Composable
fun AssessmentInfoScreen(
    navController: NavController,
    assessmentTitle: String,
    modifier: Modifier = Modifier
) {
    var medicationStatus by remember { mutableStateOf("") }
    var comments by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(screenPadding),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // Top bar
            PatientTopBar(navController)

            Spacer(Modifier.height(spacerLarge))

            Text(
                assessmentTitle,
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(Modifier.height(24.dp))

            Text("Tag your medication status", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            Text(
                "Please tag your medication state before starting the test. This helps us understand how your medication affects your mobility.",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { medicationStatus = "ON" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (medicationStatus == "ON") ButtonActive else Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("ON")
                }

                Button(
                    onClick = { medicationStatus = "OFF" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (medicationStatus == "OFF") ButtonActive else Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("OFF")
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = comments,
                onValueChange = { comments = it },
                placeholder = { Text("Additional comments") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )
        }

        Button(
            onClick = {
                navController.navigate("video_capture_screen")
            },
            colors = ButtonDefaults.buttonColors(containerColor = ButtonActive),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Text("Continue", color = Color.Black)
        }

    }
}
