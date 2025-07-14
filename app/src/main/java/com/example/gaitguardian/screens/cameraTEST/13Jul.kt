package com.example.gaitguardian.screens.cameraTEST

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.RectF
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.AspectRatio
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan

//@OptIn(ExperimentalGetImage::class)
//@SuppressLint("ServiceCast")
//@Composable
//fun LateralDistanceCameraScreen() {
//    val context = LocalContext.current
//    val lifecycleOwner = LocalLifecycleOwner.current
//
//    val previewView = remember { PreviewView(context) }
//
//    // State to display in the UI
//    var lateralDistance by remember { mutableStateOf<Float?>(null) }
//    val isSufficientSpace = lateralDistance?.let { it >= 3f } ?: false
//
//    Box(modifier = Modifier.fillMaxSize()) {
//        // Camera Preview
//        AndroidView(
//            factory = { previewView },
//            modifier = Modifier.fillMaxSize()
//        )
//
//        // Overlay: Live distance and status
//        Column(
//            modifier = Modifier
//                .align(Alignment.TopCenter)
//                .padding(16.dp)
//                .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
//                .padding(12.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Text(
//                text = "Lateral Distance: ${
//                    lateralDistance?.let { String.format("%.2f", it) + " m" } ?: "Detecting..."
//                }",
//                color = Color.White,
//                fontSize = 18.sp,
//                fontWeight = FontWeight.Bold
//            )
//            Text(
//                text = if (isSufficientSpace) "✅ Space is sufficient" else "❌ Not enough space",
//                color = if (isSufficientSpace) Color.Green else Color.Red,
//                fontSize = 16.sp,
//                fontWeight = FontWeight.Medium
//            )
//        }
//    }
//
//    // Camera + ML Setup
//    LaunchedEffect(Unit) {
//        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
//        val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
//            cameraManager.getCameraCharacteristics(id)
//                .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
//        } ?: return@LaunchedEffect
//
//        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
//        val focalLengthMM = characteristics
//            .get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
//            ?.firstOrNull() ?: 4.2f
//        val sensorWidthMM = characteristics
//            .get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
//            ?.width ?: 5.76f
//
//        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
//        val preview = Preview.Builder().build().apply {
//            setSurfaceProvider(previewView.surfaceProvider)
//        }
//
//        val imageAnalysis = ImageAnalysis.Builder()
////            .setTargetResolution(Size(1280, 720))
//            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//            .build()
//
//        val poseDetector = PoseDetection.getClient(
//            AccuratePoseDetectorOptions.Builder()
//                .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
//                .build()
//        )
//
//        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
//            val mediaImage = imageProxy.image ?: return@setAnalyzer imageProxy.close()
//            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
//            val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)
//
//            val imageWidth = imageProxy.width.toFloat()
//            val realShoulderWidthMeters = 0.45f
//
//            poseDetector.process(inputImage)
//                .addOnSuccessListener { pose ->
//                    val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
//                    val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
//
//                    if (leftShoulder != null && rightShoulder != null) {
//                        val shoulderPixelWidth =
//                            abs(leftShoulder.position.x - rightShoulder.position.x)
//
//                        if (shoulderPixelWidth > 0) {
//                            // Estimate Z (depth)
//                            val depthMeters =
//                                (realShoulderWidthMeters * focalLengthMM * imageWidth) /
//                                        (shoulderPixelWidth * sensorWidthMM)
//
//                            // Estimate lateral distance
//                            val rightEdgeX = imageWidth
//                            val rightShoulderX = rightShoulder.position.x
//                            val lateralPixelWidth = rightEdgeX - rightShoulderX
//
//                            val hfovRadians = 2 * atan(sensorWidthMM / (2 * focalLengthMM))
//                            val sceneWidthAtZ = 2 * depthMeters * tan(hfovRadians / 2)
//                            val metersPerPixel = sceneWidthAtZ / imageWidth
//
//                            val lateralDistanceMeters = lateralPixelWidth * metersPerPixel
//                            lateralDistance = lateralDistanceMeters
//                        }
//                    }
//                }
//                .addOnFailureListener {
//                    Log.e("PoseDetection", "Detection failed: ${it.message}")
//                }
//                .addOnCompleteListener {
//                    imageProxy.close()
//                }
//        }
//
//        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
//        cameraProvider.unbindAll()
//        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
//    }
//}

//// Boundary Box and shoulder points identification
//@OptIn(ExperimentalGetImage::class)
//@Composable
//fun LateralDistanceCameraScreen() {
//    val context = LocalContext.current
//    val lifecycleOwner = LocalLifecycleOwner.current
//
//    val previewView = remember { PreviewView(context) }
//
//    var lateralDistance by remember { mutableStateOf<Float?>(null) }
//    val isSufficientSpace = lateralDistance?.let { it >= 3f } ?: false
//
//    var leftShoulderPos by remember { mutableStateOf<Offset?>(null) }
//    var rightShoulderPos by remember { mutableStateOf<Offset?>(null) }
//    var boundingBox by remember { mutableStateOf<RectF?>(null) }
//
//    Box(modifier = Modifier.fillMaxSize()) {
//        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
//
//        // Distance + status UI
//        Column(
//            modifier = Modifier
//                .align(Alignment.TopCenter)
//                .padding(16.dp)
//                .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
//                .padding(12.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Text(
//                text = "Lateral Distance: ${
//                    lateralDistance?.let { String.format("%.2f", it) + " m" } ?: "Detecting..."
//                }",
//                color = Color.White,
//                fontSize = 18.sp,
//                fontWeight = FontWeight.Bold
//            )
//            Text(
//                text = if (isSufficientSpace) "✅ Space is sufficient" else "❌ Not enough space",
//                color = if (isSufficientSpace) Color.Green else Color.Red,
//                fontSize = 16.sp
//            )
//        }
//
//        // Draw overlay: shoulders and bounding box
//        Canvas(modifier = Modifier.fillMaxSize()) {
//            leftShoulderPos?.let {
//                drawCircle(Color.Red, 12f, center = it)
//            }
//            rightShoulderPos?.let {
//                drawCircle(Color.Red, 12f, center = it)
//            }
//            boundingBox?.let { box ->
//                drawRect(
//                    color = Color.Cyan,
//                    topLeft = Offset(box.left, box.top),
//                    size = Size(box.width(), box.height()),
//                    style = Stroke(width = 4f)
//                )
//            }
//        }
//    }
//
//    // Camera + ML setup
//    LaunchedEffect(Unit) {
//        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
//        val cameraId = cameraManager.cameraIdList.firstOrNull {
//            cameraManager.getCameraCharacteristics(it)
//                .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
//        } ?: return@LaunchedEffect
//
//        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
//        val focalLengthMM = characteristics
//            .get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.firstOrNull() ?: 4.2f
//        val sensorWidthMM = characteristics
//            .get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)?.width ?: 5.76f
//
//        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
//        val preview = Preview.Builder().build().apply {
//            setSurfaceProvider(previewView.surfaceProvider)
//        }
//
//        val imageAnalysis = ImageAnalysis.Builder()
////            .setTargetResolution(Size(1280, 720))
//            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//            .build()
//
//        val poseDetector = PoseDetection.getClient(
//            AccuratePoseDetectorOptions.Builder()
//                .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
//                .build()
//        )
//
//        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
//            val mediaImage = imageProxy.image ?: return@setAnalyzer imageProxy.close()
//            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
//            val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)
//
//            val imageWidth = imageProxy.width.toFloat()
//            val realShoulderWidthMeters = 0.45f
//
//            poseDetector.process(inputImage)
//                .addOnSuccessListener { pose ->
//                    val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
//                    val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
//
//                    if (leftShoulder != null && rightShoulder != null) {
//                        // Update shoulder drawing positions
//                        leftShoulderPos = Offset(leftShoulder.position.x, leftShoulder.position.y)
//                        rightShoulderPos = Offset(rightShoulder.position.x, rightShoulder.position.y)
//
//                        // Bounding box (shoulder-based)
//                        val top = min(leftShoulder.position.y, rightShoulder.position.y) - 80
//                        val bottom = max(leftShoulder.position.y, rightShoulder.position.y) + 80
//                        val left = min(leftShoulder.position.x, rightShoulder.position.x) - 80
//                        val right = max(leftShoulder.position.x, rightShoulder.position.x) + 80
//                        boundingBox = RectF(left, top, right, bottom)
//
//                        // Lateral distance calculation
//                        val shoulderPixelWidth =
//                            abs(leftShoulder.position.x - rightShoulder.position.x)
//
//                        if (shoulderPixelWidth > 0) {
//                            val depthMeters =
//                                (realShoulderWidthMeters * focalLengthMM * imageWidth) /
//                                        (shoulderPixelWidth * sensorWidthMM)
//
//                            val hfovRadians = 2 * atan(sensorWidthMM / (2 * focalLengthMM))
//                            val sceneWidthAtZ = 2 * depthMeters * tan(hfovRadians / 2)
//                            val metersPerPixel = sceneWidthAtZ / imageWidth
//
//                            val lateralPixelWidth = imageWidth - rightShoulder.position.x
//                            val lateralMeters = lateralPixelWidth * metersPerPixel
//
//                            lateralDistance = lateralMeters
//                        }
//                    }
//                }
//                .addOnFailureListener {
//                    Log.e("PoseDetection", "Detection failed: ${it.message}")
//                }
//                .addOnCompleteListener {
//                    imageProxy.close()
//                }
//        }
//
//        cameraProvider.unbindAll()
//        cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
//    }
//}

