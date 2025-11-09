package com.example.gaitguardian.screens.camera

import DistanceViewModel
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.view.OrientationEventListener
import android.view.OrientationEventListener.ORIENTATION_UNKNOWN
import android.view.Surface
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.app.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.gaitguardian.data.roomDatabase.tug.TUGAssessment
import com.example.gaitguardian.viewmodels.TugDataViewModel
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.collections.isNotEmpty


enum class devOrientation {
    PORTRAIT, LANDSCAPE_LEFT, LANDSCAPE_RIGHT, PORTRAIT_UPSIDE_DOWN
}
data class devOrientationState (
    val orientation: devOrientation,
    val lockedLandscape: Boolean
)
@Composable
fun rembDeviceOrientation(): devOrientationState {
    val context = LocalContext.current
    var orientation by remember { mutableStateOf(devOrientation.PORTRAIT) }

    var isListenerEnabled by remember { mutableStateOf(true) }
    var listenerStatus by remember { mutableStateOf("Listener Active") }

    DisposableEffect(context, isListenerEnabled) {
        if (!isListenerEnabled) return@DisposableEffect onDispose { }

        val orientationEventListener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientationDegrees: Int) {
                if (orientationDegrees == ORIENTATION_UNKNOWN) return

                val newOrientation = when (orientationDegrees) {
                    in 315..360, in 0..45 -> devOrientation.PORTRAIT
                    in 45..135 -> devOrientation.LANDSCAPE_LEFT
                    in 135..225 -> devOrientation.PORTRAIT_UPSIDE_DOWN
                    in 225..315 -> devOrientation.LANDSCAPE_RIGHT
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

    var lockedLandscape by remember { mutableStateOf(false) }

    LaunchedEffect(orientation) {
        if (!lockedLandscape &&
            (orientation == devOrientation.LANDSCAPE_LEFT || orientation == devOrientation.LANDSCAPE_RIGHT)
        ) {
            delay(5000)
            // Check if still in landscape
            if (orientation == devOrientation.LANDSCAPE_LEFT || orientation == devOrientation.LANDSCAPE_RIGHT) {
                lockedLandscape = true // lock UI
                Log.d("oriListener", "Locked to landscape")
            }
        }
    }
    return devOrientationState(orientation, lockedLandscape)
}
@androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
@SuppressLint("RestrictedApi")
@Composable
fun CameraScreen(viewModel: DistanceViewModel = viewModel(), navController: NavController, tugViewModel: TugDataViewModel) {
    val cameraPhase by viewModel.cameraPhase.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    var recording by remember { mutableStateOf<Recording?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }

    val deviceOrientation = rembDeviceOrientation()
    val isDeviceLandscape =
        deviceOrientation.lockedLandscape ||
                deviceOrientation.orientation == devOrientation.LANDSCAPE_LEFT ||
                deviceOrientation.orientation == devOrientation.LANDSCAPE_RIGHT

    Box(modifier = Modifier.fillMaxSize()) {
        if (isDeviceLandscape) {
            AndroidView({ previewView }, modifier = Modifier.fillMaxSize())

            when (cameraPhase) {
                DistanceViewModel.CameraPhase.CheckingDistance -> {
                    DistanceTestOverlay(viewModel)
                }

                DistanceViewModel.CameraPhase.CheckingLuminosity -> {
                    LuminosityCheckOverlay(viewModel)
                }

                DistanceViewModel.CameraPhase.VideoRecording -> {
                    VideoRecordingOverlay(
                        navController = navController,
                        tugDataViewModel = tugViewModel,
                        recording = recording,
                        isRecording = isRecording,
                        onRecordingStateChange = { rec, isRec ->
                            recording = rec
                            isRecording = isRec
                        },
                        context = context,
                        lifecycleOwner = lifecycleOwner,
                        videoCapture = videoCapture,
                        onRecordingFinished = { uri ->
                            Log.d("App", "Video saved: $uri")
                            viewModel.resetToDistanceCheck()
                        },
                        deviceOrientation = deviceOrientation
                    )
                }
            }
        }
        else { // In Portrait
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
            }
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                context as ComponentActivity,
                arrayOf(Manifest.permission.CAMERA),
                100
            )
            return@LaunchedEffect
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            viewModel.setCameraProvider(cameraProvider)

            val preview = Preview.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            val analyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analyzer.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                viewModel.processImage(imageProxy)
            }

            viewModel.setImageAnalysis(analyzer)

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            preview.setSurfaceProvider(previewView.surfaceProvider)

            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                analyzer,
                videoCapture!!
            )

            val cameraInfo = Camera2CameraInfo.from(camera.cameraInfo)
            val focalLengths = cameraInfo.getCameraCharacteristic(
                android.hardware.camera2.CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
            )
            val sensorSize = cameraInfo.getCameraCharacteristic(
                android.hardware.camera2.CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE
            )

            previewView.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    previewView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    val widthPx = previewView.width.takeIf { it > 0 } ?: 1920
                    if (focalLengths != null && focalLengths.isNotEmpty() && sensorSize != null) {
                        viewModel.setCameraParams(
                            focalLengthMeters = focalLengths[0] / 1000f,
                            sensorWidthMeters = sensorSize.width / 1000f,
                            imageWidthPx = widthPx
                        )
                    }
                }
            })
        }, ContextCompat.getMainExecutor(context))
    }
}

