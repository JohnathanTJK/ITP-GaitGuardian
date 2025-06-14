package com.example.gaitguardian.screens.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight.Companion.ExtraBold
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gaitguardian.ui.theme.DefaultColor
import com.example.gaitguardian.ui.theme.Heading1
import com.example.gaitguardian.ui.theme.bgColor
import com.example.gaitguardian.ui.theme.body
import com.example.gaitguardian.ui.theme.boxShape
import com.example.gaitguardian.ui.theme.buttonBackgroundColor
import com.example.gaitguardian.ui.theme.cardBackgroundColor
import com.example.gaitguardian.ui.theme.homeIconSize
import com.example.gaitguardian.ui.theme.severityBoxColor
import com.example.gaitguardian.ui.theme.subheading1
import com.example.gaitguardian.viewmodels.PatientViewModel
import java.time.LocalDate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import com.example.gaitguardian.screens.patient.LatestAssessmentResultsCard


@Composable
fun PatientHomeScreen(
    navController: NavController,
    patientViewModel: PatientViewModel,
    modifier: Modifier = Modifier
) {
    // Whole screen container
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        PatientTopBar(navController)

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val patientInfo by patientViewModel.patient.collectAsState()

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Welcome back,",
                    fontSize = body,
                    color = Color(0xFF718096)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = patientInfo?.name ?: "",
                    fontWeight = ExtraBold,
                    fontSize = Heading1,
                    color = Color.Black
                )
            }

            MissedAssessmentCard(navController)

        }

        HomeIcon(navController)
    }
}

@Composable
fun MissedAssessmentCard(navController: NavController) {
    val currentDate = LocalDate.now()

    // Example 1: Missed assessment (before today)
    val assessmentDate = LocalDate.parse("2025-06-10")

    // Example 2: Assessment due today
    // val assessmentDate = LocalDate.now()

    // Example 3: Upcoming assessment (after today)
    // val assessmentDate = LocalDate.parse("2025-06-20")

    val (titleText, dateText, showButton) = when {
        assessmentDate.isBefore(currentDate) -> Triple(
            "Missed Assessment",
            "on 10 June 2025",
            true
        )
        assessmentDate.isEqual(currentDate) -> Triple(
            "Assessment Due",
            "Today",
            true
        )
        else -> Triple(
            "Next Assessment",
            "on 20 June 2025",
            false
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor)
    ) {
        Column {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(25.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = titleText,
                    fontSize = subheading1,
                    color = DefaultColor,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateText,
                    fontSize = Heading1,
                    color = DefaultColor,
                    fontWeight = ExtraBold,
                    textAlign = TextAlign.Center
                )
            }

            if (showButton) {
                Button(
                    onClick = { navController.navigate("gait_assessment_screen") },
                    colors = ButtonDefaults.buttonColors(containerColor = buttonBackgroundColor),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text(
                        text = "Take Assessment",
                        color = DefaultColor,
                        fontSize = Heading1,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

        }
    }
    LatestAssessmentResultsCard(
        previousTiming = 15,
        latestTiming = 20,
        medicationOn = true,
        showDivider = false,
        modifier = Modifier.fillMaxWidth()
    )

}


@Composable
fun HomeIcon(navController: NavController) {
    Button(
        onClick = { /* navController.navigate("home_screen") or whatever destination */ },
        colors = ButtonDefaults.buttonColors(containerColor = buttonBackgroundColor),
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(0.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Home,
            contentDescription = "Home",
            tint = DefaultColor,
            modifier = Modifier
                .size(homeIconSize)

        )
    }

}