//// UI rotation and maybe some angle landmark point rotation as well.
//@OptIn(ExperimentalGetImage::class)
//@Composable
//fun LateralDistanceCameraScreen() {
//    val context = LocalContext.current
//    val lifecycleOwner = LocalLifecycleOwner.current  // get lifecycleOwner here in Composable context
//
//    // Remember latest shoulder points for drawing
//    var leftShoulder by remember { mutableStateOf<Offset?>(null) }
//    var rightShoulder by remember { mutableStateOf<Offset?>(null) }
//    var lateralDistance by remember { mutableStateOf<Float?>(null) }
//
//    // CameraX PreviewView
//    val previewView = remember { PreviewView(context) }
//    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
//
//    LaunchedEffect(Unit) {
//        // Setup camera and pose detection
//        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
//        val poseDetector = PoseDetection.getClient(
//            PoseDetectorOptions.Builder()
//            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
//            .build())
//
//        val preview = Preview.Builder().build().also {
//            it.setSurfaceProvider(previewView.surfaceProvider)
//        }
//
//        val analysisUseCase = ImageAnalysis.Builder()
////            .setTargetResolution(Size(1280, 720)) // landscape resolution (typical camera sensor)
//            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//            .build()
//
//        analysisUseCase.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
//            val mediaImage = imageProxy.image
//            if (mediaImage != null) {
//                // Pass in rotationDegrees from imageProxy for ML Kit
//                val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
//
//                poseDetector.process(inputImage)
//                    .addOnSuccessListener { pose ->
//                        // Get shoulders landmarks
//                        val lShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
//                        val rShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
//
//                        if (lShoulder != null && rShoulder != null) {
//                            // Rotate coordinates because:
//                            // - Camera sensor image is landscape (1280x720)
//                            // - Your app is locked portrait, UI is portrait
//                            // We rotate points 90 degrees CCW to map to portrait UI
//
//                            val imageWidth = imageProxy.width.toFloat()
//                            val imageHeight = imageProxy.height.toFloat()
//
//                            fun rotate90CCW(x: Float, y: Float): Offset {
//                                // Rotate 90° counterclockwise:
//                                // NewX = y
//                                // NewY = imageWidth - x
//                                return Offset(y, imageWidth - x)
//                            }
//
//                            val leftRot = rotate90CCW(lShoulder.position.x, lShoulder.position.y)
//                            val rightRot = rotate90CCW(rShoulder.position.x, rShoulder.position.y)
//
//                            // Update Compose states
//                            leftShoulder = leftRot
//                            rightShoulder = rightRot
//
//                            // Calculate lateral distance in meters
//                            // Assuming sensorWidth is camera sensor physical width in meters,
//                            // and imageWidth is image pixels width (here 1280 px)
//                            val sensorWidthMeters = 0.00368f // example 3.68mm sensor width -> 0.00368m
//                            val focalLengthPixels = 1600f // example focal length in pixels (approx.)
//
//                            // Pixel distance between shoulders
//                            val pixelDist = kotlin.math.abs(rightRot.x - leftRot.x)
//
//                            // Real world lateral distance (meters) = (pixelDist * sensorWidthMeters) / imageWidth
//                            // Simplified formula assuming small FOV and parallel plane
//                            val lateralMeters = (pixelDist / imageWidth) * sensorWidthMeters * focalLengthPixels / focalLengthPixels
//                            // The above cancels focalLengthPixels; you might adjust with actual focal length if you want
//
//                            lateralDistance = lateralMeters
//                        }
//                    }
//                    .addOnCompleteListener {
//                        imageProxy.close()
//                    }
//            } else {
//                imageProxy.close()
//            }
//        }
//
//        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
//        cameraProvider.unbindAll()
//        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, analysisUseCase)
//    }
//
//    // Overlay and UI
//    Box(modifier = Modifier.fillMaxSize()) {
//        Canvas(modifier = Modifier.fillMaxSize()) {
//            // Draw shoulders and bounding box if available
//            if (leftShoulder != null && rightShoulder != null) {
//                val l = leftShoulder!!
//                val r = rightShoulder!!
//
//                // Draw circles on shoulders
//                drawCircle(color = Color.Red, radius = 15f, center = l)
//                drawCircle(color = Color.Red, radius = 15f, center = r)
//
//                // Draw bounding box (rectangle covering shoulders horizontally)
//                val top = kotlin.math.min(l.y, r.y) - 40f
//                val bottom = kotlin.math.max(l.y, r.y) + 40f
//                val left = kotlin.math.min(l.x, r.x) - 40f
//                val right = kotlin.math.max(l.x, r.x) + 40f
//                drawRect(
//                    color = Color.Cyan.copy(alpha = 0.3f),
//                    topLeft = Offset(left, top),
//                    size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
//                    style = Stroke(width = 5f)
//                )
//            }
//        }
//
//        // Rotate UI info 90 degrees counterclockwise to appear upright in portrait mode
//        Column(
//            modifier = Modifier
//                .align(Alignment.TopStart)
//                .padding(16.dp)
//                .rotate(-90f) // rotate CCW to counter landscape holding
//                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
//                .padding(12.dp)
//        ) {
//            Text(
//                text = "Lateral Distance:",
//                color = Color.White,
//                fontSize = 18.sp,
//                fontWeight = FontWeight.Bold
//            )
//            Text(
//                text = lateralDistance?.let { String.format("%.2f m", it) } ?: "--",
//                color = if (lateralDistance != null && lateralDistance!! >= 3f) Color.Green else Color.Red,
//                fontSize = 22.sp,
//                fontWeight = FontWeight.Bold
//            )
//            Text(
//                text = if (lateralDistance != null && lateralDistance!! >= 3f) "✅ Space OK" else "⚠️ Too Close",
//                color = if (lateralDistance != null && lateralDistance!! >= 3f) Color.Green else Color.Red,
//                fontSize = 16.sp
//            )
//        }
//    }
//}

