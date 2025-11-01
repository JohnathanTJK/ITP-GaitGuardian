package com.example.gaitguardian.screens.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.util.Log
import android.view.OrientationEventListener
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.video.AudioConfig
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.gaitguardian.viewmodels.TugDataViewModel
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

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

    // Disable listener after 5s in landscape
    LaunchedEffect(orientation) {
        if (orientation == DeviceOrientation.LANDSCAPE_LEFT || orientation == DeviceOrientation.LANDSCAPE_RIGHT) {
            listenerStatus = "Landscape detected - Timer started (5s)"
            Log.d("oriListener", "Landscapedetected")

            delay(5000)

            if (orientation == DeviceOrientation.LANDSCAPE_LEFT || orientation == DeviceOrientation.LANDSCAPE_RIGHT) {
                isListenerEnabled = false
                Log.d("oriListener", "Listener killed - Stayed in landscape for 5s")
            }
        } else {
            if (isListenerEnabled) {
                listenerStatus = "Listener Active"
            }
        }
    }

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
    tugViewModel: TugDataViewModel,
    assessmentTitle: String,
    modifier: Modifier = Modifier
) {
    val scaffoldState = rememberBottomSheetScaffoldState()
    val context = LocalContext.current
    val deviceOrientation = rememberDeviceOrientation()

    var isRecording by remember { mutableStateOf(false) }
    val recordingTime = remember { mutableIntStateOf(0) }

    var currentLuminance by remember { mutableStateOf(0.0) }
    var isTooDark by remember { mutableStateOf(false) }
    var isTooBright by remember { mutableStateOf(false) }
    var captureErrorMessage by remember { mutableStateOf<String?>(null) }

    var countdownValue by remember { mutableStateOf<Int?>(null) }

    val isDeviceLandscape = deviceOrientation == DeviceOrientation.LANDSCAPE_LEFT ||
            deviceOrientation == DeviceOrientation.LANDSCAPE_RIGHT

    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(
                CameraController.IMAGE_CAPTURE or
                        CameraController.VIDEO_CAPTURE or
                        CameraController.IMAGE_ANALYSIS
            )
            videoCaptureQualitySelector = QualitySelector.from(Quality.HD)
        }
    }

    var hasPermissions by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions[Manifest.permission.CAMERA] == true &&
                permissions[Manifest.permission.RECORD_AUDIO] == true
    }

    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        }
    }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 0.dp,
        sheetContent = { },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isDeviceLandscape) {
                CameraPreview2(
                    controller = controller,
                    modifier = Modifier.fillMaxSize(),
                    onAnalysisResult = { luminance, isDark, isBright, errorMessage ->
                        currentLuminance = luminance
                        isTooDark = isDark
                        isTooBright = isBright
                        captureErrorMessage = errorMessage
                    },
                )

                // Countdown
                countdownValue?.let { value ->
                    val scale by animateFloatAsState(
                        targetValue = if (value != null) 3f else 0f,
                        label = ""
                    )

                    AnimatedVisibility(
                        visible = value != null,
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Text(
                            text = "$value",
                            color = Color.White,
                            fontSize = 120.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.graphicsLayer(
                                rotationZ = 90f,
                                //scaleX = scale,
                                //scaleY = scale,
                                transformOrigin = TransformOrigin.Center
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(screenHeightDp.dp, screenWidthDp.dp)
                        .align(Alignment.Center)
                        .graphicsLayer {
                            rotationZ = 90f
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                        }
                ) {
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
                                    tugViewModel = tugViewModel,
                                    recordingTimeState = recordingTime,
                                    onRecordingStateChange = { recording ->
                                        isRecording = recording
                                    },
                                    assessmentTitle = assessmentTitle,
                                    countdownValueState = { countdownValue = it }
                                )
                            },
                            modifier = Modifier.size(84.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(if (isRecording) Color.Red else Color.DarkGray),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Videocam,
                                    contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                                    tint = Color.White,
                                    modifier = Modifier.size(46.dp)
                                )
                            }
                        }
                    }

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

private var recording: Recording? = null

@SuppressLint("MissingPermission")
private fun recordVideo(
    context: Context,
    controller: LifecycleCameraController,
    navController: NavController,
    recordingTimeState: MutableState<Int>,
    patientViewModel: PatientViewModel,
    tugViewModel: TugDataViewModel,
    onRecordingStateChange: (Boolean) -> Unit,
    assessmentTitle: String,
    countdownValueState: (Int?) -> Unit
) {
    if (recording != null) {
        recording?.stop()
        recording = null
        onRecordingStateChange(false)
        return
    }

    val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
    val mainExecutor = ContextCompat.getMainExecutor(context)

    GlobalScope.launch(Dispatchers.Main) {
        for (i in 3 downTo 1) {
            countdownValueState(i)
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
            delay(1000)
        }

        countdownValueState(null)
        toneGenerator.release()

        // Start recording
        val outputFile = File(
            context.getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES),
            "my-recording-${System.currentTimeMillis()}.mp4"
        )

        var recordingStartTimeNanos = 0L

        recording = controller.startRecording(
            FileOutputOptions.Builder(outputFile).build(),
            AudioConfig.AUDIO_DISABLED,
            mainExecutor,
        ) { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    onRecordingStateChange(true)
                    recordingStartTimeNanos = System.nanoTime()
                }

                is VideoRecordEvent.Finalize -> {
                    if (event.hasError()) {
                        recording?.close()
                        recording = null
                        onRecordingStateChange(false)
                        Toast.makeText(context, "Video capture failed", Toast.LENGTH_LONG).show()
                    } else {
                        val currentDateTime: String =
                            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                                .format(Date())
                        val durationSeconds =
                            ((System.nanoTime() - recordingStartTimeNanos) / 1_000_000_000).toInt()
                        recordingTimeState.value = durationSeconds
                        onRecordingStateChange(false)
                        Toast.makeText(context, "Video capture succeeded", Toast.LENGTH_LONG).show()

                        val encodedPath = Uri.encode(outputFile.absolutePath)

                        if (patientViewModel.saveVideos.value) {
                            navController.navigate("loading_screen/${assessmentTitle}/${encodedPath}")

                            val newTug = TUGAssessment(
                                dateTime = currentDateTime,
                                videoDuration = recordingTimeState.value.toFloat(),
                                videoTitle = outputFile.name,
                                onMedication = tugViewModel.onMedication.value,
                                patientComments = tugViewModel.selectedComments.value.joinToString(", ")
                            )
                            tugViewModel.insertNewAssessment(newTug)
                        } else {
                            navController.navigate("loading_screen/${assessmentTitle}/${encodedPath}")
                            val newTugNoVideo = TUGAssessment(
                                dateTime = currentDateTime,
                                videoDuration = recordingTimeState.value.toFloat(),
                                onMedication = tugViewModel.onMedication.value,
                                patientComments = tugViewModel.assessmentComment.value
                            )
                            tugViewModel.insertNewAssessment(newTugNoVideo)
                        }
                    }
                }
            }
        }
    }
}
