package com.example.gaitguardian.screens.patient

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

@Composable
fun FtfsAssessmentScreen(navController: NavController, modifier: Modifier = Modifier) {
    Column {
        Text("Ftfs")

        Button(
            onClick = { navController.popBackStack() },
            modifier = modifier.fillMaxWidth()
        ) {
            Text("Go Back")
        }

    }
}