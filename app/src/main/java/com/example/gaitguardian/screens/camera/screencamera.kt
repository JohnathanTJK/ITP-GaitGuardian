package com.example.gaitguardian.screens.camera

import android.annotation.SuppressLint
import android.content.Context
import android.os.Environment
import android.util.Log
import android.view.OrientationEventListener
import android.widget.Toast
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.video.AudioConfig
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.gaitguardian.data.roomDatabase.tug.TUGAssessment
import com.example.gaitguardian.viewmodels.PatientViewModel
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DeviceOrientation {
    PORTRAIT, LANDSCAPE_LEFT, LANDSCAPE_RIGHT, PORTRAIT_UPSIDE_DOWN
}

@Composable
fun rememberDeviceOrientation(): DeviceOrientation {
    val context = LocalContext.current
    var orientation by remember { mutableStateOf(DeviceOrientation.PORTRAIT) }

    var isListenerEnabled by remember { mutableStateOf(true) }
    var listenerStatus by remember { mutableStateOf("Listener Active") }

    DisposableEffect(context, isListenerEnabled) {
        if (!isListenerEnabled) return@DisposableEffect onDispose { }

        val orientationEventListener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientationDegrees: Int) {
                if (orientationDegrees == ORIENTATION_UNKNOWN) return

                val newOrientation = when (orientationDegrees) {
                    in 315..360, in 0..45 -> DeviceOrientation.PORTRAIT
                    in 45..135 -> DeviceOrientation.LANDSCAPE_LEFT
                    in 135..225 -> DeviceOrientation.PORTRAIT_UPSIDE_DOWN
                    in 225..315 -> DeviceOrientation.LANDSCAPE_RIGHT
                    else -> orientation
                }

                if (newOrientation != orientation) {
                    orientation = newOrientation
                }
            }
        }

        if (orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable()
        }

        onDispose {
            orientationEventListener.disable()
        }
    }

// Timer to disable listener if landscape persists for 5 seconds
    LaunchedEffect(orientation) {
        if (orientation == DeviceOrientation.LANDSCAPE_LEFT || orientation == DeviceOrientation.LANDSCAPE_RIGHT) {
            listenerStatus = "Landscape detected - Timer started (5s)"
            Log.d("oriListener", "Landscapedetected")

            delay(5000) // Wait 5 seconds

            // Check if still in landscape after 5 seconds
            if (orientation == DeviceOrientation.LANDSCAPE_LEFT || orientation == DeviceOrientation.LANDSCAPE_RIGHT) {
                isListenerEnabled = false // This will trigger DisposableEffect to clean up
                Log.d("oriListener", "Listener killed - Stayed in landscape for 5s")
            }
        } else {
            // Reset status when not in landscape
            if (isListenerEnabled) {
                listenerStatus = "Listener Active"
            }
        }
    }

