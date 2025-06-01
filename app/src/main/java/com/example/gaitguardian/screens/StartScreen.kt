package com.example.gaitguardian.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

@Composable
fun StartScreen(navController: NavController, modifier: Modifier = Modifier) {
    Column{
        Text("Welcome to GaitGuardian")
        Button(
            onClick = {navController.navigate("clinician_graph")}
        ) {
            Text("I am Clinician")
        }
        Button(
            onClick = {navController.navigate("patient_graph")}
        ) {
            Text("I am patient")
        }
    }
}