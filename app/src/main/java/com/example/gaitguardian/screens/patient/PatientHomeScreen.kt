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
                    text = patientInfo?.name ?: "Sophia Tan",
                    fontWeight = ExtraBold,
                    fontSize = Heading1,
                    color = Color.Black
                )
            }

            MissedAssessmentCard(navController)
            LatestAssessmentResultsCard()
        }

//        HomeIcon(navController)
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

}
@Composable
fun LatestAssessmentResultsCard(
    previousTiming: Int = 13, // example previous timing in seconds
    latestTiming: Int = 15    // example latest timing in seconds
) {
    val maxVal = maxOf(previousTiming, latestTiming, 30) // ensure some max for scaling

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
                    text = "Latest Assessment Results",
                    fontSize = subheading1,
                    color = DefaultColor,
                    textAlign = TextAlign.Center
                )
            }

            // Row for Severity and Medication status boxes
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                StatusBox(title = "Severity", value = "2", modifier = Modifier.weight(1f))
                StatusBox(title = "Medication", value = "ON", modifier = Modifier.weight(1f))
            }

            //Graphical representation of assessment timings
            HorizontalDivider(thickness = 1.dp, color = Color.LightGray)

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalProgressBar(
                previousValue = 12f,
                latestValue = 18f,
                maxValue = 30f,
                modifier = Modifier.padding(horizontal = 16.dp)
            )


        }
    }
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

@Composable
fun StatusBox(title: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(severityBoxColor, boxShape)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,verticalArrangement = Arrangement.Center) {
            Text(
                text = title,
                fontWeight = ExtraBold,
                color = DefaultColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = value,
                fontSize = Heading1,
                color = DefaultColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun HorizontalProgressBar(
    previousValue: Float,
    latestValue: Float,
    maxValue: Float = 30f,
    modifier: Modifier = Modifier
) {
    val totalFraction = (previousValue + latestValue) / maxValue
    val prevFraction = previousValue / maxValue
    val latestFraction = latestValue / maxValue

    Column(modifier = modifier) {
        // Progress bar container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .background(Color.LightGray, RoundedCornerShape(8.dp))
        ) {
            // Previous segment
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(prevFraction)
                    .background(Color.Gray, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
            )
            // Latest segment
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(latestFraction)
                    .background(Color(0xFF4CAF50), RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                    .align(Alignment.CenterStart)
                    .offset(x = with(LocalDensity.current) { (prevFraction * (LocalConfiguration.current.screenWidthDp.dp.toPx())).toDp() })
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Labels below bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Prev: ${previousValue.toInt()}s", color = Color.Gray)
            Text("Now: ${latestValue.toInt()}s", color = Color(0xFF4CAF50))
        }
    }
}