//crop landmark to fit preview
//@Composable
//fun CameraWithAccuratePoseOverlay() {
//    val context = LocalContext.current
//    val lifecycleOwner = LocalLifecycleOwner.current
//
//    // States for shoulder points and lateral distance
//    var leftShoulder by remember { mutableStateOf<Offset?>(null) }
//    var rightShoulder by remember { mutableStateOf<Offset?>(null) }
//    var lateralDistanceMeters by remember { mutableStateOf<Float?>(null) }
//
//    // PreviewView for CameraX preview
//    val previewView = remember { PreviewView(context) }
//    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
//
//    LaunchedEffect(previewView) {
//        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
//        val poseDetector = PoseDetection.getClient(
//            PoseDetectorOptions.Builder()
//                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
//                .build()
//        )
//
//        val preview = Preview.Builder().build().also {
//            it.setSurfaceProvider(previewView.surfaceProvider)
//        }
//
//        val analysisUseCase = ImageAnalysis.Builder()
//            .setTargetResolution(android.util.Size(1280, 720)) // Camera sensor size in landscape
//            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//            .build()
//
//        analysisUseCase.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
//            val mediaImage = imageProxy.image
//            if (mediaImage != null) {
//                val inputImage =
//                    InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
//
//                poseDetector.process(inputImage)
//                    .addOnSuccessListener { pose ->
//
//                        val lShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
//                        val rShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
//
//                        if (lShoulder != null && rShoulder != null) {
//                            val imageWidth = imageProxy.width.toFloat()
//                            val imageHeight = imageProxy.height.toFloat()
//
//                            val previewWidth = previewView.width.toFloat()
//                            val previewHeight = previewView.height.toFloat()
//
//                            // Calculate scale and offsets based on PreviewView's default scale type (FILL_CENTER)
//                            val scaleX = previewWidth / imageWidth
//                            val scaleY = previewHeight / imageHeight
//                            val scale = maxOf(scaleX, scaleY)
//
//                            val scaledWidth = imageWidth * scale
//                            val scaledHeight = imageHeight * scale
//
//                            val dx = (scaledWidth - previewWidth) / 2f
//                            val dy = (scaledHeight - previewHeight) / 2f
//
//                            fun mapPoint(x: Float, y: Float): Offset {
//                                return Offset(x * scale - dx, y * scale - dy)
//                            }
//
//                            val mappedLeft = mapPoint(lShoulder.position.x, lShoulder.position.y)
//                            val mappedRight = mapPoint(rShoulder.position.x, rShoulder.position.y)
//
//                            leftShoulder = mappedLeft
//                            rightShoulder = mappedRight
//
//                            // Calculate lateral distance in pixels
//                            val pixelDist = abs(mappedRight.x - mappedLeft.x)
//
//                            // Approximate sensor width in meters (e.g., 3.68 mm = 0.00368 m)
//                            val sensorWidthMeters = 0.00368f
//
//                            // Using simple ratio to convert pixel distance to meters:
//                            // (pixelDist / previewWidth) * sensorWidthMeters * focalLength / focalLength = (pixelDist / previewWidth) * sensorWidthMeters
//                            // Here focal length cancels out because we're approximating flat plane assumption
//                            val lateralMeters = (pixelDist / previewWidth) * sensorWidthMeters
//
//                            lateralDistanceMeters = lateralMeters
//                        }
//                    }
//                    .addOnCompleteListener {
//                        imageProxy.close()
//                    }
//            } else {
//                imageProxy.close()
//            }
//        }
//
//        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
//        cameraProvider.unbindAll()
//        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, analysisUseCase)
//    }
//
//    Box(modifier = Modifier.fillMaxSize()) {
//        Canvas(modifier = Modifier.fillMaxSize()) {
//            if (leftShoulder != null && rightShoulder != null) {
//                val l = leftShoulder!!
//                val r = rightShoulder!!
//
//                // Draw shoulder points
//                drawCircle(color = Color.Red, radius = 15f, center = l)
//                drawCircle(color = Color.Red, radius = 15f, center = r)
//
//                // Draw bounding box around shoulders
//                val left = min(l.x, r.x) - 40f
//                val right = max(l.x, r.x) + 40f
//                val top = min(l.y, r.y) - 40f
//                val bottom = max(l.y, r.y) + 40f
//
//                drawRect(
//                    color = Color.Cyan.copy(alpha = 0.4f),
//                    topLeft = Offset(left, top),
//                    size = Size(right - left, bottom - top),
//                    style = Stroke(width = 5f)
//                )
//            }
//        }
//
//        // Show lateral distance text
//        Box(
//            modifier = Modifier
//                .align(Alignment.TopStart)
//                .padding(16.dp)
//                .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(8.dp))
//                .padding(12.dp)
//        ) {
//            Text(
//                text = "Lateral Distance: ${
//                    lateralDistanceMeters?.let { String.format("%.3f m", it) } ?: "--"
//                }",
//                color = if (lateralDistanceMeters != null && lateralDistanceMeters!! >= 3f) Color.Green else Color.Red,
//                fontSize = 20.sp,
//                fontWeight = FontWeight.Bold
//            )
//        }
//    }
//}

// GPT final
//@OptIn(ExperimentalGetImage::class)
//@Composable
//fun LateralDistanceCameraScreen() {
//    val context = LocalContext.current
//    val lifecycleOwner = LocalLifecycleOwner.current
//
//    // States for shoulder points and lateral distance
//    var leftShoulder by remember { mutableStateOf<Offset?>(null) }
//    var rightShoulder by remember { mutableStateOf<Offset?>(null) }
//    var lateralDistanceMeters by remember { mutableStateOf<Float?>(null) }
//
//    // Remember PreviewView instance
//    val previewView = remember { PreviewView(context) }
//
//    // State for sensor width (meters)
//    var sensorWidthMeters by remember { mutableStateOf<Float?>(null) }
//
//    // Get sensor physical size on launch
//    LaunchedEffect(Unit) {
//        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
//
//        // Get camera ID for back camera
//        val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
//            val characteristics = cameraManager.getCameraCharacteristics(id)
//            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
//            lensFacing == CameraCharacteristics.LENS_FACING_BACK
//        }
//
//        cameraId?.let { id ->
//            val characteristics = cameraManager.getCameraCharacteristics(id)
//            val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
////            sensorWidthMeters = sensorSize?.width ?: 0f
//            sensorWidthMeters = sensorSize?.width ?: 0f
//
//        }
//    }
//
//    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
//
//    LaunchedEffect(previewView, sensorWidthMeters) {
//        if (sensorWidthMeters == null) return@LaunchedEffect
//
//        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
//        val poseDetector = PoseDetection.getClient(
//            PoseDetectorOptions.Builder()
//                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
//                .build()
//        )
//
//        val preview = Preview.Builder().build().also {
//            it.setSurfaceProvider(previewView.surfaceProvider)
//        }
//
//        val analysisUseCase = ImageAnalysis.Builder()
//            .setTargetResolution(android.util.Size(1280, 720))
//            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//            .build()
//
//        analysisUseCase.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
//            val mediaImage = imageProxy.image
//            if (mediaImage != null) {
//                val inputImage =
//                    InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
//
//                poseDetector.process(inputImage)
//                    .addOnSuccessListener { pose ->
//                        val lShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
//                        val rShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
//
//                        if (lShoulder != null && rShoulder != null) {
//                            val imageWidth = imageProxy.width.toFloat()
//                            val imageHeight = imageProxy.height.toFloat()
//
//                            val previewWidth = previewView.width.toFloat()
//                            val previewHeight = previewView.height.toFloat()
//
//                            val scaleX = previewWidth / imageWidth
//                            val scaleY = previewHeight / imageHeight
//                            val scale = maxOf(scaleX, scaleY)
//
//                            val scaledWidth = imageWidth * scale
//                            val scaledHeight = imageHeight * scale
//
//                            val dx = (scaledWidth - previewWidth) / 2f
//                            val dy = (scaledHeight - previewHeight) / 2f
//
//                            fun mapPoint(x: Float, y: Float): Offset {
//                                return Offset(x * scale - dx, y * scale - dy)
//                            }
//
//                            val mappedLeft = mapPoint(lShoulder.position.x, lShoulder.position.y)
//                            val mappedRight = mapPoint(rShoulder.position.x, rShoulder.position.y)
//
//                            leftShoulder = mappedLeft
//                            rightShoulder = mappedRight
//
//                            val pixelDist = kotlin.math.abs(mappedRight.x - mappedLeft.x)
//
//                            // Use the dynamic sensor width here
//                            val meters = sensorWidthMeters ?: 0f
//                            // Convert pixel distance to meters approx:
//                            val lateralMeters = (pixelDist / previewWidth) * meters
//                            lateralDistanceMeters = lateralMeters
//                        }
//                    }
//                    .addOnCompleteListener {
//                        imageProxy.close()
//                    }
//            } else {
//                imageProxy.close()
//            }
//        }
//
//        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
//        cameraProvider.unbindAll()
//        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, analysisUseCase)
//    }
//
//    Box(modifier = Modifier.fillMaxSize()) {
//        Canvas(modifier = Modifier.fillMaxSize()) {
//            if (leftShoulder != null && rightShoulder != null) {
//                val l = leftShoulder!!
//                val r = rightShoulder!!
//
//                drawCircle(color = Color.Red, radius = 15f, center = l)
//                drawCircle(color = Color.Red, radius = 15f, center = r)
//
//                val left = kotlin.math.min(l.x, r.x) - 40f
//                val right = kotlin.math.max(l.x, r.x) + 40f
//                val top = kotlin.math.min(l.y, r.y) - 40f
//                val bottom = kotlin.math.max(l.y, r.y) + 40f
//
//                drawRect(
//                    color = Color.Cyan.copy(alpha = 0.4f),
//                    topLeft = Offset(left, top),
//                    size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
//                    style = Stroke(width = 5f)
//                )
//            }
//        }
//
//        Box(
//            modifier = Modifier
//                .align(Alignment.TopStart)
//                .padding(16.dp)
//                .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(8.dp))
//                .padding(12.dp)
//        ) {
//            Text(
//                text = "Lateral Distance: ${
//                    lateralDistanceMeters?.let { String.format("%.3f m", it) } ?: "--"
//                }",
//                color = if (lateralDistanceMeters != null && lateralDistanceMeters!! >= 3f) Color.Green else Color.Red,
//                fontSize = 20.sp,
//                fontWeight = FontWeight.Bold
//            )
//        }
//    }
//}
//


