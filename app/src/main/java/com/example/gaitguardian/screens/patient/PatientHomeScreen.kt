package com.example.gaitguardian.screens.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight.Companion.ExtraBold
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.text.style.TextAlign
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
        // Top Row: Language & Settings Icon
        PatientTopBar(navController)

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            "Welcome Back, Sophia",
            fontWeight = ExtraBold,
            fontSize = Heading1,
            color = DefaultColor
        )

        Spacer(modifier = Modifier.height(30.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(cardPadding)) {
                Text(
                    "Missed Assessment set on",
                    fontSize = subheading1,
                    color = DefaultColor,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "20 May 2025",
                    fontWeight = ExtraBold,
                    fontSize = Heading2,
                    color = DefaultColor,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(30.dp))
                Button(
                    onClick = { navController.navigate("gait_assessment_screen") },
                    colors = ButtonDefaults.buttonColors(containerColor = buttonBackgroundColor)
                ) {
                    Text("Take Assessment", color = DefaultColor, fontSize = Heading1)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(severityBoxColor, boxShape)
                    .padding(boxPadding)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Severity",
                        fontWeight = ExtraBold,
                        color = DefaultColor,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "2",
                        fontSize = Heading1,
                        color = DefaultColor,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(severityBoxColor, boxShape)
                    .padding(boxPadding)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Medication",
                        fontWeight = ExtraBold,
                        color = DefaultColor,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "ON",
                        fontSize = Heading1,
                        color = DefaultColor,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("Current Timing: 15 seconds", fontSize = body, color = DefaultColor)
        Text("+2 Secs", fontSize = body, color = DefaultColor)

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
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

        Spacer(modifier = Modifier.height(8.dp))

        Text("Previous Timing: 13 seconds", fontSize = body, color = DefaultColor)

        Spacer(modifier = Modifier.weight(1f))

        Icon(
            Icons.Default.Home,
            contentDescription = "Home",
            modifier = Modifier
                .size(homeIconSize)
                .padding(homeIconPadding)
        )
    }
}

// ---------- Styling constants below -------------

