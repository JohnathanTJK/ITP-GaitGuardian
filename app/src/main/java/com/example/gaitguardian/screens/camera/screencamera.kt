package com.example.gaitguardian.screens.camera

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.view.OrientationEventListener
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.video.AudioConfig
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.gaitguardian.viewmodels.PatientViewModel
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import java.io.File

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
fun NewCameraScreen(navController: NavController, patientViewModel: PatientViewModel, modifier: Modifier = Modifier) {
    val scaffoldState = rememberBottomSheetScaffoldState()
    val context = LocalContext.current
    val deviceOrientation = rememberDeviceOrientation()

    // Add recording state
    var isRecording by remember { mutableStateOf(false) }
    val recordingTime = remember { mutableIntStateOf(0) }

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
    //

    // Show camera only when device is in landscape (either direction)
    val isDeviceLandscape = deviceOrientation == DeviceOrientation.LANDSCAPE_LEFT ||
            deviceOrientation == DeviceOrientation.LANDSCAPE_RIGHT

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
                    onDistanceDetectionResult = { distance, horizontal, lateral, covers3 ->
                        currentDistance = distance
                        currentHorizontalCoverage = horizontal
                        currentLateralCoverage = lateral
                        covered3Meters = covers3
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
                        IconButton(
                            onClick = {
                                recordVideo(
                                    context = context,
                                    controller = controller,
                                    navController = navController,
                                    patientViewModel = patientViewModel,
                                    recordingTimeState = recordingTime,// ✅ pass the whole state
                                    onRecordingStateChange = { recording -> isRecording = recording }
                                )
                            }
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Videocam,
                                contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                                tint = if (isRecording) Color.Red else Color.White
                            )
                        }
                    }

                    // Distance + Analysis Info
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.TopStart)
                    ) {
                        Text("Distance: $currentDistance")
                        Text("Horizontal: $currentHorizontalCoverage")
                        Text("Lateral: $currentLateralCoverage")
                        Text("3m covered: $covered3Meters")
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
                    val durationSeconds = ((System.nanoTime() - recordingStartTimeNanos) / 1_000_000_000).toInt()
                    recordingTimeState.value = durationSeconds
                    Log.d("recordingTime", recordingTimeState.value.toString())
                    onRecordingStateChange(false)
                    Toast.makeText(
                        context,
                        "Video capture succeeded",
                        Toast.LENGTH_LONG
                    ).show()
                    if (patientViewModel.saveVideos.value) {
                        patientViewModel.addRecording(recordingTimeState.value)
                        navController.navigate("loading_screen/${recordingTimeState.value}")
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