@Composable
fun LuminosityCheckOverlay(viewModel: DistanceViewModel) {
    val luminance by viewModel.luminance.collectAsState()
    val error by viewModel.luminosityError.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Checking Lighting Conditions",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Luminance: %.1f".format(luminance),
            color = Color.White,
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (error != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    error!!,
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Text(
                "✅ Lighting conditions are good",
                color = Color.Green,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

    }
}

@Composable
fun DistanceTestOverlay(viewModel: DistanceViewModel) {
    val personDistance by viewModel.personDistance.collectAsState()
    val lateralWidth by viewModel.lateralWidth.collectAsState()
    var heightInput by remember { mutableStateOf("1.7") }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            TextField(
                value = heightInput,
                onValueChange = { heightInput = it },
                label = { Text("Person height (m)") },
                modifier = Modifier.width(150.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                viewModel.setPersonHeight(heightInput.toFloatOrNull() ?: 1.7f)
            }) {
                Text("Apply")
            }
        }

        // Bottom info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(16.dp)
        ) {
            Text(
                "Distance to Camera: %.2f m".format(personDistance),
                color = Color.White,
                fontSize = 18.sp
            )
            Text(
                "Visible Width: %.2f m".format(lateralWidth),
                color = Color.White,
                fontSize = 18.sp
            )
            if (lateralWidth >= 3.0f) {
                Text(
                    "3m lateral space available - Recording will start soon...",
                    color = Color.Green,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    "Not enough lateral space",
                    color = Color.Red,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun VideoRecordingOverlay(
    navController: NavController,
    tugDataViewModel: TugDataViewModel,
    recording: Recording?,
    isRecording: Boolean,
    onRecordingStateChange: (Recording?, Boolean) -> Unit,
    context: Context,
    lifecycleOwner: LifecycleOwner,
    videoCapture: VideoCapture<Recorder>?,
    onRecordingFinished: (Uri?) -> Unit,
    deviceOrientation: devOrientationState
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // Recording indicator at top
        if (isRecording) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .background(Color.Red.copy(alpha = 0.7f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color.White, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Recording", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // Record button at bottom
        Button(
            onClick = {
                if (isRecording) {
                    recording?.stop()
                    onRecordingStateChange(null, false)
                } else {
                    if (videoCapture != null) {
                        startRecording(
                            context,
                            lifecycleOwner,
                            videoCapture,
                            tugDataViewModel,
                            navController,
                            onRecordingStateChange,
                            onRecordingFinished,
                            deviceOrientation
                        )
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) Color.Red else Color.Green
            ),
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
                .size(80.dp)
        ) {
            Text(
                if (isRecording) "Stop" else "Record",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


private fun startRecording(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    videoCapture: VideoCapture<Recorder>,
    tugViewModel: TugDataViewModel,
    navController: NavController,
    onRecordingStateChange: (Recording?, Boolean) -> Unit,
    onRecordingFinished: (Uri?) -> Unit,
    deviceOrientation: devOrientationState
) {
    // Set the target rotation based on device orientation
    val targetRotation = when (deviceOrientation.orientation) {
        devOrientation.PORTRAIT -> Surface.ROTATION_0
        devOrientation.LANDSCAPE_LEFT -> Surface.ROTATION_270
        devOrientation.LANDSCAPE_RIGHT -> Surface.ROTATION_90
        devOrientation.PORTRAIT_UPSIDE_DOWN -> Surface.ROTATION_180
    }
    videoCapture.targetRotation = targetRotation

    val outputFile = File(
        context.getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES),
        "new-video-${System.currentTimeMillis()}.mp4")
    val outputOptions = FileOutputOptions.Builder(outputFile).build()

    val pendingRecording = videoCapture.output.prepareRecording(context, outputOptions)
    var activeRecording: Recording? = null
    var recordingStartTimeNanos: Long = 0
    var assessmentTitle = "Timed Up and Go"
    activeRecording = pendingRecording.start(ContextCompat.getMainExecutor(context)) { event ->
        when (event) {
            is VideoRecordEvent.Start -> {
                Log.d("VideoRecording", "Recording started")
                recordingStartTimeNanos = System.nanoTime()
                onRecordingStateChange(activeRecording, true)
            }
            is VideoRecordEvent.Finalize -> {
                if (event.hasError()) {
                    Log.e("CameraRecording", "Video capture failed: ${event.error}", event.cause)
                    activeRecording?.close()
                    onRecordingStateChange(null, false)
                    if (outputFile.exists()) outputFile.delete()
                    Toast.makeText(context, "Video capture failed", Toast.LENGTH_LONG).show()
                } else {
                    val currentDateTime = SimpleDateFormat(
                        "dd MMM yyyy, hh:mm a", Locale.getDefault()
                    ).format(Date())
                    val durationSeconds = ((System.nanoTime() - recordingStartTimeNanos) / 1_000_000_000).toInt()
                    onRecordingStateChange(null, false)
                    Toast.makeText(context, "Video capture succeeded", Toast.LENGTH_LONG).show()

                    onRecordingFinished(Uri.fromFile(outputFile))
                    val encodedPath = Uri.encode(outputFile.absolutePath)

                    val newTug = TUGAssessment(
                        dateTime = currentDateTime,
                        videoDuration = durationSeconds.toFloat(),
                        videoTitle = outputFile.name,
                        onMedication = tugViewModel.onMedication.value,
                        patientComments = tugViewModel.selectedComments.value.joinToString(", ")
                    )
                    tugViewModel.insertNewAssessment(newTug)
                    navController.navigate("loading_screen/${assessmentTitle}/${encodedPath}")
                }
            }
        }
    }
}
