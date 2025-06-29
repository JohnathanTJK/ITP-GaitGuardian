package com.example.gaitguardian.screens.patient

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.util.Log
import android.util.SizeF
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin

// ONLY WITH SENSORMANAGER
@OptIn(ExperimentalCamera2Interop::class)
@SuppressLint("UnsafeOptInUsageError", "RestrictedApi")
@Composable
fun LateralCoverageScreen() {
    // State variables
    var personDistance by remember { mutableStateOf<Float?>(null) }
    var lateralCoverage by remember { mutableStateOf(0f) }
    var personHorizontalCoverage by remember { mutableStateOf(0f) }
    var covers3Meters by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Initializing...") }
    var debugInfo by remember { mutableStateOf("") }

    // Camera orientation state
    var cameraHeight by remember { mutableStateOf(1.5f) } // Default assumption: 1.5m height
    var cameraTiltAngle by remember { mutableStateOf(0f) } // Degrees from horizontal
    var cameraRoll by remember { mutableStateOf(0f) }
    var orientationReady by remember { mutableStateOf(false) }

    // Check if device is in landscape orientation
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = ContextCompat.getMainExecutor(context)

    // Store sensor listener reference for cleanup
    var sensorListener by remember { mutableStateOf<SensorEventListener?>(null) }

    // Sensor setup for camera orientation
    LaunchedEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val rotationMatrix = FloatArray(9)
                    val orientationValues = FloatArray(3)

                    SensorManager.getRotationMatrixFromVector(rotationMatrix, it.values)
                    SensorManager.getOrientation(rotationMatrix, orientationValues)

                    // Convert to degrees and adjust for camera orientation
                    val pitch = Math.toDegrees(orientationValues[1].toDouble()).toFloat()
                    val roll = Math.toDegrees(orientationValues[2].toDouble()).toFloat()

                    // When phone is held normally (portrait) with camera pointing out:
                    // - Positive pitch = camera tilted up
                    // - Negative pitch = camera tilted down
                    cameraTiltAngle = -pitch // Invert because we want angle from horizontal
                    cameraRoll = roll
                    orientationReady = true
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorListener = listener
        sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
    }

    // ML Kit Pose Detector
    val poseDetector = remember {
        PoseDetection.getClient(
            PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .build()
        )
    }

    // CameraX Setup
    val previewView = remember { PreviewView(context) }
    LaunchedEffect(Unit) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        // Get camera characteristics
        val camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector)
        val camera2Info = Camera2CameraInfo.from(camera.cameraInfo)
        val focalLength = camera2Info.getCameraCharacteristic(
            CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
        )?.firstOrNull() ?: 1.0f
        val sensorSize = camera2Info.getCameraCharacteristic(
            CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE
        ) ?: SizeF(0f, 0f)

        // Image Analysis Pipeline
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analyzer ->
                analyzer.setAnalyzer(executor) { imageProxy ->
                    // Only process if phone is in landscape orientation
                    if (!isLandscape) {
                        status = "Please rotate to landscape mode"
                        debugInfo = "Portrait mode detected - rotate device to landscape"
                        personDistance = null
                        personHorizontalCoverage = 0f
                        covers3Meters = false
                        imageProxy.close()
                        return@setAnalyzer
                    }

                    // Only process if orientation is ready
                    if (!orientationReady) {
                        status = "Waiting for orientation sensors..."
                        debugInfo = "Orientation sensors not ready yet"
                        personDistance = null
                        personHorizontalCoverage = 0f
                        covers3Meters = false
                        imageProxy.close()
                        return@setAnalyzer
                    }

                    poseDetector.process(
                        InputImage.fromMediaImage(
                            imageProxy.image!!,
                            imageProxy.imageInfo.rotationDegrees
                        )
                    ).addOnSuccessListener { pose ->
                        debugInfo = "Landmarks found: ${pose.allPoseLandmarks.size}"
                        debugInfo += " | Tilt: ${String.format("%.1f", cameraTiltAngle)}°"
                        Log.d("PoseDetection", "Pose detection success, landmarks: ${pose.allPoseLandmarks.size}")

                        if (pose.allPoseLandmarks.isNotEmpty()) {
                            Log.d("PoseDetection", "Processing pose landmarks...")
                            // Calculate bounding box from pose landmarks
                            val landmarks = pose.allPoseLandmarks
                            val xCoords = landmarks.map { it.position.x }
                            val yCoords = landmarks.map { it.position.y }

                            val left = (xCoords.minOrNull() ?: 0f).toInt()
                            val right = (xCoords.maxOrNull() ?: 0f).toInt()
                            val top = (yCoords.minOrNull() ?: 0f).toInt()
                            val bottom = (yCoords.maxOrNull() ?: 0f).toInt()

                            // Ensure valid bounding box
                            val boundingBox = if (right > left && bottom > top) {
                                Rect(left, top, right, bottom)
                            } else {
                                // Fallback: use image center with estimated size
                                val centerX = imageProxy.width / 2
                                val centerY = imageProxy.height / 2
                                val estimatedWidth = imageProxy.width / 3
                                val estimatedHeight = imageProxy.height / 2
                                Rect(
                                    centerX - estimatedWidth/2,
                                    centerY - estimatedHeight/2,
                                    centerX + estimatedWidth/2,
                                    centerY + estimatedHeight/2
                                )
                            }

                            debugInfo += " | Box: ${boundingBox.width()}x${boundingBox.height()}"
                            status = "Person detected! Processing..."
                            Log.d("BoundingBox", "Bounding box: ${boundingBox.width()}x${boundingBox.height()}")

                            // Use bounding box estimation for distance
                            Log.d("DistanceEstimation", "Estimating distance from bounding box...")
                            personDistance = estimateDistanceFromBoundingBox(
                                boundingBox,
                                imageProxy.height,
                                focalLength / 1000f
                            ).also { estimatedDistance ->
                                status = "Ground distance calculated"
                                Log.d("DistanceEstimation", "Estimated distance: $estimatedDistance")
                            }

                            Log.d("DistanceResult", "Final distance: $personDistance")

                            // Calculate coverage if distance available
                            personDistance?.let { distance ->
                                Log.d("CoverageCalculation", "Starting coverage calculation with distance: $distance")
                                Log.d("CoverageCalculation", "Camera params - focal: $focalLength, sensor: ${sensorSize.width}")
                                Log.d("CoverageCalculation", "Tilt: $cameraTiltAngle°")

                                // Calculate actual ground coverage using orientation
                                lateralCoverage = calculateGroundCoverage(
                                    focalLength / 1000f,
                                    sensorSize.width / 1000f ,
                                    distance,
                                    cameraHeight,
                                    cameraTiltAngle
                                )
                                Log.d("CoverageCalculation", "Ground lateral coverage: $lateralCoverage")

                                // Calculate person's actual ground coverage
                                personHorizontalCoverage = calculatePersonGroundCoverage(
                                    boundingBox,
                                    imageProxy.width,
                                    focalLength / 1000f,
                                    sensorSize.width / 1000f,
                                    distance,
                                    cameraHeight,
                                    cameraTiltAngle
                                )
                                Log.d("CoverageCalculation", "Person ground coverage: $personHorizontalCoverage")

                                // Check if person covers 3 meters horizontally
                                covers3Meters = personHorizontalCoverage >= 3.0f

                                debugInfo += " | Person: ${"%.2f".format(personHorizontalCoverage)}m | 3m: $covers3Meters"
                                Log.d("LateralCoverage", "Person width: ${personHorizontalCoverage}m, Covers 3m: $covers3Meters")
                            } ?: run {
                                Log.d("CoverageCalculation", "No distance available, skipping coverage calculation")
                            }
                        } else {
                            status = "No person detected"
                            debugInfo = "No pose landmarks detected"
                            debugInfo += " | Tilt: ${String.format("%.1f", cameraTiltAngle)}°"
                            Log.d("PoseDetection", "No pose landmarks detected")
                            personDistance = null
                            personHorizontalCoverage = 0f
                            covers3Meters = false
                        }
                    }.addOnFailureListener { e ->
                        Log.e("PoseDetection", "Detection failed", e)
                        status = "Pose detection error: ${e.message}"
                        personDistance = null
                        personHorizontalCoverage = 0f
                        covers3Meters = false
                    }.addOnCompleteListener {
                        imageProxy.close()
                    }
                }
            }

        // Bind use cases
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            },
            imageAnalysis
        )
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            // Unregister sensor listener
            sensorListener?.let { listener ->
                val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
                sensorManager.unregisterListener(listener)
            }
        }
    }

    // UI
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(16.dp)
        ) {
            Text(text = status, color = Color.White, fontSize = 16.sp)

            // Debug info
            Text(
                text = debugInfo,
                color = Color.Yellow,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            personDistance?.let { distance ->
                Text(
                    text = "Distance: ${"%.1f".format(distance)}m",
                    color = Color.White,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Text(
                    text = "Ground Coverage: ${"%.1f".format(lateralCoverage)}m",
                    color = if (lateralCoverage >= 6f) Color.Green else Color.Red,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Person Ground Width: ${"%.2f".format(personHorizontalCoverage)}m",
                    color = Color.Cyan,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // Camera info
                Text(
                    text = "📱 Height: ${String.format("%.1f", cameraHeight)}m, Tilt: ${String.format("%.1f", cameraTiltAngle)}°",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // 3-meter coverage check
            if (personHorizontalCoverage > 0f) {
                Text(
                    text = if (covers3Meters) {
                        "✅ Covers 3m+ ground distance"
                    } else {
                        "❌ Less than 3m coverage (${String.format("%.2f", personHorizontalCoverage)}m)"
                    },
                    color = if (covers3Meters) Color.Green else Color.Red,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

// Bounding box distance estimation
private fun estimateDistanceFromBoundingBox(
    box: Rect,
    imageHeight: Int,
    focalLengthPixels: Float
): Float {
    // Assumes average person height = 1.7m
    return (focalLengthPixels * 1.7f) / box.height()
}

// Calculate actual ground coverage distance
private fun calculateGroundCoverage(
    focalLength: Float,
    sensorWidth: Float,
    personDistance: Float,
    cameraHeight: Float,
    cameraTiltAngle: Float
): Float {
    val hFOV = 2 * atan(sensorWidth / (2 * focalLength))
    val halfFOV = hFOV / 2

    // Calculate where left and right edges of camera view hit the ground
    val leftGroundPos = calculateGroundIntersection(personDistance, cameraHeight, cameraTiltAngle, -halfFOV)
    val rightGroundPos = calculateGroundIntersection(personDistance, cameraHeight, cameraTiltAngle, halfFOV)

    return abs(rightGroundPos - leftGroundPos)
}

// Calculate where a camera ray hits the ground
private fun calculateGroundIntersection(
    personDistance: Float,
    cameraHeight: Float,
    cameraTiltAngle: Float,
    horizontalAngle: Float
): Float {
    // Convert angles to radians
    val tiltRad = Math.toRadians(cameraTiltAngle.toDouble())
    val hAngleRad = horizontalAngle.toDouble()

    // Calculate the ray direction components
    val rayForward = cos(tiltRad) * cos(hAngleRad)
    val rayDown = sin(tiltRad)
    val raySideways = cos(tiltRad) * sin(hAngleRad)

    // Find where ray hits ground (y = 0, starting from camera height)
    val t = if (abs(rayDown) > 0.001) {
        cameraHeight / rayDown // Parameter along ray where it hits ground
    } else {
        // Ray is parallel to ground, use person distance as approximation
        personDistance.toDouble()
    }

    // Calculate the horizontal ground position
    val groundSideways = raySideways * t

    // Return the lateral distance from camera position
    return groundSideways.toFloat()
}

// Calculate person's actual ground coverage
private fun calculatePersonGroundCoverage(
    boundingBox: Rect,
    imageWidth: Int,
    focalLength: Float,
    sensorWidth: Float,
    distance: Float,
    cameraHeight: Float,
    cameraTiltAngle: Float
): Float {
    // Calculate the horizontal angles for left and right edges of person
    val hFOV = 2 * atan(sensorWidth / (2 * focalLength))
    val pixelsPerRadian = imageWidth / hFOV

    val personLeft = boundingBox.left
    val personRight = boundingBox.right
    val imageCenter = imageWidth / 2

    val leftAngle = (personLeft - imageCenter) / pixelsPerRadian
    val rightAngle = (personRight - imageCenter) / pixelsPerRadian

    // Calculate ground positions for person's left and right edges
    val leftGroundPos = calculateGroundIntersection(distance, cameraHeight, cameraTiltAngle, leftAngle.toFloat())
    val rightGroundPos = calculateGroundIntersection(distance, cameraHeight, cameraTiltAngle, rightAngle.toFloat())

    return abs(rightGroundPos - leftGroundPos)
}



//// Fixed version with corrected unit conversions and 3m walkable distance logic
//
//@OptIn(ExperimentalCamera2Interop::class)
//@SuppressLint("UnsafeOptInUsageError", "RestrictedApi")
//@Composable
//fun LateralCoverageScreen() {
//    // State variables
//    var personDistance by remember { mutableStateOf<Float?>(null) }
//    var totalCameraGroundCoverage by remember { mutableStateOf(0f) }
//    var walkableDistanceLeft by remember { mutableStateOf(0f) }
//    var walkableDistanceRight by remember { mutableStateOf(0f) }
//    var maxWalkableDistance by remember { mutableStateOf(0f) }
//    var covers3Meters by remember { mutableStateOf(false) }
//    var status by remember { mutableStateOf("Initializing...") }
//    var debugInfo by remember { mutableStateOf("") }
//
//    // Camera orientation state
//    var cameraHeight by remember { mutableStateOf(1.5f) }
//    var cameraTiltAngle by remember { mutableStateOf(0f) }
//    var cameraRoll by remember { mutableStateOf(0f) }
//    var orientationReady by remember { mutableStateOf(false) }
//
//    val configuration = LocalConfiguration.current
//    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
//
//    val context = LocalContext.current
//    val lifecycleOwner = LocalLifecycleOwner.current
//    val executor = ContextCompat.getMainExecutor(context)
//
//    var sensorListener by remember { mutableStateOf<SensorEventListener?>(null) }
//
//    // Sensor setup for camera orientation
//    LaunchedEffect(Unit) {
//        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
//        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
//
//        val listener = object : SensorEventListener {
//            override fun onSensorChanged(event: SensorEvent?) {
//                event?.let {
//                    val rotationMatrix = FloatArray(9)
//                    val orientationValues = FloatArray(3)
//
//                    SensorManager.getRotationMatrixFromVector(rotationMatrix, it.values)
//                    SensorManager.getOrientation(rotationMatrix, orientationValues)
//
//                    val pitch = Math.toDegrees(orientationValues[1].toDouble()).toFloat()
//                    val roll = Math.toDegrees(orientationValues[2].toDouble()).toFloat()
//
//                    cameraTiltAngle = -pitch
//                    cameraRoll = roll
//                    orientationReady = true
//                }
//            }
//
//            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
//        }
//
//        sensorListener = listener
//        sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
//    }
//
//    val poseDetector = remember {
//        PoseDetection.getClient(
//            PoseDetectorOptions.Builder()
//                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
//                .build()
//        )
//    }
//
//    val previewView = remember { PreviewView(context) }
//    LaunchedEffect(Unit) {
//        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
//        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
//
//        val camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector)
//        val camera2Info = Camera2CameraInfo.from(camera.cameraInfo)
//
//        // Get focal length in mm
//        val focalLengthMm = camera2Info.getCameraCharacteristic(
//            CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
//        )?.firstOrNull() ?: 4.0f
//
//        // Get sensor size in mm
//        val sensorSize = camera2Info.getCameraCharacteristic(
//            CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE
//        ) ?: SizeF(5.76f, 4.29f) // Default values for typical smartphone
//
//        Log.d("CameraSpecs", "Focal length: ${focalLengthMm}mm, Sensor: ${sensorSize.width}x${sensorSize.height}mm")
//
//        val imageAnalysis = ImageAnalysis.Builder()
//            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//            .build()
//            .also { analyzer ->
//                analyzer.setAnalyzer(executor) { imageProxy ->
//                    if (!isLandscape) {
//                        status = "Please rotate to landscape mode"
//                        debugInfo = "Portrait mode detected"
//                        personDistance = null
//                        covers3Meters = false
//                        imageProxy.close()
//                        return@setAnalyzer
//                    }
//
//                    if (!orientationReady) {
//                        status = "Waiting for orientation sensors..."
//                        debugInfo = "Orientation sensors not ready"
//                        personDistance = null
//                        covers3Meters = false
//                        imageProxy.close()
//                        return@setAnalyzer
//                    }
//
//                    poseDetector.process(
//                        InputImage.fromMediaImage(
//                            imageProxy.image!!,
//                            imageProxy.imageInfo.rotationDegrees
//                        )
//                    ).addOnSuccessListener { pose ->
//                        debugInfo = "Landmarks: ${pose.allPoseLandmarks.size}"
//                        debugInfo += " | Tilt: ${String.format("%.1f", cameraTiltAngle)}°"
//
//                        if (pose.allPoseLandmarks.isNotEmpty()) {
//                            val landmarks = pose.allPoseLandmarks
//                            val xCoords = landmarks.map { it.position.x }
//                            val yCoords = landmarks.map { it.position.y }
//
//                            val left = (xCoords.minOrNull() ?: 0f).toInt()
//                            val right = (xCoords.maxOrNull() ?: 0f).toInt()
//                            val top = (yCoords.minOrNull() ?: 0f).toInt()
//                            val bottom = (yCoords.maxOrNull() ?: 0f).toInt()
//
//                            val boundingBox = if (right > left && bottom > top) {
//                                Rect(left, top, right, bottom)
//                            } else {
//                                val centerX = imageProxy.width / 2
//                                val centerY = imageProxy.height / 2
//                                val estimatedWidth = imageProxy.width / 3
//                                val estimatedHeight = imageProxy.height / 2
//                                Rect(
//                                    centerX - estimatedWidth/2,
//                                    centerY - estimatedHeight/2,
//                                    centerX + estimatedWidth/2,
//                                    centerY + estimatedHeight/2
//                                )
//                            }
//
//                            status = "Person detected! Processing..."
//
//                            // FIXED: Convert focal length from mm to pixels for distance estimation
//                            val focalLengthPixels = (focalLengthMm * imageProxy.width) / sensorSize.width
//
//                            personDistance = estimateDistanceFromBoundingBox(
//                                boundingBox,
//                                imageProxy.height,
//                                focalLengthPixels
//                            )
//
//                            Log.d("Distance", "Calculated distance: ${personDistance}m (focal: ${focalLengthPixels}px)")
//
//                            personDistance?.let { distance ->
//                                // Calculate total camera ground coverage
//                                totalCameraGroundCoverage = calculateTotalGroundCoverage(
//                                    focalLengthMm / 1000f, // Convert mm to meters
//                                    sensorSize.width / 1000f, // Convert mm to meters
//                                    distance,
//                                    cameraHeight,
//                                    cameraTiltAngle
//                                )
//
//                                // Calculate walkable distances from person to camera view edges
//                                val walkableDistances = calculateWalkableDistances(
//                                    boundingBox,
//                                    imageProxy.width,
//                                    focalLengthMm / 1000f, // Convert mm to meters
//                                    sensorSize.width / 1000f, // Convert mm to meters
//                                    distance,
//                                    cameraHeight,
//                                    cameraTiltAngle
//                                )
//
//                                walkableDistanceLeft = walkableDistances.first
//                                walkableDistanceRight = walkableDistances.second
//                                maxWalkableDistance = maxOf(walkableDistanceLeft, walkableDistanceRight)
//
//                                // Check if person can walk 3 meters in either direction
//                                covers3Meters = maxWalkableDistance >= 3.0f
//
//                                debugInfo += " | Max walk: ${"%.2f".format(maxWalkableDistance)}m"
//                                debugInfo += " | 3m: $covers3Meters"
//
//                                Log.d("Coverage", "Total ground: ${totalCameraGroundCoverage}m")
//                                Log.d("Coverage", "Walkable L/R: ${walkableDistanceLeft}m / ${walkableDistanceRight}m")
//                                Log.d("Coverage", "Max walkable: ${maxWalkableDistance}m, Covers 3m: $covers3Meters")
//                            }
//                        } else {
//                            status = "No person detected"
//                            debugInfo = "No pose landmarks"
//                            debugInfo += " | Tilt: ${String.format("%.1f", cameraTiltAngle)}°"
//                            personDistance = null
//                            covers3Meters = false
//                        }
//                    }.addOnFailureListener { e ->
//                        Log.e("PoseDetection", "Detection failed", e)
//                        status = "Detection error: ${e.message}"
//                        personDistance = null
//                        covers3Meters = false
//                    }.addOnCompleteListener {
//                        imageProxy.close()
//                    }
//                }
//            }
//
//        cameraProvider.unbindAll()
//        cameraProvider.bindToLifecycle(
//            lifecycleOwner,
//            cameraSelector,
//            Preview.Builder().build().also {
//                it.setSurfaceProvider(previewView.surfaceProvider)
//            },
//            imageAnalysis
//        )
//    }
//
//    DisposableEffect(Unit) {
//        onDispose {
//            sensorListener?.let { listener ->
//                val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
//                sensorManager.unregisterListener(listener)
//            }
//        }
//    }
//
//    // UI
//    Box(modifier = Modifier.fillMaxSize()) {
//        AndroidView(
//            factory = { previewView },
//            modifier = Modifier.fillMaxSize()
//        )
//
//        Column(
//            modifier = Modifier
//                .align(Alignment.BottomCenter)
//                .padding(16.dp)
//                .background(Color.Black.copy(alpha = 0.7f))
//                .padding(16.dp)
//        ) {
//            Text(text = status, color = Color.White, fontSize = 16.sp)
//
//            Text(
//                text = debugInfo,
//                color = Color.Yellow,
//                fontSize = 14.sp,
//                modifier = Modifier.padding(top = 4.dp)
//            )
//
//            personDistance?.let { distance ->
//                Text(
//                    text = "Distance: ${"%.1f".format(distance)}m",
//                    color = Color.White,
//                    fontSize = 18.sp,
//                    modifier = Modifier.padding(top = 8.dp)
//                )
//
//                Text(
//                    text = "Total Ground Coverage: ${"%.1f".format(totalCameraGroundCoverage)}m",
//                    color = Color.Cyan,
//                    fontSize = 16.sp
//                )
//
//                Text(
//                    text = "Walkable Left: ${"%.1f".format(walkableDistanceLeft)}m",
//                    color = Color.White,
//                    fontSize = 14.sp
//                )
//
//                Text(
//                    text = "Walkable Right: ${"%.1f".format(walkableDistanceRight)}m",
//                    color = Color.White,
//                    fontSize = 14.sp
//                )
//
//                Text(
//                    text = "Max Walkable: ${"%.1f".format(maxWalkableDistance)}m",
//                    color = if (maxWalkableDistance >= 3f) Color.Green else Color.Red,
//                    fontSize = 16.sp,
//                    fontWeight = FontWeight.Bold
//                )
//
//                Text(
//                    text = "📱 Height: ${String.format("%.1f", cameraHeight)}m, Tilt: ${String.format("%.1f", cameraTiltAngle)}°",
//                    color = Color.Gray,
//                    fontSize = 12.sp,
//                    modifier = Modifier.padding(top = 4.dp)
//                )
//            }
//
//            if (maxWalkableDistance > 0f) {
//                Text(
//                    text = if (covers3Meters) {
//                        "✅ Can walk 3m+ in camera view"
//                    } else {
//                        "❌ Less than 3m walkable (${String.format("%.1f", maxWalkableDistance)}m max)"
//                    },
//                    color = if (covers3Meters) Color.Green else Color.Red,
//                    fontSize = 18.sp,
//                    fontWeight = FontWeight.Bold,
//                    modifier = Modifier.padding(top = 8.dp)
//                )
//            }
//        }
//    }
//}
//
//// FIXED: Distance estimation using proper focal length in pixels
//private fun estimateDistanceFromBoundingBox(
//    box: Rect,
//    imageHeight: Int,
//    focalLengthPixels: Float
//): Float {
//    // Assumes average person height = 1.7m
//    val estimatedDistance = (focalLengthPixels * 1.7f) / box.height()
//    return estimatedDistance
//}
//
//// Calculate total camera ground coverage
//private fun calculateTotalGroundCoverage(
//    focalLengthM: Float,
//    sensorWidthM: Float,
//    personDistance: Float,
//    cameraHeight: Float,
//    cameraTiltAngle: Float
//): Float {
//    val hFOV = 2 * atan(sensorWidthM / (2 * focalLengthM))
//    val halfFOV = hFOV / 2
//
//    val leftGroundPos = calculateGroundIntersection(personDistance, cameraHeight, cameraTiltAngle, -halfFOV)
//    val rightGroundPos = calculateGroundIntersection(personDistance, cameraHeight, cameraTiltAngle, halfFOV)
//
//    return abs(rightGroundPos - leftGroundPos)
//}
//
//// FIXED: Calculate walkable distances from person to camera view edges
//private fun calculateWalkableDistances(
//    boundingBox: Rect,
//    imageWidth: Int,
//    focalLengthM: Float,
//    sensorWidthM: Float,
//    distance: Float,
//    cameraHeight: Float,
//    cameraTiltAngle: Float
//): Pair<Float, Float> {
//    val hFOV = 2 * atan(sensorWidthM / (2 * focalLengthM))
//    val halfFOV = hFOV / 2
//
//    // Calculate camera view edges on ground
//    val leftEdgeGroundPos = calculateGroundIntersection(distance, cameraHeight, cameraTiltAngle, -halfFOV)
//    val rightEdgeGroundPos = calculateGroundIntersection(distance, cameraHeight, cameraTiltAngle, halfFOV)
//
//    // Calculate person's position on ground
//    val pixelsPerRadian = imageWidth / hFOV
//    val imageCenter = imageWidth / 2
//    val personCenterX = (boundingBox.left + boundingBox.right) / 2
//    val personAngle = (personCenterX - imageCenter) / pixelsPerRadian
//    val personGroundPos = calculateGroundIntersection(distance, cameraHeight, cameraTiltAngle, personAngle.toFloat())
//
//    // Calculate walkable distances from person to each edge
//    val walkableLeft = abs(personGroundPos - leftEdgeGroundPos)
//    val walkableRight = abs(rightEdgeGroundPos - personGroundPos)
//
//    return Pair(walkableLeft, walkableRight)
//}
//
//// Calculate where a camera ray hits the ground (same as before)
//private fun calculateGroundIntersection(
//    personDistance: Float,
//    cameraHeight: Float,
//    cameraTiltAngle: Float,
//    horizontalAngle: Float
//): Float {
//    val tiltRad = Math.toRadians(cameraTiltAngle.toDouble())
//    val hAngleRad = horizontalAngle.toDouble()
//
//    val rayForward = cos(tiltRad) * cos(hAngleRad)
//    val rayDown = sin(tiltRad)
//    val raySideways = cos(tiltRad) * sin(hAngleRad)
//
//    val t = if (abs(rayDown) > 0.001) {
//        cameraHeight / rayDown
//    } else {
//        personDistance.toDouble()
//    }
//
//    val groundSideways = raySideways * t
//
//    return groundSideways.toFloat()
//}