// claude implementation with distance walkable
//@OptIn(ExperimentalGetImage::class)
//@Composable
//fun LateralDistanceCameraScreen() {
//    val context = LocalContext.current
//    val lifecycleOwner = LocalLifecycleOwner.current
//
//    val targetResolution = android.util.Size(640, 480)
//
//    // States for shoulder points and walkable distance
//    var leftShoulder by remember { mutableStateOf<Offset?>(null) }
//    var rightShoulder by remember { mutableStateOf<Offset?>(null) }
//    var walkableDistanceMeters by remember { mutableStateOf<Float?>(null) }
//    var estimatedDistanceToPersonMeters by remember { mutableStateOf<Float?>(null) }
//
//    // Remember PreviewView instance
////    val previewView = remember { PreviewView(context) }
//    val previewView = remember {
//        PreviewView(context).apply {
//            scaleType = PreviewView.ScaleType.FILL_CENTER
//        }
//    }
////    val previewView = remember {
////        PreviewView(context).apply {
////            scaleType = PreviewView.ScaleType.FIT_CENTER
////        }
////    }
//    // Camera parameters
//    var sensorWidthMm by remember { mutableStateOf<Float?>(null) }
//    var focalLengthMm by remember { mutableStateOf<Float?>(null) }
//
//    // Get camera parameters on launch
//    LaunchedEffect(Unit) {
//        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
//
//        // Get camera ID for back camera
//        val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
//            val characteristics = cameraManager.getCameraCharacteristics(id)
//            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
//            lensFacing == CameraCharacteristics.LENS_FACING_BACK
//        }
//
//        cameraId?.let { id ->
//            val characteristics = cameraManager.getCameraCharacteristics(id)
//            val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
//            val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
//
//            // Keep sensor width in millimeters
//            sensorWidthMm = sensorSize?.width ?: 0f
//            // Use the first available focal length (usually the primary lens)
//            focalLengthMm = focalLengths?.firstOrNull() ?: 0f
//        }
//    }
//
//    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
//
//    LaunchedEffect(previewView, sensorWidthMm, focalLengthMm) {
//        if (sensorWidthMm == null || focalLengthMm == null) return@LaunchedEffect
//
//        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
//        val poseDetector = PoseDetection.getClient(
//            PoseDetectorOptions.Builder()
//                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
//                .build()
//        )
//
//        val preview = Preview.Builder()
//            .setTargetResolution(targetResolution)
//            .build().also {
//            it.setSurfaceProvider(previewView.surfaceProvider)
//        }
//
//        val analysisUseCase = ImageAnalysis.Builder()
//            // according to documentation, default size is 640x840
//            .setTargetResolution(targetResolution)
//            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//            .build()
//
//        analysisUseCase.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
//            val mediaImage = imageProxy.image
//            if (mediaImage != null) {
//                Log.d("LateralDistance", "ImageProxy: ${imageProxy.width} x ${imageProxy.height}")
//                Log.d("LateralDistance", "PreviewView: ${previewView.width} x ${previewView.height}")
//                val inputImage =
//                    InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
//
//                poseDetector.process(inputImage)
//                    .addOnSuccessListener { pose ->
//                        val lShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
//                        val rShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
//
//                        if (lShoulder != null && rShoulder != null) {
//                            val imageWidth = imageProxy.width.toFloat()
//                            val imageHeight = imageProxy.height.toFloat()
//
//                            val previewWidth = previewView.width.toFloat()
//                            val previewHeight = previewView.height.toFloat()
//
//                            val scaleX = previewWidth / imageWidth
//                            val scaleY = previewHeight / imageHeight
//                            val scale = maxOf(scaleX, scaleY)
//
//                            val scaledWidth = imageWidth * scale
//                            val scaledHeight = imageHeight * scale
//
//                            val dx = (scaledWidth - previewWidth) / 2f
//                            val dy = (scaledHeight - previewHeight) / 2f
//
//                            fun mapPoint(x: Float, y: Float): Offset {
//                                return Offset(x * scale - dx, y * scale - dy)
//                            }
//
//                            val mappedLeft = mapPoint(lShoulder.position.x, lShoulder.position.y)
//                            val mappedRight = mapPoint(rShoulder.position.x, rShoulder.position.y)
//
//                            leftShoulder = mappedLeft
//                            rightShoulder = mappedRight
//
//                            // Calculate shoulder width in pixels
//                            val shoulderWidthPixels = kotlin.math.abs(mappedRight.x - mappedLeft.x)
//
//                            // Estimate distance to person using known average shoulder width
//                            val averageShoulderWidthMm = 450f // Average adult shoulder width in mm
//                            val shoulderWidthOnSensorMm = (shoulderWidthPixels / imageWidth) * sensorWidthMm!!
//                            val distanceToPersonMm = (averageShoulderWidthMm * focalLengthMm!!) / shoulderWidthOnSensorMm
//                            val distanceToPersonMeters = distanceToPersonMm / 1000f
//
//                            estimatedDistanceToPersonMeters = distanceToPersonMeters
//
//                            // Calculate the real-world width that the camera can see at this distance
//                            val realWorldWidthMm = (previewWidth * distanceToPersonMeters * sensorWidthMm!!) /
//                                    (focalLengthMm!! * imageWidth)
//
//                            // Convert to meters for walkable distance
//                            walkableDistanceMeters = realWorldWidthMm / 1000f
//                        }
//                    }
//                    .addOnCompleteListener {
//                        imageProxy.close()
//                    }
//            } else {
//                imageProxy.close()
//            }
//        }
//
//        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
//        cameraProvider.unbindAll()
//        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, analysisUseCase)
//    }
//
//    Box(modifier = Modifier.fillMaxSize()) {
//        Canvas(modifier = Modifier.fillMaxSize()) {
//            if (leftShoulder != null && rightShoulder != null) {
//                val l = leftShoulder!!
//                val r = rightShoulder!!
//
//                drawCircle(color = Color.Red, radius = 15f, center = l)
//                drawCircle(color = Color.Red, radius = 15f, center = r)
//
//                val left = kotlin.math.min(l.x, r.x) - 40f
//                val right = kotlin.math.max(l.x, r.x) + 40f
//                val top = kotlin.math.min(l.y, r.y) - 40f
//                val bottom = kotlin.math.max(l.y, r.y) + 40f
//
//                drawRect(
//                    color = Color.Cyan.copy(alpha = 0.4f),
//                    topLeft = Offset(left, top),
//                    size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
//                    style = Stroke(width = 5f)
//                )
//
//                // Draw frame boundaries to show walkable area
//                drawRect(
//                    color = Color.Green.copy(alpha = 0.3f),
//                    topLeft = Offset(0f, 0f),
//                    size = androidx.compose.ui.geometry.Size(size.width, size.height),
//                    style = Stroke(width = 8f)
//                )
//            }
//        }
//
//        // Display walkable distance
//        Box(
//            modifier = Modifier
//                .align(Alignment.TopStart)
//                .padding(16.dp)
//                .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(8.dp))
//                .padding(12.dp)
//        ) {
//            Column {
//                Text(
//                    text = "Walkable Distance: ${
//                        walkableDistanceMeters?.let { String.format("%.2f m", it) } ?: "--"
//                    }",
//                    color = if (walkableDistanceMeters != null && walkableDistanceMeters!! >= 3f) Color.Green else Color.Red,
//                    fontSize = 18.sp,
//                    fontWeight = FontWeight.Bold
//                )
//                Text(
//                    text = "Est. Distance: ${
//                        estimatedDistanceToPersonMeters?.let { String.format("%.1f m", it) } ?: "--"
//                    }",
//                    color = Color.White,
//                    fontSize = 14.sp
//                )
//            }
//        }
//
//        // Instructions
//        Box(
//            modifier = Modifier
//                .align(Alignment.BottomCenter)
//                .padding(16.dp)
//                .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(8.dp))
//                .padding(12.dp)
//        ) {
//            Text(
//                text = "Position camera to view person from the side.\nWalkable distance shows how far they can move left-right in frame.",
//                color = Color.White,
//                fontSize = 12.sp,
//                textAlign = TextAlign.Center
//            )
//        }
//    }
//}

