package com.example.gaitguardian.screens.clinician

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gaitguardian.ui.theme.bgColor
import com.example.gaitguardian.viewmodels.ClinicianViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun PinEntryScreen(
    pinLength: Int = 4,
    onPinComplete: (String) -> Unit,
    onPinChange: (String) -> Unit = {},
    errorMessage: String = "",
    modifier: Modifier = Modifier
) {
    var pin by remember { mutableStateOf("") }
    val focusRequesters = remember {
        List(pinLength) { FocusRequester() }
    }

    LaunchedEffect(pin) {
        onPinChange(pin)
        if (pin.length == pinLength) {
            onPinComplete(pin)
        } else if (pin.length < pinLength) {
            // focus on the next box
            focusRequesters[pin.length].requestFocus()
        }
    }

    // Initial focus on first box
    LaunchedEffect(Unit) {
        focusRequesters[0].requestFocus()
    }

    Column(
        modifier = modifier
            .background(bgColor)
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Enter PIN",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pinLength) { index ->
                PinDigitBox(
                    digit = pin.getOrNull(index)?.toString() ?: "",
                    isFocused = pin.length == index, // Current box is the one being filled
                    isActive = pin.length >= index,   // Box is active if it's current or filled
                    focusRequester = focusRequesters[index],
                    onValueChange = { newValue ->
                        if (newValue.length <= 1 && newValue.all { it.isDigit() }) {
                            if (newValue.isEmpty()) {
                                // Handle backspace - remove last character
                                if (pin.isNotEmpty()) {
                                    pin = pin.dropLast(1)
                                    if (index > 0) {
                                        focusRequesters[index - 1].requestFocus()
                                    }
                                }
                            } else {
                                // Add new digit only if it's the next position
                                if (index == pin.length) {
                                    pin += newValue
                                }
                            }
                        }
                    }
                )
            }
        }

        // Show error message if any
        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = Color.Red,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp),
                textAlign = TextAlign.Center
            )
        }

        if (pin.isNotEmpty()) {
            TextButton(
                onClick = {
                    pin = "" // clear the entire pin
                },
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text("Clear")
            }
        }
    }
}

@Composable
fun PinDigitBox(
    digit: String,
    isFocused: Boolean,
    isActive: Boolean,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        isFocused -> Color(0xFFF7C89C)
        isActive -> Color(0xFFFEEBC8)
        else -> Color(0xFFF7C89C).copy(alpha = 0.5f)
    }

    val backgroundColor = when {
        digit.isNotEmpty() -> Color(0xFFFEEBC8)
        isActive -> Color(0xFFFEEBC8).copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    }

    Box(
        modifier = modifier
            .size(64.dp)
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isActive) {
            BasicTextField(
                value = digit,
                onValueChange = onValueChange,
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .fillMaxSize()
                    .onKeyEvent { event ->
                        if (event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DEL) {
                            onValueChange("")
                            true
                        } else {
                            false
                        }
                    },
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    textAlign = TextAlign.Center,
                    color = Color.Black
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (digit.isEmpty() && isFocused) {
                            Text(
                                text = "",
                                color = Color.Black
                            )
                        }
                        innerTextField()
                    }
                }
            )
        } else {
            // Inactive boxes just show the digit or placeholder
            Text(
                text = if (digit.isNotEmpty()) digit else "-",
                color = if (digit.isNotEmpty()) {
                    Color.Black
                } else {
                    Color.Black.copy(alpha = 0.3f)
                }
            )
        }
    }
}

@Composable
fun PinEntryExample(navController: NavController,
                    clinicianViewModel: ClinicianViewModel,
                    notificationId: Int? = null)
{ // for testing now
    var showSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val clinicianInfo by clinicianViewModel.clinician.collectAsState()
    val clinicianPin = clinicianInfo?.pin
    if (showSuccess) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Pin verified. Redirecting...",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.Black
            )
        }
    } else {
        Column {
            PinEntryScreen(
                pinLength = 4,
                errorMessage = errorMessage,
                onPinComplete = { pin ->
                    // Validate PIN here, for now just 1234
//                    if (pin == "1234") { // validation
                    if (pin == clinicianPin) {
                        clinicianViewModel.saveCurrentUserView("clinician")
                        showSuccess = true
                        coroutineScope.launch {
                            delay(1500)

                            if (notificationId != null) {
                                // Navigate to detail screen if notificationId (tapped notification)
                                navController.navigate("clinician_detailed_patient_view_screen/$notificationId") {
//                                navController.navigate("clinician_graph/clinician_detailed_patient_view_screen/$notificationId") {
                                    popUpTo("clinician_pin_verification_screen") { inclusive = true }
                                    launchSingleTop = true
                                }
                            } else {
                                // Navigate to home
                                navController.navigate("clinician_graph") {
                                    popUpTo("clinician_pin_verification_screen") { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                    }
                    else {
                        errorMessage = "Incorrect PIN. Try again."
                    }
                },
                onPinChange = { currentPin ->
                    // Clear error when user starts typing again
                    if (errorMessage.isNotEmpty()) {
                        errorMessage = ""
                    }
                }
            )
        }
    }
}
