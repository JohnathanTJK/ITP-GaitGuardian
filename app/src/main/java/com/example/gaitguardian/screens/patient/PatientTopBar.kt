package com.example.gaitguardian.screens.patient

import androidx.compose.foundation.background
import androidx.navigation.NavController
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Currently not in use anymore, replaced with topBar in NavGraph's Scaffold :D
@Composable
fun PatientTopBar(navController: NavController)  {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
            //.background(Color(0xFFE0E0E0)),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "English",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Black
        )
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Settings",
            modifier = Modifier.size(30.dp)
        )


    }
    // Divider line below the top bar
    HorizontalDivider(
        thickness = 1.dp,
        color = Color.Black
    )
}