//// 4:3 ratio
//@OptIn(ExperimentalGetImage::class)
//@Composable
//fun LateralDistanceCameraScreen() {
//    val context = LocalContext.current
//    val lifecycleOwner = LocalLifecycleOwner.current
//
//    // States for shoulder points and walkable distance
//    var leftShoulder by remember { mutableStateOf<Offset?>(null) }
//    var rightShoulder by remember { mutableStateOf<Offset?>(null) }
//    var walkableDistanceMeters by remember { mutableStateOf<Float?>(null) }
//    var estimatedDistanceToPersonMeters by remember { mutableStateOf<Float?>(null) }
//
//    // Remember PreviewView instance with 4:3 aspect ratio
//    val previewView = remember {
//        PreviewView(context).apply {
//            scaleType = PreviewView.ScaleType.FIT_CENTER
//            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
//        }
//    }
//
//    // Camera parameters
//    var sensorWidthMm by remember { mutableStateOf<Float?>(null) }
//    var focalLengthMm by remember { mutableStateOf<Float?>(null) }
//
//    // Get camera parameters on launch
//    LaunchedEffect(Unit) {
//        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
//
//        // Get camera ID for back camera
//        val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
//            val characteristics = cameraManager.getCameraCharacteristics(id)
//            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
//            lensFacing == CameraCharacteristics.LENS_FACING_BACK
//        }
//
//        cameraId?.let { id ->
//            val characteristics = cameraManager.getCameraCharacteristics(id)
//            val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
//            val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
//
//            // Keep sensor width in millimeters
//            sensorWidthMm = sensorSize?.width ?: 0f
//            // Use the first available focal length (usually the primary lens)
//            focalLengthMm = focalLengths?.firstOrNull() ?: 0f
//        }
//    }
//
//    Box(modifier = Modifier.fillMaxSize()) {
//        // Use AspectRatio to enforce 4:3 aspect ratio
//        AndroidView(
//            factory = { previewView },
//            modifier = Modifier
//                .wrapContentSize()
//                .aspectRatio(4f / 3f)
//                .align(Alignment.Center)
//        )
//    }
//
//    LaunchedEffect(previewView, sensorWidthMm, focalLengthMm) {
//        if (sensorWidthMm == null || focalLengthMm == null) return@LaunchedEffect
//
//        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
//        val poseDetector = PoseDetection.getClient(
//            PoseDetectorOptions.Builder()
//                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
//                .build()
//        )
//
//        // Configure preview with 4:3 aspect ratio
//        val preview = Preview.Builder()
//            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
//            .build()
//            .also {
//                it.setSurfaceProvider(previewView.surfaceProvider)
//            }
//
//        // Configure image analysis with 4:3 aspect ratio
//        val analysisUseCase = ImageAnalysis.Builder()
//            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
//            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//            .build()
//
//        analysisUseCase.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
//            val mediaImage = imageProxy.image
//            if (mediaImage != null) {
//                val inputImage =
//                    InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
//
//                poseDetector.process(inputImage)
//                    .addOnSuccessListener { pose ->
//                        val lShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
//                        val rShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
//
//                        if (lShoulder != null && rShoulder != null) {
//                            val imageWidth = imageProxy.width.toFloat()
//                            val imageHeight = imageProxy.height.toFloat()
//
//                            val previewWidth = previewView.width.toFloat()
//                            val previewHeight = previewView.height.toFloat()
//
//                            // Calculate scale factors for 4:3 aspect ratio mapping
//                            val scaleX = previewWidth / imageWidth
//                            val scaleY = previewHeight / imageHeight
//                            val scale = minOf(scaleX, scaleY) // Use minOf for FIT_CENTER behavior
//
//                            val scaledWidth = imageWidth * scale
//                            val scaledHeight = imageHeight * scale
//
//                            val dx = (previewWidth - scaledWidth) / 2f
//                            val dy = (previewHeight - scaledHeight) / 2f
//
//                            fun mapPoint(x: Float, y: Float): Offset {
//                                return Offset(x * scale + dx, y * scale + dy)
//                            }
//
//                            val mappedLeft = mapPoint(lShoulder.position.x, lShoulder.position.y)
//                            val mappedRight = mapPoint(rShoulder.position.x, rShoulder.position.y)
//
//                            leftShoulder = mappedLeft
//                            rightShoulder = mappedRight
//
//                            // Calculate shoulder width in pixels
//                            val shoulderWidthPixels = kotlin.math.abs(mappedRight.x - mappedLeft.x)
//
//                            // Estimate distance to person using known average shoulder width
//                            val averageShoulderWidthMm = 450f // Average adult shoulder width in mm
//                            val shoulderWidthOnSensorMm = (shoulderWidthPixels / scaledWidth) * sensorWidthMm!!
//                            val distanceToPersonMm = (averageShoulderWidthMm * focalLengthMm!!) / shoulderWidthOnSensorMm
//                            val distanceToPersonMeters = distanceToPersonMm / 1000f
//
//                            estimatedDistanceToPersonMeters = distanceToPersonMeters
//
//                            // Calculate the real-world width that the camera can see at this distance
//                            val realWorldWidthMm = (previewWidth * distanceToPersonMeters * sensorWidthMm!!) /
//                                    (focalLengthMm!! * scaledWidth)
//
//                            // Convert to meters for walkable distance
//                            walkableDistanceMeters = realWorldWidthMm / 1000f
//                        }
//                    }
//                    .addOnCompleteListener {
//                        imageProxy.close()
//                    }
//            } else {
//                imageProxy.close()
//            }
//        }
//
//        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
//        cameraProvider.unbindAll()
//        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, analysisUseCase)
//    }
//
//    Box(modifier = Modifier.fillMaxSize()) {
//        Canvas(modifier = Modifier.fillMaxSize()) {
//            if (leftShoulder != null && rightShoulder != null) {
//                val l = leftShoulder!!
//                val r = rightShoulder!!
//
//                drawCircle(color = Color.Red, radius = 15f, center = l)
//                drawCircle(color = Color.Red, radius = 15f, center = r)
//
//                val left = kotlin.math.min(l.x, r.x) - 40f
//                val right = kotlin.math.max(l.x, r.x) + 40f
//                val top = kotlin.math.min(l.y, r.y) - 40f
//                val bottom = kotlin.math.max(l.y, r.y) + 40f
//
//                drawRect(
//                    color = Color.Cyan.copy(alpha = 0.4f),
//                    topLeft = Offset(left, top),
//                    size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
//                    style = Stroke(width = 5f)
//                )
//
//                // Draw frame boundaries to show walkable area
//                drawRect(
//                    color = Color.Green.copy(alpha = 0.3f),
//                    topLeft = Offset(0f, 0f),
//                    size = androidx.compose.ui.geometry.Size(size.width, size.height),
//                    style = Stroke(width = 8f)
//                )
//            }
//        }
//
//        // Display walkable distance
//        Box(
//            modifier = Modifier
//                .align(Alignment.TopStart)
//                .padding(16.dp)
//                .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(8.dp))
//                .padding(12.dp)
//        ) {
//            Column {
//                Text(
//                    text = "Walkable Distance: ${
//                        walkableDistanceMeters?.let { String.format("%.2f m", it) } ?: "--"
//                    }",
//                    color = if (walkableDistanceMeters != null && walkableDistanceMeters!! >= 3f) Color.Green else Color.Red,
//                    fontSize = 18.sp,
//                    fontWeight = FontWeight.Bold
//                )
//                Text(
//                    text = "Est. Distance: ${
//                        estimatedDistanceToPersonMeters?.let { String.format("%.1f m", it) } ?: "--"
//                    }",
//                    color = Color.White,
//                    fontSize = 14.sp
//                )
//            }
//        }
//
//        // Instructions
//        Box(
//            modifier = Modifier
//                .align(Alignment.BottomCenter)
//                .padding(16.dp)
//                .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(8.dp))
//                .padding(12.dp)
//        ) {
//            Text(
//                text = "Position camera to view person from the side.\nWalkable distance shows how far they can move left-right in frame.",
//                color = Color.White,
//                fontSize = 12.sp,
//                textAlign = TextAlign.Center
//            )
//        }
//    }
//}


// claude

