package com.example.gaitguardian.screens.patient

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.camera.view.PreviewView
import java.io.File
import android.Manifest
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoCaptureScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    var hasCameraPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingTime by remember { mutableStateOf(0) }

    // Timer for recording duration
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingTime = 0
            while (isActive && isRecording) {
                delay(1000)
                recordingTime += 1
            }
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Color(0xFFF5F4EE))
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Black
                    )
                }
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(Color(0xFFF5F4EE)),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (!isRecording) {
                            // Start recording
                            videoCapture?.let { capture ->
                                if (recording == null) {
                                    val videoFolder = context.getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES)
                                    val file = File(videoFolder, "video_${System.currentTimeMillis()}.mp4")
                                    val outputOptions = FileOutputOptions.Builder(file).build()

                                    recording = capture.output
                                        .prepareRecording(context, outputOptions)
                                        .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                                            when (recordEvent) {
                                                is VideoRecordEvent.Start -> {
                                                    Log.d("VideoCapture", "Recording started")
                                                    isRecording = true
                                                }

                                                is VideoRecordEvent.Finalize -> {
                                                    Log.d("VideoCapture", "Recording finalized: ${recordEvent.outputResults.outputUri}")
                                                    recording = null
                                                    isRecording = false

                                                    // Navigate to result screen and pass recording time
                                                    navController.navigate("result_screen/$recordingTime")
                                                }

                                                else -> {
                                                    // You can update UI with duration here if you want
                                                }
                                            }
                                        }
                                }
                            }
                        } else {
                            // Stop recording
                            recording?.stop()
                            recording = null
                            isRecording = false
                        }
                    },
                    colors = if (!isRecording)
                        ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC940))
                    else
                        ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373)),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Text(if (!isRecording) "Start" else "Complete", color = Color.Black)
                }
                if (isRecording) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Recording: ${recordingTime}s",
                        modifier = Modifier.align(Alignment.CenterVertically),
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    ) { paddingValues ->
        if (!hasCameraPermission) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Camera permission is required")
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()

                            val recorder = Recorder.Builder()
                                .setQualitySelector(QualitySelector.from(Quality.HD))
                                .build()
                            val video = VideoCapture.withOutput(recorder)
                            videoCapture = video

                            val preview = Preview.Builder().build()
                            preview.setSurfaceProvider(previewView.surfaceProvider)

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    video
                                )
                            } catch (exc: Exception) {
                                Log.e("VideoCaptureScreen", "Use case binding failed", exc)
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
}