package com.example.gaitguardian.screens.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight.Companion.ExtraBold
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gaitguardian.ui.theme.*

@Composable
fun PatientHomeScreen(navController: NavController, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PatientTopBar(navController)
        Spacer(Modifier.height(30.dp))

        GreetingText()
        Spacer(Modifier.height(30.dp))

        MissedAssessmentCard(navController)
        Spacer(Modifier.height(20.dp))

        StatusRow()
        Spacer(Modifier.height(20.dp))

        TimingSection()
        Spacer(Modifier.height(8.dp))

        ProgressBarSection()
        Spacer(Modifier.height(8.dp))

        PreviousTimingText()
        Spacer(Modifier.weight(1f))

        HomeIcon(navController)
    }
}

@Composable
fun GreetingText() {
    Text(
        text = "Welcome Back, Sophia",
        fontWeight = ExtraBold,
        fontSize = Heading1,
        color = DefaultColor
    )
}

@Composable
fun MissedAssessmentCard(navController: NavController) {
    Card(
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(cardPadding)
        ) {
            Text(
                text = "Missed Assessment set on",
                fontSize = subheading1,
                color = DefaultColor,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "20 May 2025",
                fontWeight = ExtraBold,
                fontSize = Heading2,
                color = DefaultColor,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(30.dp))
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

@Composable
fun StatusRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusBox(title = "Severity", value = "2")
        StatusBox(title = "Medication", value = "ON")
    }
}

@Composable
fun StatusBox(title: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(severityBoxColor, boxShape)
            .padding(boxPadding)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
fun TimingSection() {
    Text("Current Timing: 15 seconds", fontSize = body, color = DefaultColor)
    Text("+2 Secs", fontSize = body, color = DefaultColor)
}

@Composable
fun ProgressBarSection() {
    Row(Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .weight(0.3f)
                .height(progressBarHeight)
                .background(progressGreen)
        )
        Box(
            modifier = Modifier
                .weight(0.3f)
                .height(progressBarHeight)
                .background(progressYellow)
        )
        Box(
            modifier = Modifier
                .weight(0.4f)
                .height(progressBarHeight)
                .background(progressRed)
        )
    }

    Box(
        modifier = Modifier
            .height(progressBarHeight)
            .offset(x = progressMarkerOffset)
            .width(progressMarkerWidth)
            .background(progressMarkerColor)
    )
}

@Composable
fun PreviousTimingText() {
    Text("Previous Timing: 13 seconds", fontSize = body, color = DefaultColor)
}

@Composable
fun HomeIcon(navController: NavController) {
    Button(
        onClick = { /* navController.navigate("home_screen") or whatever destination */ },
        colors = ButtonDefaults.buttonColors(containerColor = buttonBackgroundColor),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
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