//@OptIn(ExperimentalGetImage::class)
//@Composable
//fun LateralDistanceCameraScreen() {
//    val context = LocalContext.current
//    val lifecycleOwner = LocalLifecycleOwner.current
//
//    // States for shoulder points and walkable distance
//    var leftShoulder by remember { mutableStateOf<Offset?>(null) }
//    var rightShoulder by remember { mutableStateOf<Offset?>(null) }
//    var walkableDistanceMeters by remember { mutableStateOf<Float?>(null) }
//    var estimatedDistanceToPersonMeters by remember { mutableStateOf<Float?>(null) }
//
//    // Remember PreviewView instance
//    val previewView = remember {
//        PreviewView(context).apply {
//            scaleType = PreviewView.ScaleType.FILL_CENTER
//        }
//    }
//
//    // Camera parameters
//    var sensorWidthMm by remember { mutableStateOf<Float?>(null) }
//    var focalLengthMm by remember { mutableStateOf<Float?>(null) }
//
//    // Get camera parameters on launch
//    LaunchedEffect(Unit) {
//        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
//
//        // Get camera ID for back camera
//        val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
//            val characteristics = cameraManager.getCameraCharacteristics(id)
//            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
//            lensFacing == CameraCharacteristics.LENS_FACING_BACK
//        }
//
//        cameraId?.let { id ->
//            val characteristics = cameraManager.getCameraCharacteristics(id)
//            val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
//            val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
//
//            // Keep sensor width in millimeters
//            sensorWidthMm = sensorSize?.width ?: 0f
//            // Use the first available focal length (usually the primary lens)
//            focalLengthMm = focalLengths?.firstOrNull() ?: 0f
//        }
//    }
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .systemBarsPadding() // Respects system bars and notches
//    ) {
//        AndroidView(
//            factory = { previewView },
//            modifier = Modifier
//                .aspectRatio(4f / 3f) // Maintain 4:3 aspect ratio
//                .fillMaxWidth() // Take 80% of screen width, adjust as needed
//                .align(Alignment.Center)
//        )
//    }
//
//    LaunchedEffect(previewView, sensorWidthMm, focalLengthMm) {
//        if (sensorWidthMm == null || focalLengthMm == null) return@LaunchedEffect
//
//        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
//        val poseDetector = PoseDetection.getClient(
//            PoseDetectorOptions.Builder()
//                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
//                .build()
//        )
//
//        val resolutionSelector = ResolutionSelector.Builder()
//            .setResolutionStrategy(
//                ResolutionStrategy(
//                    android.util.Size(640, 480),
//                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
//                )
//            )
//            .build()
//
//        val preview = Preview.Builder()
//            .setResolutionSelector(resolutionSelector)
//            .build().also {
//                it.setSurfaceProvider(previewView.surfaceProvider)
//            }
//
//        val analysisUseCase = ImageAnalysis.Builder()
//            .setResolutionSelector(resolutionSelector)
//            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//            .build()
//
//        analysisUseCase.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
//            val mediaImage = imageProxy.image
//            if (mediaImage != null) {
//                Log.d("LateralDistance", "ImageProxy: ${imageProxy.width} x ${imageProxy.height}")
//                Log.d("LateralDistance", "PreviewView: ${previewView.width} x ${previewView.height}")
//                val inputImage =
//                    InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
//
//                poseDetector.process(inputImage)
//                    .addOnSuccessListener { pose ->
//                        val lShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
//                        val rShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
//
//                        if (lShoulder != null && rShoulder != null) {
//                            val imageWidth = imageProxy.width.toFloat()
//                            val imageHeight = imageProxy.height.toFloat()
//
//                            val previewWidth = previewView.width.toFloat()
//                            val previewHeight = previewView.height.toFloat()
//
//                            val scaleX = previewWidth / imageWidth
//                            val scaleY = previewHeight / imageHeight
//                            val scale = maxOf(scaleX, scaleY)
//
//                            val scaledWidth = imageWidth * scale
//                            val scaledHeight = imageHeight * scale
//
//                            val dx = (scaledWidth - previewWidth) / 2f
//                            val dy = (scaledHeight - previewHeight) / 2f
//
//                            fun mapPoint(x: Float, y: Float): Offset {
//                                return Offset(x * scale - dx, y * scale - dy)
//                            }
//
//                            val mappedLeft = mapPoint(lShoulder.position.x, lShoulder.position.y)
//                            val mappedRight = mapPoint(rShoulder.position.x, rShoulder.position.y)
//
//                            leftShoulder = mappedLeft
//                            rightShoulder = mappedRight
//
//                            // Calculate shoulder width in pixels
//                            val shoulderWidthPixels = kotlin.math.abs(mappedRight.x - mappedLeft.x)
//
//                            // Estimate distance to person using known average shoulder width
//                            val averageShoulderWidthMm = 450f // Average adult shoulder width in mm
//                            val shoulderWidthOnSensorMm = (shoulderWidthPixels / imageWidth) * sensorWidthMm!!
//                            val distanceToPersonMm = (averageShoulderWidthMm * focalLengthMm!!) / shoulderWidthOnSensorMm
//                            val distanceToPersonMeters = distanceToPersonMm / 1000f
//
//                            estimatedDistanceToPersonMeters = distanceToPersonMeters
//
//                            // Calculate the real-world width that the camera can see at this distance
//                            val realWorldWidthMm = (previewWidth * distanceToPersonMeters * sensorWidthMm!!) /
//                                    (focalLengthMm!! * imageWidth)
//
//                            // Convert to meters for walkable distance
//                            walkableDistanceMeters = realWorldWidthMm / 1000f
//                        }
//                    }
//                    .addOnCompleteListener {
//                        imageProxy.close()
//                    }
//            } else {
//                imageProxy.close()
//            }
//        }
//
//        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
//        cameraProvider.unbindAll()
//        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, analysisUseCase)
//    }
//
//    Box(modifier = Modifier.fillMaxSize()) {
//        Canvas(modifier = Modifier.fillMaxSize()) {
//            if (leftShoulder != null && rightShoulder != null) {
//                val l = leftShoulder!!
//                val r = rightShoulder!!
//
//                drawCircle(color = Color.Red, radius = 15f, center = l)
//                drawCircle(color = Color.Red, radius = 15f, center = r)
//
//                val left = kotlin.math.min(l.x, r.x) - 40f
//                val right = kotlin.math.max(l.x, r.x) + 40f
//                val top = kotlin.math.min(l.y, r.y) - 40f
//                val bottom = kotlin.math.max(l.y, r.y) + 40f
//
//                drawRect(
//                    color = Color.Cyan.copy(alpha = 0.4f),
//                    topLeft = Offset(left, top),
//                    size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
//                    style = Stroke(width = 5f)
//                )
//
//                // Draw frame boundaries to show walkable area
//                drawRect(
//                    color = Color.Green.copy(alpha = 0.3f),
//                    topLeft = Offset(0f, 0f),
//                    size = androidx.compose.ui.geometry.Size(size.width, size.height),
//                    style = Stroke(width = 8f)
//                )
//            }
//        }
//
//        // Display walkable distance
//        Box(
//            modifier = Modifier
//                .align(Alignment.TopStart)
//                .padding(16.dp)
//                .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(8.dp))
//                .padding(12.dp)
//        ) {
//            Column {
//                Text(
//                    text = "Walkable Distance: ${
//                        walkableDistanceMeters?.let { String.format("%.2f m", it) } ?: "--"
//                    }",
//                    color = if (walkableDistanceMeters != null && walkableDistanceMeters!! >= 3f) Color.Green else Color.Red,
//                    fontSize = 18.sp,
//                    fontWeight = FontWeight.Bold
//                )
//                Text(
//                    text = "Est. Distance: ${
//                        estimatedDistanceToPersonMeters?.let { String.format("%.1f m", it) } ?: "--"
//                    }",
//                    color = Color.White,
//                    fontSize = 14.sp
//                )
//            }
//        }
//
//        // Instructions
//        Box(
//            modifier = Modifier
//                .align(Alignment.BottomCenter)
//                .padding(16.dp)
//                .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(8.dp))
//                .padding(12.dp)
//        ) {
//            Text(
//                text = "Position camera to view person from the side.\nWalkable distance shows how far they can move left-right in frame.",
//                color = Color.White,
//                fontSize = 12.sp,
//                textAlign = TextAlign.Center
//            )
//        }
//    }
//}

