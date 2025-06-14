package com.example.gaitguardian.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.gaitguardian.viewmodels.ClinicianViewModel
import com.example.gaitguardian.viewmodels.PatientViewModel

@Composable
fun StartScreen(navController: NavController, patientViewModel: PatientViewModel, clinicianViewModel: ClinicianViewModel, modifier: Modifier = Modifier) {
    Column{
        Text("Welcome to GaitGuardian")
        Button(
            onClick = {
                // Store the selected view into DataStorePreferences
                clinicianViewModel.saveCurrentUserView("clinician")
                navController.navigate("clinician_graph")}
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
            onClick = {clinicianViewModel.insertFirstClinician()}
        )
        {
            Text("Insert Clinician Info 'Bob Bobby'  FOR TESTING")
        }
        Button(
            onClick = {
                // Store the selected view into DataStorePreferences
                patientViewModel.saveCurrentUserView("patient")
                navController.navigate("patient_graph")}
        ) {
            Text("I am patient")
        }
        Button(
            onClick = {clinicianViewModel.deleteAll()}
        ) {
            Text("DELETE CLINICIAN TABLE")
        }
        Button(
            onClick = {clinicianViewModel.updateId(1, "Bob Bobby")}
        ) {
            Text("update BobbyId")
        }
    }
}