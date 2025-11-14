package com.example.gaitguardian.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gaitguardian.data.roomDatabase.clinician.Clinician
import com.example.gaitguardian.data.roomDatabase.patient.Patient
import com.example.gaitguardian.ui.theme.ButtonActive
import com.example.gaitguardian.ui.theme.bgColor
import com.example.gaitguardian.viewmodels.ClinicianViewModel
import com.example.gaitguardian.viewmodels.PatientViewModel
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.mutableIntStateOf

@Composable
fun StartScreen(
    navController: NavController,
    patientViewModel: PatientViewModel,
    clinicianViewModel: ClinicianViewModel,
    modifier: Modifier = Modifier
) {
    var patientName by remember { mutableStateOf("") }
    var patientAge by remember { mutableStateOf("") }
    var patientConfirmed by remember { mutableStateOf(false) }
    var clinicianName by remember { mutableStateOf("") }
    var clinicianPin by remember { mutableStateOf("") }
    var clinicianConfirmed by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(clinicianConfirmed) {
        if (clinicianConfirmed) {
            successMessage = "Patient and Clinician information saved successfully!"
            patientViewModel.saveCurrentUserView("patient")
            delay(2000)
            navController.navigate("patient_graph") {
                popUpTo("start_screen") { inclusive = true }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to GaitGuardian",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        if (!patientConfirmed) {
            PatientInfoSection(
                patientName = patientName,
                patientAge = patientAge,
                errorMessage = errorMessage,
                onPatientNameChange = {
                    patientName = it
                    errorMessage = ""
                },
                onPatientAgeChange = {
                    patientAge = it
                    errorMessage = ""
                },
                onConfirm = {
                    when {
                        patientName.isBlank() -> {
                            errorMessage = "Please enter your name"
                        }
                        patientAge.isBlank() -> {
                            errorMessage = "Please enter your age"
                        }
                        patientAge.toIntOrNull() == null -> {
                            errorMessage = "Please enter a valid age"
                        }
                        patientAge.toIntOrNull()!! <= 0 -> {
                            errorMessage = "Please enter a valid age"
                        }
                        else -> {
                            val newPatient = Patient(
                                name = patientName.trim(),
                                age = patientAge.toInt()
                            )
                            patientViewModel.insertPatient(newPatient)
                            patientConfirmed = true
                            errorMessage = ""
                        }
                    }
                }
            )
        } else {
            ClinicianInfoSection(
                clinicianName = clinicianName,
                clinicianPin = clinicianPin,
                errorMessage = errorMessage,
                successMessage = successMessage,
                onClinicianNameChange = {
                    clinicianName = it
                    errorMessage = ""
                },
                onClinicianPinChange = {
                    clinicianPin = it
                    errorMessage = ""
                },
                onConfirm = {
                    when {
                        clinicianName.isBlank() -> {
                            errorMessage = "Please enter clinician name"
                        }
                        clinicianPin.isBlank() -> {
                            errorMessage = "Please enter clinician PIN"
                        }
                        clinicianPin.length != 4 || clinicianPin.toIntOrNull() == null -> {
                            errorMessage = "PIN must be a 4-digit number"
                        }
                        else -> {
                            val newClinician = Clinician(name = clinicianName.trim(), pin = clinicianPin.trim())
                            clinicianViewModel.insertClinician(newClinician)
                            clinicianConfirmed = true
                            errorMessage = ""
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun PatientInfoSection(
    patientName: String,
    patientAge: String,
    errorMessage: String,
    onPatientNameChange: (String) -> Unit,
    onPatientAgeChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Patient Information",
            fontSize = 24.sp,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Please enter your information to get started",
            fontSize = 16.sp,
            color = Color.DarkGray,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        if (errorMessage.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF5F5)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = Color(0xFFE53E3E)
                )
            ) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFE53E3E) ,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        InfoTextField(
            value = patientName,
            onValueChange = onPatientNameChange,
            label = "Enter your name here...",
            modifier = Modifier.padding(bottom = 16.dp)
        )

        InfoTextField(
            value = patientAge,
            onValueChange = onPatientAgeChange,
            label = "Enter your age here...",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(
            onClick = onConfirm,
            colors = ButtonDefaults.buttonColors(containerColor = ButtonActive),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "Confirm",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun ClinicianInfoSection(
    clinicianName: String,
    clinicianPin: String,
    errorMessage: String,
    successMessage: String,
    onClinicianNameChange: (String) -> Unit,
    onClinicianPinChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Clinician Information",
            fontSize = 24.sp,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Now, please enter your clinician information",
            fontSize = 16.sp,
            color = Color.DarkGray,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        InfoTextField(
            value = clinicianName,
            onValueChange = onClinicianNameChange,
            label = "Enter clinician name here...",
            modifier = Modifier.padding(bottom = 16.dp)
        )
        InfoTextField(
            value = clinicianPin,
            onValueChange = onClinicianPinChange,
            label = "Enter your 4 digit PIN here...",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(
            onClick = onConfirm,
            colors = ButtonDefaults.buttonColors(containerColor = ButtonActive),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "Confirm",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (errorMessage.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF5F5)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = Color(0xFFE53E3E)
                )
            ) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFE53E3E) ,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        if (successMessage.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF0FFF4)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = Color(0xFF38A169)
                )
            ) {
                Text(
                    text = successMessage,
                    color = Color(0xFF38A169),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun InfoTextField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.DarkGray,
            focusedContainerColor = Color(0xFFF5F5F5),
            unfocusedContainerColor = Color(0xFFF9F9F9),
            focusedBorderColor = ButtonActive,
            unfocusedBorderColor = Color.LightGray
        ),
        label = {
            Text(
                text = label,
                color = Color.Gray,
            )
        },
        keyboardOptions = keyboardOptions,
        singleLine = true
    )
}