//@OptIn(ExperimentalGetImage::class)
//@Composable
//fun LateralDistanceCameraScreen() {
//    val context = LocalContext.current
//    val lifecycleOwner = LocalLifecycleOwner.current
//
//    // States for shoulder points and walkable distance
//    var leftShoulder by remember { mutableStateOf<Offset?>(null) }
//    var rightShoulder by remember { mutableStateOf<Offset?>(null) }
//    var walkableDistanceMeters by remember { mutableStateOf<Float?>(null) }
//    var estimatedDistanceToPersonMeters by remember { mutableStateOf<Float?>(null) }
//
//    // Remember PreviewView instance
//    val previewView = remember {
//        PreviewView(context).apply {
//            scaleType = PreviewView.ScaleType.FILL_CENTER
//        }
//    }
//
//    // Camera parameters
//    var sensorWidthMm by remember { mutableStateOf<Float?>(null) }
//    var focalLengthMm by remember { mutableStateOf<Float?>(null) }
//
//    // Get camera parameters on launch
//    LaunchedEffect(Unit) {
//        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
//
//        // Get camera ID for back camera
//        val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
//            val characteristics = cameraManager.getCameraCharacteristics(id)
//            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
//            lensFacing == CameraCharacteristics.LENS_FACING_BACK
//        }
//
//        cameraId?.let { id ->
//            val characteristics = cameraManager.getCameraCharacteristics(id)
//            val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
//            val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
//
//            // Keep sensor width in millimeters
//            sensorWidthMm = sensorSize?.width ?: 0f
//            // Use the first available focal length (usually the primary lens)
//            focalLengthMm = focalLengths?.firstOrNull() ?: 0f
//        }
//    }
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .systemBarsPadding() // Respects system bars and notches
//    ) {
//        AndroidView(
//            factory = { previewView },
//            modifier = Modifier
//                .aspectRatio(4f / 3f) // Maintain 4:3 aspect ratio
//                .fillMaxWidth(0.8f) // Take 80% of screen width, adjust as needed
//                .align(Alignment.Center)
//        )
//    }
//
//    LaunchedEffect(previewView, sensorWidthMm, focalLengthMm) {
//        if (sensorWidthMm == null || focalLengthMm == null) return@LaunchedEffect
//
//        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
//        val poseDetector = PoseDetection.getClient(
//            PoseDetectorOptions.Builder()
//                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
//                .build()
//        )
//
//        val resolutionSelector = ResolutionSelector.Builder()
//            .setResolutionStrategy(
//                ResolutionStrategy(
//                    android.util.Size(640, 480),
//                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
//                )
//            )
//            .build()
//
//        val preview = Preview.Builder()
//            .setResolutionSelector(resolutionSelector)
//            .build().also {
//                it.setSurfaceProvider(previewView.surfaceProvider)
//            }
//
//        val analysisUseCase = ImageAnalysis.Builder()
//            .setResolutionSelector(resolutionSelector)
//            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//            .build()
//
//        analysisUseCase.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
//            val mediaImage = imageProxy.image
//            if (mediaImage != null) {
//                Log.d("LateralDistance", "ImageProxy: ${imageProxy.width} x ${imageProxy.height}")
//                Log.d("LateralDistance", "PreviewView: ${previewView.width} x ${previewView.height}")
//                val inputImage =
//                    InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
//
//                poseDetector.process(inputImage)
//                    .addOnSuccessListener { pose ->
//                        val lShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
//                        val rShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
//
//                        if (lShoulder != null && rShoulder != null) {
//                            val imageWidth = imageProxy.width.toFloat()
//                            val imageHeight = imageProxy.height.toFloat()
//
//                            val previewWidth = previewView.width.toFloat()
//                            val previewHeight = previewView.height.toFloat()
//
//                            val scaleX = previewWidth / imageWidth
//                            val scaleY = previewHeight / imageHeight
//                            val scale = maxOf(scaleX, scaleY)
//
//                            val scaledWidth = imageWidth * scale
//                            val scaledHeight = imageHeight * scale
//
//                            val dx = (scaledWidth - previewWidth) / 2f
//                            val dy = (scaledHeight - previewHeight) / 2f
//
//                            fun mapPoint(x: Float, y: Float): Offset {
//                                return Offset(x * scale - dx, y * scale - dy)
//                            }
//
//                            val mappedLeft = mapPoint(lShoulder.position.x, lShoulder.position.y)
//                            val mappedRight = mapPoint(rShoulder.position.x, rShoulder.position.y)
//
//                            leftShoulder = mappedLeft
//                            rightShoulder = mappedRight
//
//                            // Calculate shoulder width in pixels (in image coordinates)
//                            val shoulderWidthPixels = kotlin.math.abs(mappedRight.x - mappedLeft.x)
//
//                            // Convert shoulder width from preview coordinates back to image coordinates
//                            val shoulderWidthImagePixels = shoulderWidthPixels / scale
//
//                            // Estimate distance to person using known average shoulder width
//                            val averageShoulderWidthMm = 450f // Average adult shoulder width in mm
//                            val shoulderWidthOnSensorMm = (shoulderWidthImagePixels / imageWidth) * sensorWidthMm!!
//                            val distanceToPersonMm = (averageShoulderWidthMm * focalLengthMm!!) / shoulderWidthOnSensorMm
//                            val distanceToPersonMeters = distanceToPersonMm / 1000f
//                            Log.d("LateralDistance", "Distance to person: $distanceToPersonMeters meters")
//                            estimatedDistanceToPersonMeters = distanceToPersonMeters
//
//                            // Calculate the real-world width that the camera can see at this distance
//                            // This is the field of view width - how much real world space the camera sees
//                            val realWorldWidthMm = (distanceToPersonMeters * sensorWidthMm!!) / focalLengthMm!!
//
//                            // Convert to meters for walkable distance
//                            walkableDistanceMeters = realWorldWidthMm / 1000f
//                            Log.d("LateralDistance", "Walkable distance: $walkableDistanceMeters")
//                        }
//                    }
//                    .addOnCompleteListener {
//                        imageProxy.close()
//                    }
//            } else {
//                imageProxy.close()
//            }
//        }
//
//        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
//        cameraProvider.unbindAll()
//        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, analysisUseCase)
//    }
//
//    Box(modifier = Modifier.fillMaxSize()) {
//        Canvas(modifier = Modifier.fillMaxSize()) {
//            if (leftShoulder != null && rightShoulder != null) {
//                val l = leftShoulder!!
//                val r = rightShoulder!!
//
//                drawCircle(color = Color.Red, radius = 15f, center = l)
//                drawCircle(color = Color.Red, radius = 15f, center = r)
//
//                val left = kotlin.math.min(l.x, r.x) - 40f
//                val right = kotlin.math.max(l.x, r.x) + 40f
//                val top = kotlin.math.min(l.y, r.y) - 40f
//                val bottom = kotlin.math.max(l.y, r.y) + 40f
//
//                drawRect(
//                    color = Color.Cyan.copy(alpha = 0.4f),
//                    topLeft = Offset(left, top),
//                    size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
//                    style = Stroke(width = 5f)
//                )
//
//                // Draw frame boundaries to show walkable area
//                drawRect(
//                    color = Color.Green.copy(alpha = 0.3f),
//                    topLeft = Offset(0f, 0f),
//                    size = androidx.compose.ui.geometry.Size(size.width, size.height),
//                    style = Stroke(width = 8f)
//                )
//            }
//        }
//
//        // Display walkable distance
//        Box(
//            modifier = Modifier
//                .align(Alignment.TopStart)
//                .padding(16.dp)
//                .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(8.dp))
//                .padding(12.dp)
//        ) {
//            Column {
//                Text(
//                    text = "Walkable Distance: ${
//                        walkableDistanceMeters?.let { String.format("%.2f m", it) } ?: "--"
//                    }",
//                    color = if (walkableDistanceMeters != null && walkableDistanceMeters!! >= 3f) Color.Green else Color.Red,
//                    fontSize = 18.sp,
//                    fontWeight = FontWeight.Bold
//                )
//                Text(
//                    text = "Est. Distance: ${
//                        estimatedDistanceToPersonMeters?.let { String.format("%.1f m", it) } ?: "--"
//                    }",
//                    color = Color.White,
//                    fontSize = 14.sp
//                )
//            }
//        }
//
//        // Instructions
//        Box(
//            modifier = Modifier
//                .align(Alignment.BottomCenter)
//                .padding(16.dp)
//                .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(8.dp))
//                .padding(12.dp)
//        ) {
//            Text(
//                text = "Position camera to view person from the side.\nWalkable distance shows how far they can move left-right in frame.",
//                color = Color.White,
//                fontSize = 12.sp,
//                textAlign = TextAlign.Center
//            )
//        }
//    }
//}

