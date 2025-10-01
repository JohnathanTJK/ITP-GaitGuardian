package com.example.gaitguardian.screens

import android.os.Environment
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.gaitguardian.ui.theme.bgColor
import com.example.gaitguardian.viewmodels.ClinicianViewModel
import com.example.gaitguardian.viewmodels.PatientViewModel

@Composable
fun SettingsScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    isClinician: Boolean = false,
    patientViewModel: PatientViewModel,
    clinicianViewModel: ClinicianViewModel
) {
    
    fun switchViewAndNavigate(isClinician: Boolean) {
        if (isClinician) {
            patientViewModel.saveCurrentUserView("patient")
            navController.navigate("patient_graph") {
                popUpTo("clinician_graph") { inclusive = true }
                launchSingleTop = true
            }
        } else {
            clinicianViewModel.saveCurrentUserView("clinician")
//            navController.navigate("clinician_graph"){
            navController.navigate("clinician_pin_verification_screen/-1") {
//                popUpTo("patient_graph") { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val saveVideos by patientViewModel.saveVideos.collectAsState()
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    val availableLanguages = listOf(
        "en" to "English",
        "bm" to "Malay",
        "in" to "Indian",
        "zh" to "Chinese"
    )
    val currentLanguage = "en" // TODO: get from DataStore

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxSize()
            .background(bgColor)
            .padding(8.dp)
    ) {
        // General Card
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .shadow(8.dp, shape = MaterialTheme.shapes.medium),
            color = Color(0xFFFFC279),
            shape = MaterialTheme.shapes.medium,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "General",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 5.dp),
                    color = Color.Black
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { switchViewAndNavigate(isClinician) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SwitchAccount,
                        contentDescription = "Switch Account",
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = if (isClinician) "Switch to Patient View" else "Switch to Clinician View",
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLanguageDialog = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Language",
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = "Change Language", fontSize = 18.sp, color = Color.Black)
                }

                if (showLanguageDialog) {
                    LanguagePickerDialog(
                        availableLanguages = availableLanguages,
                        currentLanguageCode = currentLanguage,
                        onDismissRequest = { showLanguageDialog = false },
                        onLanguageSelected = { selectedLangCode ->
                            // TODO: Save language change
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Video Privacy Card - only for patients
        if (!isClinician) {
            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .shadow(8.dp, shape = MaterialTheme.shapes.medium),
                color = Color(0xFFFFC279),
                shape = MaterialTheme.shapes.medium,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Video Privacy",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 5.dp),
                        color = Color.Black
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPrivacyDialog = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PrivacyTip, contentDescription = "Privacy", tint = Color.Black)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Manage Video Privacy", fontSize = 18.sp, color = Color.Black)
                    }
                    if (showPrivacyDialog) {
                        VideoPrivacyDialog(
                            saveVideos = saveVideos,
                            onConfirm = { newValue ->
                                patientViewModel.setSaveVideos(newValue)
                                showPrivacyDialog = false
                            },
                            onDismiss = { showPrivacyDialog = false }
                        )
                    }

                    if (saveVideos) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navController.navigate("view_videos_screen") }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.VideoLibrary, contentDescription = "View Videos", tint = Color.Black)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("View Saved Videos", fontSize = 18.sp, color = Color.Black)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}


@Composable
fun LanguagePickerDialog(
    availableLanguages: List<Pair<String, String>>, // Pair<languageCode, languageDisplayName>
    currentLanguageCode: String,
    onDismissRequest: () -> Unit,
    onLanguageSelected: (String) -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Select Language", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Black)
                val currentLanguageName = availableLanguages.firstOrNull { it.first == currentLanguageCode }?.second ?: currentLanguageCode
                Text("Current Language: $currentLanguageName", fontSize = 16.sp, color = Color.Black)
                Spacer(Modifier.height(16.dp))
                availableLanguages.forEach { (code, displayName) ->
                    Text(
                        text = displayName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onLanguageSelected(code)
                                onDismissRequest()
                            }
                            .padding(12.dp),
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel", color = Color.Black)
                }
            }

        }
    }

}

@Composable
fun VideoPrivacyDialog(
    saveVideos: Boolean,
    onConfirm: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showClearVideosDialog by remember { mutableStateOf(false) }

    // Function to check if videos exist
    fun hasExistingVideos(): Boolean {
        val videoFolder = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        val videoFiles = videoFolder?.listFiles()?.filter { it.extension == "mp4" }
        return !videoFiles.isNullOrEmpty()
    }

    // Function to clear videos
    fun clearExistingVideos() {
        val videoFolder = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        val videoFiles = videoFolder?.listFiles()?.filter { it.extension == "mp4" }
        videoFiles?.forEach { it.delete() }
    }

    // Main privacy dialog
    if (!showClearVideosDialog) {
        AlertDialog(
            containerColor = Color.White,
            titleContentColor = Color.Black,
            textContentColor = Color.Gray,
            tonalElevation = 8.dp,
            shape = RoundedCornerShape(16.dp),
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    if (saveVideos && hasExistingVideos()) {
                        // User is turning OFF saving and has existing videos
                        showClearVideosDialog = true
                    } else {
                        // No existing videos or user is turning ON saving
                        onConfirm(!saveVideos)
                    }
                },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF44336),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)) {
                    Text(if (saveVideos) "Turn OFF Saving" else "Allow Saving")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)) {
                    Text("Cancel")
                }
            },
            title = { Text("Video Privacy", fontWeight = FontWeight.Bold, fontSize =20.sp, color = Color.Black) },
            text = {
                Text(
                    if (saveVideos)
                        "Your videos are currently being saved. Do you want to stop saving them?"
                    else
                        "Your videos are not saved. Do you want to allow saving recorded videos?"
                ,
                    fontSize = 16.sp,
                    color = Color.Black)
            }
        )
    }

    // Clear existing videos dialog
    if (showClearVideosDialog) {
        AlertDialog(
            containerColor = Color.White,
            titleContentColor = Color.Black,
            textContentColor = Color.Gray,
            tonalElevation = 8.dp,
            shape = RoundedCornerShape(16.dp),
            onDismissRequest = {
                showClearVideosDialog = false
                onDismiss()
            },
            confirmButton = {
                TextButton(onClick = {
                    clearExistingVideos()
                    onConfirm(false) // Turn off saving
                    showClearVideosDialog = false
                },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF44336),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                    ) {
                    Text("Yes, Clear Videos", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onConfirm(false) // Turn off saving but keep videos
                    showClearVideosDialog = false
                },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("No, Keep Videos", color = Color.White)
                }
            },
            title = { Text("Clear Existing Videos?", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black) },
            text = {
                Text("You have existing saved videos. Do you want to clear them when turning off video saving?", fontSize = 16.sp, color = Color.Black)
            }
        )
    }
}