// Display the listener status on screen
    Text(
        text = listenerStatus,
        modifier = Modifier.padding(16.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = if (isListenerEnabled) Color.Green else Color.Red
    )
    return orientation
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewCameraScreen(
    navController: NavController,
    patientViewModel: PatientViewModel,
    modifier: Modifier = Modifier
) {
    val scaffoldState = rememberBottomSheetScaffoldState()
    val context = LocalContext.current
    val deviceOrientation = rememberDeviceOrientation()

    // Add recording state
    var isRecording by remember { mutableStateOf(false) }
    val recordingTime = remember { mutableIntStateOf(0) }
    var showRecordButton by remember { mutableStateOf(false) }

    // Add image analysis states
    var currentLuminance by remember { mutableStateOf(0.0) }
    var isTooDark by remember { mutableStateOf(false) }
    var isTooBright by remember { mutableStateOf(false) }
    var captureErrorMessage by remember { mutableStateOf<String?>(null) }

    // MY PART
    // State variables
    var currentDistance by remember { mutableStateOf<Float?>(null) }
    var currentHorizontalCoverage by remember { mutableStateOf<Float?>(null) }
    var currentLateralCoverage by remember { mutableStateOf<Float?>(null) }
    var covered3Meters by remember { mutableStateOf<Boolean?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var debugInfo by remember { mutableStateOf<String?>(null) }
    var cameraTiltAngle by remember { mutableStateOf<Float?>(null) }

    // Show camera only when device is in landscape (either direction)
    val isDeviceLandscape = deviceOrientation == DeviceOrientation.LANDSCAPE_LEFT ||
            deviceOrientation == DeviceOrientation.LANDSCAPE_RIGHT

    LaunchedEffect(
        currentDistance,
        currentHorizontalCoverage,
        currentLateralCoverage,
        cameraTiltAngle,
        covered3Meters,
        status,
        debugInfo,
        captureErrorMessage
    ) {
        val allReady = currentDistance != null &&
                currentHorizontalCoverage != null &&
                currentLateralCoverage != null &&
                cameraTiltAngle != null &&
                covered3Meters != null &&
                status != null &&
                debugInfo != null &&
                captureErrorMessage == null

        if (allReady) {
            delay(3000) // Wait for 3 seconds
            showRecordButton = true
        } else {
            showRecordButton = false // Reset when not ready
        }
    }

    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(
                CameraController.IMAGE_CAPTURE or
                        CameraController.VIDEO_CAPTURE or
                        CameraController.IMAGE_ANALYSIS
            )
            videoCaptureQualitySelector = QualitySelector.from(
                Quality.HD // May need to confirm which aspect ratio
            )
        }
    }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 0.dp,
        sheetContent = {
            // Bottom sheet content
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isDeviceLandscape) {
                // unrotated camera preview
                CameraPreview2(
                    controller = controller,
                    modifier = Modifier.fillMaxSize(),
                    onAnalysisResult = { luminance, isDark, isBright, errorMessage ->
                        currentLuminance = luminance
                        isTooDark = isDark
                        isTooBright = isBright
                        captureErrorMessage = errorMessage
                    },
                    onDistanceDetectionResult = { distance, horizontal, lateral, covers3, getStatus, getDebugInfo, tiltAngle ->
                        currentDistance = distance
                        currentHorizontalCoverage = horizontal
                        currentLateralCoverage = lateral
                        covered3Meters = covers3
                        status = getStatus
                        debugInfo = getDebugInfo
                        cameraTiltAngle = tiltAngle
                    }
                )

                // Rotated UI
                Box(
                    modifier = Modifier
                        .size(screenHeightDp.dp, screenWidthDp.dp) // Swap dimensions
                        .align(Alignment.Center)
                        .graphicsLayer {
                            rotationZ = 90f
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                        }
                ) {

                    // Record button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
//                        if(showRecordButton)
//                        {
                            IconButton(
                                onClick = {
                                    recordVideo(
                                        context = context,
                                        controller = controller,
                                        navController = navController,
                                        patientViewModel = patientViewModel,
                                        recordingTimeState = recordingTime,
                                        onRecordingStateChange = { recording ->
                                            isRecording = recording
                                        }
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Videocam,
                                    contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                                    tint = if (isRecording) Color.Red else Color.White
                                )
                            }
//                        }
                    }
                    if (currentLateralCoverage != null && currentHorizontalCoverage != null && currentDistance != null && cameraTiltAngle != null
                        && covered3Meters != null && status != null && debugInfo != null
                    ) {

                        // Distance + Analysis Info
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                                .background(Color.Black.copy(alpha = 0.7f))
                                .padding(16.dp)
                        ) {
                            status?.let { status ->
                                Text(text = status, color = Color.White, fontSize = 16.sp)
                            }
                            debugInfo?.let { debug ->
                                Text(
                                    text = debug,
                                    color = Color.Yellow,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            // Only show distance info when data is available
                            currentDistance?.let { distance ->
                                Text(
                                    text = "Distance: ${"%.1f".format(distance)}m",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }

                            // Safe lateral coverage display
                            currentLateralCoverage?.let { lateral ->
                                Text(
                                    text = "Ground Coverage: ${"%.1f".format(lateral)}m",
                                    color = if (lateral >= 6f) Color.Green else Color.Red,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Safe horizontal coverage display
                            currentHorizontalCoverage?.let { horizontal ->
                                Text(
                                    text = "Person Ground Width: ${"%.2f".format(horizontal)}m",
                                    color = Color.Cyan,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            // Camera info - safe tilt angle display
                            cameraTiltAngle?.let { tilt ->
                                Text(
                                    text = "Tilt: ${String.format("%.1f", tilt)}°",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            // 3-meter coverage check - safe display
                            currentLateralCoverage?.let { horizontal ->
                                if (horizontal > 0f) {
                                    Text(
                                        text = if (covered3Meters == true) {
                                            if (horizontal > 0.4f) {
                                                "Too much, move camera closer"
                                            } else {  // horizontal <= 0.4f AND covers 3m
                                                "Covers 3m+ ground distance"
                                            }
                                        } else {
                                            "Less than 3m coverage (${
                                                String.format(
                                                    "%.2f",
                                                    horizontal
                                                )
                                            }m)"
                                        },
                                        color = if (covered3Meters == true) Color.Green else Color.Red,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                    // Capture Error for Blur / Brightness
                    captureErrorMessage?.let { message ->
                        Card(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 32.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.8f))
                        ) {
                            Text(
                                text = message,
                                modifier = Modifier.padding(12.dp),
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Orientation Debug
                    Text(
                        text = "Device: ${deviceOrientation.name}",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                // Portrait
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ScreenRotation,
                        contentDescription = "Rotate Screen",
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Rotate to Landscape",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Turn your device sideways to access the camera.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Current: ${deviceOrientation.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Video recording
private var recording: Recording? = null

@SuppressLint("MissingPermission")
private fun recordVideo(
    context: Context,
    controller: LifecycleCameraController,
    navController: NavController,
    recordingTimeState: MutableState<Int>,
    patientViewModel: PatientViewModel,
    onRecordingStateChange: (Boolean) -> Unit
) {
    if (recording != null) {
        recording?.stop()
        recording = null
        onRecordingStateChange(false)
        return
    }
    val outputFile = File(
        context.getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES),
        "my-recording-${System.currentTimeMillis()}.mp4"
    )

    var recordingStartTimeNanos = 0L // to track the start time

    recording = controller.startRecording(
        FileOutputOptions.Builder(outputFile).build(),
        AudioConfig.AUDIO_DISABLED,
        ContextCompat.getMainExecutor(context),
    ) { event ->
        when (event) {
            is VideoRecordEvent.Start -> {
                // Recording started successfully
                onRecordingStateChange(true)
                // get time in seconds
                recordingStartTimeNanos = System.nanoTime()
            }

            is VideoRecordEvent.Finalize -> {
                if (event.hasError()) {
                    recording?.close()
                    recording = null
                    onRecordingStateChange(false)
                    Toast.makeText(
                        context,
                        "Video capture failed",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
//                    recordingTimeState.value = event.recordingStats.recordedDurationNanos.toInt() / 1_000_000_000
//                    Log.d("recordingTime", recordingTimeState.value.toString())
                    // difference between current and start time = video duration
                    val durationSeconds =
                        ((System.nanoTime() - recordingStartTimeNanos) / 1_000_000_000).toInt()
                    recordingTimeState.value = durationSeconds
                    Log.d("recordingTime", recordingTimeState.value.toString())
                    onRecordingStateChange(false)
                    Toast.makeText(
                        context,
                        "Video capture succeeded",
                        Toast.LENGTH_LONG
                    ).show()
                    if (patientViewModel.saveVideos.value) {
                        val currentDateTime: String = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
                            .format(Date())
                        patientViewModel.addRecording(recordingTimeState.value)
                        navController.navigate("loading_screen/${recordingTimeState.value}")
                        val newTug = TUGAssessment (
                            // TODO: TO UPDATE WITH PATIENT'S MEDICATION STATUS + COMMENTS ETC.
                            dateTime = currentDateTime,
                            videoDuration = recordingTimeState.value.toFloat(),
                            videoTitle = outputFile.name,
                            medication = patientViewModel.medicationStatus.value,
                            onMedication = patientViewModel.onMedication.value,
                            patientComments = patientViewModel.assessmentComment.value,
                        )
                        patientViewModel.insertNewAssessment(newTug)
                        Log.d("tug", "tug inserted into db")
                    } else {
                        val videoUri = event.outputResults.outputUri
                        val file = File(videoUri.path ?: "")
                        if (file.exists()) file.delete()
                        navController.navigate("loading_screen/${recordingTimeState.value}")
                    }
                }
            }
        }
    }
}