@OptIn(ExperimentalGetImage::class)
@Composable
fun LateralDistanceCameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // States for shoulder points and walkable distance
    var leftShoulder by remember { mutableStateOf<Offset?>(null) }
    var rightShoulder by remember { mutableStateOf<Offset?>(null) }
    var walkableDistanceMeters by remember { mutableStateOf<Float?>(null) }
    var estimatedDistanceToPersonMeters by remember { mutableStateOf<Float?>(null) }

    // Remember PreviewView instance
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // Camera parameters
    var sensorWidthMm by remember { mutableStateOf<Float?>(null) }
    var focalLengthMm by remember { mutableStateOf<Float?>(null) }

    // Get camera parameters on launch
    LaunchedEffect(Unit) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        // Get camera ID for back camera
        val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
            lensFacing == CameraCharacteristics.LENS_FACING_BACK
        }

        cameraId?.let { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
            val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)

            // Keep sensor width in millimeters
            sensorWidthMm = sensorSize?.width ?: 0f
            // Use the first available focal length (usually the primary lens)
            focalLengthMm = focalLengths?.firstOrNull() ?: 0f

            // DEBUG: Camera parameters
            Log.d("CameraParams", "=== CAMERA PARAMETERS ===")
            Log.d("CameraParams", "Camera ID: $id")
            Log.d("CameraParams", "Sensor width: $sensorWidthMm mm")
            Log.d("CameraParams", "Sensor height: ${sensorSize?.height} mm")
            Log.d("CameraParams", "Focal length: $focalLengthMm mm")
            Log.d("CameraParams", "All focal lengths: ${focalLengths?.joinToString(", ")}")
            Log.d("CameraParams", "========================")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding() // Respects system bars and notches
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .aspectRatio(4f / 3f) // Maintain 4:3 aspect ratio
                .fillMaxWidth(0.8f) // Take 80% of screen width, adjust as needed
                .align(Alignment.Center)
        )
//        AndroidView(
//            factory = { previewView },
//            modifier = Modifier
//                .fillMaxSize()  // Instead of aspectRatio(4f/3f).fillMaxWidth(0.8f)
//                .align(Alignment.Center)
//        )
    }

    LaunchedEffect(previewView, sensorWidthMm, focalLengthMm) {
        if (sensorWidthMm == null || focalLengthMm == null) {
            Log.e("CameraParams", "Camera parameters not available!")
            return@LaunchedEffect
        }

        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        val poseDetector = PoseDetection.getClient(
            PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .build()
        )

        val resolutionSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(
                    android.util.Size(640, 480),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                )
            )
            .build()

        val preview = Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        val analysisUseCase = ImageAnalysis.Builder()
            .setResolutionSelector(resolutionSelector)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        analysisUseCase.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val inputImage =
                    InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                poseDetector.process(inputImage)
                    .addOnSuccessListener { pose ->
                        val lShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
                        val rShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)

                        if (lShoulder != null && rShoulder != null) {
                            val imageWidth = imageProxy.width.toFloat()
                            val imageHeight = imageProxy.height.toFloat()

                            val previewWidth = previewView.width.toFloat()
                            val previewHeight = previewView.height.toFloat()

                            val scaleX = previewWidth / imageWidth
                            val scaleY = previewHeight / imageHeight
                            val scale = maxOf(scaleX, scaleY)

                            val scaledWidth = imageWidth * scale
                            val scaledHeight = imageHeight * scale

                            val dx = (scaledWidth - previewWidth) / 2f
                            val dy = (scaledHeight - previewHeight) / 2f

                            fun mapPoint(x: Float, y: Float): Offset {
                                return Offset(x * scale - dx, y * scale - dy)
                            }

                            val mappedLeft = mapPoint(lShoulder.position.x, lShoulder.position.y)
                            val mappedRight = mapPoint(rShoulder.position.x, rShoulder.position.y)

                            leftShoulder = mappedLeft
                            rightShoulder = mappedRight

                            // Calculate shoulder width in pixels (in image coordinates)
                            val shoulderWidthPixels = kotlin.math.abs(mappedRight.x - mappedLeft.x)

                            // Convert shoulder width from preview coordinates back to image coordinates
                            val shoulderWidthImagePixels = shoulderWidthPixels / scale

                            // Estimate distance to person using known average shoulder width
                            val averageShoulderWidthMm = 450f // Average adult shoulder width in mm
                            val shoulderWidthOnSensorMm = (shoulderWidthImagePixels / imageWidth) * sensorWidthMm!!
                            val distanceToPersonMm = (averageShoulderWidthMm * focalLengthMm!!) / shoulderWidthOnSensorMm
                            val distanceToPersonMeters = distanceToPersonMm / 1000f

                            estimatedDistanceToPersonMeters = distanceToPersonMeters

                            // Calculate the real-world width that the camera can see at this distance
                            // This is the field of view width - how much real world space the camera sees
                            val realWorldWidthMm = (distanceToPersonMm * sensorWidthMm!!) / focalLengthMm!!

                            // Convert to meters for walkable distance
                            walkableDistanceMeters = realWorldWidthMm / 1000f

                            // DEBUG: All calculations
                            Log.d("PoseDebug", "=== POSE DETECTION DEBUG ===")
                            Log.d("PoseDebug", "Image size: ${imageWidth}x${imageHeight}")
                            Log.d("PoseDebug", "Preview size: ${previewWidth}x${previewHeight}")
                            Log.d("PoseDebug", "Scale factors - X: $scaleX, Y: $scaleY, Used: $scale")
                            Log.d("PoseDebug", "Shoulder positions - Left: (${lShoulder.position.x}, ${lShoulder.position.y}), Right: (${rShoulder.position.x}, ${rShoulder.position.y})")
                            Log.d("PoseDebug", "Mapped positions - Left: $mappedLeft, Right: $mappedRight")
                            Log.d("PoseDebug", "Shoulder width (preview pixels): $shoulderWidthPixels")
                            Log.d("PoseDebug", "Shoulder width (image pixels): $shoulderWidthImagePixels")
                            Log.d("PoseDebug", "Shoulder width ratio: ${shoulderWidthImagePixels / imageWidth}")
                            Log.d("PoseDebug", "Shoulder width on sensor: $shoulderWidthOnSensorMm mm")
                            Log.d("PoseDebug", "Distance calculation: ($averageShoulderWidthMm * $focalLengthMm) / $shoulderWidthOnSensorMm")
                            Log.d("PoseDebug", "Distance to person: $distanceToPersonMm mm = $distanceToPersonMeters m")
                            Log.d("PoseDebug", "Real world width calculation: ($distanceToPersonMm * $sensorWidthMm) / $focalLengthMm")
                            Log.d("PoseDebug", "Real world width: $realWorldWidthMm mm")
                            Log.d("PoseDebug", "Walkable distance: $walkableDistanceMeters m")
                            Log.d("PoseDebug", "============================")
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("PoseDebug", "Pose detection failed", exception)
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                Log.w("PoseDebug", "MediaImage is null")
                imageProxy.close()
            }
        }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, analysisUseCase)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (leftShoulder != null && rightShoulder != null) {
                val l = leftShoulder!!
                val r = rightShoulder!!

                drawCircle(color = Color.Red, radius = 15f, center = l)
                drawCircle(color = Color.Red, radius = 15f, center = r)

                val left = kotlin.math.min(l.x, r.x) - 40f
                val right = kotlin.math.max(l.x, r.x) + 40f
                val top = kotlin.math.min(l.y, r.y) - 40f
                val bottom = kotlin.math.max(l.y, r.y) + 40f

                drawRect(
                    color = Color.Cyan.copy(alpha = 0.4f),
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                    style = Stroke(width = 5f)
                )

                // Draw frame boundaries to show walkable area
                drawRect(
                    color = Color.Green.copy(alpha = 0.3f),
                    topLeft = Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(size.width, size.height),
                    style = Stroke(width = 8f)
                )
            }
        }

        // Display walkable distance
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = "Walkable Distance: ${
                        walkableDistanceMeters?.let { String.format("%.2f m", it) } ?: "--"
                    }",
                    color = if (walkableDistanceMeters != null && walkableDistanceMeters!! >= 3f) Color.Green else Color.Red,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Est. Distance: ${
                        estimatedDistanceToPersonMeters?.let { String.format("%.1f m", it) } ?: "--"
                    }",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }

        // Instructions
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Text(
                text = "Position camera to view person from the side.\nWalkable distance shows how far they can move left-right in frame.",
                color = Color.White,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}