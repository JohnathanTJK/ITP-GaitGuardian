package com.example.gaitguardian.screens.patient

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

@Composable
fun TugAssessmentScreen(navController: NavController, modifier: Modifier = Modifier) {
    Column {
        Text("TUG")

        Button(
            onClick = { navController.popBackStack() },
            modifier = modifier.fillMaxWidth()
        ) {
            Text("Go Back")
        }

    }
}