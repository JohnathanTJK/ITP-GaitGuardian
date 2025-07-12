package com.example.gaitguardian.screens.patient

import androidx.compose.foundation.background
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
import com.example.gaitguardian.viewmodels.PatientViewModel

@Composable
fun AssessmentInfoScreen(
    navController: NavController,
    assessmentTitle: String,
    modifier: Modifier = Modifier,
    patientViewModel: PatientViewModel
) {
    val firstPrivacyCheck by patientViewModel.firstPrivacyCheck.collectAsState()
    val comments by patientViewModel.assessmentComment.collectAsState()
    val onMedication by patientViewModel.onMedication.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(screenPadding),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {

            Spacer(Modifier.height(spacerLarge))

            Text(
                assessmentTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.Black  // Set text color to black
            )

            Spacer(Modifier.height(24.dp))

            Text(
                "Tag your medication status",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Black
            )
            Spacer(Modifier.height(8.dp))

            Text(
                "Please tag your medication state before starting the test. This helps us understand how your medication affects your mobility.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black
            )

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MedicationStatusButton(
                    text = "ON",
//                    isSelected = medicationStatus == "ON",
//                    onClick = { patientViewModel.setMedicationStatus("ON") },
                    isSelected = onMedication,
                    onClick = { patientViewModel.setOnMedication(true) },
                    modifier = Modifier.weight(1f)
                )
                MedicationStatusButton(
                    text = "OFF",
//                    isSelected = medicationStatus == "OFF",
//                    onClick = { patientViewModel.setMedicationStatus("OFF") },
                    isSelected = !onMedication,
                    onClick = { patientViewModel.setOnMedication(false) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = comments,
                onValueChange = { patientViewModel.setAssessmentComment(it) },
                placeholder = { Text("Additional comments", color = Color.Black.copy(alpha = 0.5f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                textStyle = LocalTextStyle.current.copy(color = Color.Black)
            )
        }

        Button(
            onClick = {
                if(!firstPrivacyCheck){
                    navController.navigate("video_privacy_screen")
                }
                else{
                    navController.navigate("camera_screen")
                }
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

@Composable
fun MedicationStatusButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) ButtonActive else Color.White,
            contentColor = Color.Black
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Text(text, color = Color.Black)
    }
}
