package com.example.gaitguardian.screens.patient


import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
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
import com.example.gaitguardian.viewmodels.PatientViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File


@Composable
fun VideoCaptureScreen(
    navController: NavController,
    patientViewModel: PatientViewModel
) {
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
    var recordingTime by remember { mutableIntStateOf(0) }


    // Brightness detection states
    var isTooDark by remember { mutableStateOf(false) }
    var isTooBright by remember { mutableStateOf(false) }
    var captureErrorMessage by remember { mutableStateOf<String?>(null) }
    var currentLuminance by remember { mutableDoubleStateOf(0.0) }




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
                            videoCapture?.let { capture ->
                                if (recording == null) {
                                    val videoFolder =
                                        context.getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES)
                                    val file =
                                        File(videoFolder, "video_${System.currentTimeMillis()}.mp4")
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
                                                    isRecording = false
                                                    recording = null
                                                    if (patientViewModel.saveVideos.value) {
                                                        patientViewModel.addRecording(recordingTime)
                                                        navController.navigate("loading_screen/$recordingTime")
                                                    } else {
                                                        val videoUri = recordEvent.outputResults.outputUri
                                                        val file = File(videoUri.path ?: "")
                                                        if (file.exists()) file.delete()
                                                        navController.navigate("loading_screen/$recordingTime")
                                                    }
                                                }
                                            }
                                        }
                                }
                            }
                        } else {
                            recording?.stop()
                            recording = null
                            isRecording = false
                        }
                    },
                    enabled = isRecording || captureErrorMessage == null,
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
                if (isTooDark) {
                    Text(
                        text = "Too dark — please increase lighting.",
                        color = Color.Red,
                        modifier = Modifier.padding(8.dp)
                    )
                } else if (isTooBright) {
                    Text(
                        text = "Too bright — please reduce lighting.",
                        color = Color.Red,
                        modifier = Modifier.padding(8.dp)
                    )
                }


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


                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()


                            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                                val buffer = imageProxy.planes[0].buffer
                                val data = ByteArray(buffer.remaining())
                                buffer.get(data)
                                val luminance = data.map { it.toInt() and 0xFF }.average()
                                currentLuminance = luminance


                                isTooDark = luminance < 160
                                isTooBright = luminance > 200


                                // Blur detection via luminance variance
                                val variance = data.map { (it.toInt() and 0xFF).toDouble() }
                                    .let { values ->
                                        val mean = values.average()
                                        values.map { (it - mean) * (it - mean) }.average()
                                    }


                                val isBlurry = variance < 180.0  // tweak this threshold as needed after testing


                                captureErrorMessage = when {
                                    luminance < 60 -> "Lighting too dark. Please brighten the environment."
                                    luminance > 200 -> "Lighting too bright — overexposed video will affect detection."
                                    isBlurry -> "Image is blurry — adjust focus or hold device steady."
                                    else -> null
                                }


                                Log.d("Analyzer", "Luminance: $luminance, Variance: $variance")


                                imageProxy.close()
                            }


                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA


                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    video,
                                    imageAnalysis
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


                if (captureErrorMessage != null && !isRecording) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xAA000000))
                            .padding(12.dp)
                            .align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = captureErrorMessage!!,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }


            }
        }
    }
}
