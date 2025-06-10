package com.example.gaitguardian.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.gaitguardian.viewmodels.PatientViewModel

@Composable
fun StartScreen(navController: NavController, patientViewModel: PatientViewModel, modifier: Modifier = Modifier) {
    Column{
        Text("Welcome to GaitGuardian")
        Button(
            onClick = {navController.navigate("clinician_graph")}
        ) {
            Text("I am Clinician")
        }
        Button(
            onClick = {patientViewModel.insertFirstPatient()}
        )
        {
            Text("Insert Patient Info 'Sophia Tan' into RoomDB")
        }
        Button(
            onClick = {navController.navigate("patient_graph")}
        ) {
            Text("I am patient")
        }
    }
}