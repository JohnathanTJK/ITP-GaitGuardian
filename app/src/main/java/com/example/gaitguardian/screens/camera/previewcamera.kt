package com.example.gaitguardian.screens.camera

import android.annotation.SuppressLint
import android.content.Context
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
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin
//
////@OptIn(ExperimentalGetImage::class,ExperimentalCamera2Interop::class)
////@Composable
////fun CameraPreview2(
////    controller: LifecycleCameraController,
////    modifier: Modifier = Modifier,
////    onAnalysisResult: (luminance: Double, isTooDark: Boolean, isTooBright: Boolean, errorMessage: String?) -> Unit,
////    onDistanceDetectionResult: (personDistance: Float, personHorizontalCoverage: Float, lateralCoverage: Float, cover3Meters: Boolean) -> Unit
////) {
////    val lifecycleOwner = LocalLifecycleOwner.current
////    val context = LocalContext.current
////    var personDistance by remember { mutableStateOf<Float?>(null) }
////    var lateralCoverage by remember { mutableStateOf(0f) }
////    var personHorizontalCoverage by remember { mutableStateOf(0f) }
////    var covers3Meters by remember { mutableStateOf(false) }
////    var status by remember { mutableStateOf("Initializing...") }
////    var debugInfo by remember { mutableStateOf("") }
////    var sensorListener by remember { mutableStateOf<SensorEventListener?>(null) }
////
////    // Camera orientation state
////    var cameraHeight by remember { mutableStateOf(1.5f) } // Default assumption: 1.5m height
////    var cameraTiltAngle by remember { mutableStateOf(0f) } // Degrees from horizontal
////    var cameraRoll by remember { mutableStateOf(0f) }
////    var orientationReady by remember { mutableStateOf(false) }
////
////    LaunchedEffect(Unit) {
////        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
////        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
////
////        val listener = object : SensorEventListener {
////            override fun onSensorChanged(event: SensorEvent?) {
////                event?.let {
////                    val rotationMatrix = FloatArray(9)
////                    val orientationValues = FloatArray(3)
////
////                    SensorManager.getRotationMatrixFromVector(rotationMatrix, it.values)
////                    SensorManager.getOrientation(rotationMatrix, orientationValues)
////
////                    // Convert to degrees and adjust for camera orientation
////                    val pitch = Math.toDegrees(orientationValues[1].toDouble()).toFloat()
////                    val roll = Math.toDegrees(orientationValues[2].toDouble()).toFloat()
////
////                    // When phone is held normally (portrait) with camera pointing out:
////                    // - Positive pitch = camera tilted up
////                    // - Negative pitch = camera tilted down
////                    cameraTiltAngle = -pitch // Invert because we want angle from horizontal
////                    cameraRoll = roll
////                    orientationReady = true
////                }
////            }
////
////            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
////        }
////
////        sensorListener = listener
////        sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
////    }
////    // ML Kit Pose Detector
////    val poseDetector = remember {
////        PoseDetection.getClient(
////            PoseDetectorOptions.Builder()
////                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
////                .build()
////        )
////    }
////
////    val camera2Info = controller.cameraInfo?.let { Camera2CameraInfo.from(it) }
////    val focalLength = camera2Info?.getCameraCharacteristic(
////        CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
////    )?.firstOrNull() ?: 1.0f
////    val sensorSize = camera2Info?.getCameraCharacteristic(
////        CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE
////    ) ?: SizeF(0f, 0f)
////
////    // Set up image analysis
////    LaunchedEffect(controller) {
////        controller.setImageAnalysisAnalyzer(
////            ContextCompat.getMainExecutor(context)
////        ) { imageProxy ->
////            // Extract luminance data
////            val buffer = imageProxy.planes[0].buffer
////            val data = ByteArray(buffer.remaining())
////            buffer.get(data)
////            val luminance = data.map { it.toInt() and 0xFF }.average()
////
////            val isTooDark = luminance < 160
////            val isTooBright = luminance > 200
////
////            // Blur detection via luminance variance
////            val variance = data.map { (it.toInt() and 0xFF).toDouble() }
////                .let { values ->
////                    val mean = values.average()
////                    values.map { (it - mean) * (it - mean) }.average()
////                }
////
////            val isBlurry = variance < 180.0
////
////            val errorMessage = when {
////                luminance < 60 -> "Lighting too dark. Please brighten the environment."
////                luminance > 200 -> "Lighting too bright — overexposed video will affect detection."
////                isBlurry -> "Image is blurry — adjust focus or hold device steady."
////                else -> null
////            }
////
////            // Update the parent composable
////            onAnalysisResult(luminance, isTooDark, isTooBright, errorMessage)
////
//////            Log.d("Analyzer", "Luminance: $luminance, Variance: $variance")
//////            imageProxy.close()
//////        }
//////        controller.setImageAnalysisAnalyzer(
//////            ContextCompat.getMainExecutor(context)
//////        ) { imageProxy ->
////            poseDetector.process(
////                InputImage.fromMediaImage(
////                    imageProxy.image!!,
////                    imageProxy.imageInfo.rotationDegrees
////                )
////            ).addOnSuccessListener { pose ->
//////                debugInfo = "Landmarks found: ${pose.allPoseLandmarks.size}"
//////                debugInfo += " | Tilt: ${String.format("%.1f", cameraTiltAngle)}°"
//////                Log.d("PoseDetection", "Pose detection success, landmarks: ${pose.allPoseLandmarks.size}")
////
////                if (pose.allPoseLandmarks.isNotEmpty()) {
////                    Log.d("PoseDetection", "Processing pose landmarks...")
////                    // Calculate bounding box from pose landmarks
////                    val landmarks = pose.allPoseLandmarks
////                    val xCoords = landmarks.map { it.position.x }
////                    val yCoords = landmarks.map { it.position.y }
////
////                    val left = (xCoords.minOrNull() ?: 0f).toInt()
////                    val right = (xCoords.maxOrNull() ?: 0f).toInt()
////                    val top = (yCoords.minOrNull() ?: 0f).toInt()
////                    val bottom = (yCoords.maxOrNull() ?: 0f).toInt()
////
////                    // Ensure valid bounding box
////                    val boundingBox = if (right > left && bottom > top) {
////                        Rect(left, top, right, bottom)
////                    } else {
////                        // Fallback: use image center with estimated size
////                        val centerX = imageProxy.width / 2
////                        val centerY = imageProxy.height / 2
////                        val estimatedWidth = imageProxy.width / 3
////                        val estimatedHeight = imageProxy.height / 2
////                        Rect(
////                            centerX - estimatedWidth/2,
////                            centerY - estimatedHeight/2,
////                            centerX + estimatedWidth/2,
////                            centerY + estimatedHeight/2
////                        )
////                    }
////
//////                    debugInfo += " | Box: ${boundingBox.width()}x${boundingBox.height()}"
//////                    status = "Person detected! Processing..."
//////                    Log.d("BoundingBox", "Bounding box: ${boundingBox.width()}x${boundingBox.height()}")
////
////                    // Use bounding box estimation for distance
////                    Log.d("DistanceEstimation", "Estimating distance from bounding box...")
////                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
////
////                    val correctedBoxHeight = when (rotationDegrees) {
////                        90, 270 -> boundingBox.width() // portrait-locked but physically landscape
////                        else -> boundingBox.height()
////                    }
////
////                    val correctedImageWidth = when (rotationDegrees) {
////                        90, 270 -> imageProxy.height
////                        else -> imageProxy.width
////                    }
//////
//////                    personDistance = estimateDistanceFromBoundingBox(
//////                        correctedBoxHeight,
//////                        focalLength
//////                    )
////                    personDistance = estimateDistanceFromBoundingBox(
////                        boundingBox,
////                        imageProxy.height,
////                        focalLength
////                    ).also { estimatedDistance ->
////                        status = "Ground distance calculated"
////                        Log.d("DistanceEstimation", "Estimated distance: $estimatedDistance")
////                    }
////
////                    Log.d("DistanceResult", "Final distance: $personDistance")
////
////                    // Calculate coverage if distance available
////                    personDistance?.let { distance ->
////                        Log.d("CoverageCalculation", "Starting coverage calculation with distance: $distance")
////                        Log.d("CoverageCalculation", "Camera params - focal: $focalLength, sensor: ${sensorSize.width}")
////                        Log.d("CoverageCalculation", "Tilt: $cameraTiltAngle°")
////
////                        // Calculate actual ground coverage using orientation
////                        lateralCoverage = calculateGroundCoverage(
////                            focalLength,
////                            sensorSize.width,
////                            distance,
////                            cameraHeight,
////                            cameraTiltAngle
////                        )
////                        Log.d("CoverageCalculation", "Ground lateral coverage: $lateralCoverage")
////
////                        // Calculate person's actual ground coverage
////                        personHorizontalCoverage = calculatePersonGroundCoverage(
////                            boundingBox,
////                            imageProxy.width,
////                            focalLength,
////                            sensorSize.width,
////                            distance,
////                            cameraHeight,
////                            cameraTiltAngle
////                        )
////                        Log.d("CoverageCalculation", "Person ground coverage: $personHorizontalCoverage")
////
////                        // Check if person covers 3 meters horizontally
////                        covers3Meters = personHorizontalCoverage >= 3.0f
////
////                        debugInfo += " | Person: ${"%.2f".format(personHorizontalCoverage)}m | 3m: $covers3Meters"
////                        Log.d("LateralCoverage", "Person width: ${personHorizontalCoverage}m, Covers 3m: $covers3Meters")
////                    } ?: run {
////                        Log.d("CoverageCalculation", "No distance available, skipping coverage calculation")
////                    }
////                    onDistanceDetectionResult(personDistance!!,personHorizontalCoverage,lateralCoverage,covers3Meters)
////
////                } else {
////                    status = "No person detected"
////                    debugInfo = "No pose landmarks detected"
////                    debugInfo += " | Tilt: ${String.format("%.1f", cameraTiltAngle)}°"
////                    Log.d("PoseDetection", "No pose landmarks detected")
////                    personDistance = null
////                    personHorizontalCoverage = 0f
////                    covers3Meters = false
////                }
////            }.addOnFailureListener { e ->
////                Log.e("PoseDetection", "Detection failed", e)
////                status = "Pose detection error: ${e.message}"
////                personDistance = null
////                personHorizontalCoverage = 0f
////                covers3Meters = false
////            }.addOnCompleteListener {
////                imageProxy.close()
////            }
////        }
////    }
//
//
@SuppressLint("DefaultLocale")
@OptIn(ExperimentalGetImage::class, ExperimentalCamera2Interop::class)
@Composable
fun CameraPreview2(
    controller: LifecycleCameraController,
    modifier: Modifier = Modifier,
    onAnalysisResult: (luminance: Double, isTooDark: Boolean, isTooBright: Boolean, errorMessage: String?) -> Unit,
//    onDistanceDetectionResult: (personDistance: Float, personHorizontalCoverage: Float, lateralCoverage: Float, cover3Meters: Boolean, status: String, debugInfo: String, cameraTiltAngle: Float) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    var personDistance by remember { mutableStateOf<Float?>(null) }
    var lateralCoverage by remember { mutableStateOf(0f) }
    var personHorizontalCoverage by remember { mutableStateOf(0f) }
    var covers3Meters by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Initializing...") }
    var debugInfo by remember { mutableStateOf("") }

    var cameraHeight by remember { mutableStateOf(1.5f) }
    var cameraTiltAngle by remember { mutableStateOf(0f) }
    var cameraRoll by remember { mutableStateOf(0f) }
    var orientationReady by remember { mutableStateOf(false) }
    var sensorListener by remember { mutableStateOf<SensorEventListener?>(null) }

    var debounceJob by remember { mutableStateOf<Job?>(null) }

    // Orientation sensor
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

                    val pitch = Math.toDegrees(orientationValues[1].toDouble()).toFloat()
                    val roll = Math.toDegrees(orientationValues[2].toDouble()).toFloat()

                    cameraTiltAngle = -pitch
                    cameraRoll = roll
                    orientationReady = true
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorListener = listener
        sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
    }

    val poseDetector = remember {
        PoseDetection.getClient(
            PoseDetectorOptions.Builder().setDetectorMode(PoseDetectorOptions.STREAM_MODE).build()
        )
    }

    val camera2Info = controller.cameraInfo?.let { Camera2CameraInfo.from(it) }
    val focalLength = camera2Info?.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.firstOrNull() ?: 3.6f
    val sensorSize = camera2Info?.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE) ?: SizeF(4.8f, 3.6f)

    // Analyzer
    LaunchedEffect(controller) {
        controller.setImageAnalysisAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
            val buffer = imageProxy.planes[0].buffer
            val data = ByteArray(buffer.remaining()).apply { buffer.get(this) }
            val luminance = data.map { it.toInt() and 0xFF }.average()

            val isTooDark = luminance < 160
            val isTooBright = luminance > 200
            val variance = data.map { (it.toInt() and 0xFF).toDouble() }.let {
                val mean = it.average()
                it.map { v -> (v - mean) * (v - mean) }.average()
            }
            val isBlurry = variance < 180.0
            val errorMessage = when {
                luminance < 60 -> "Lighting too dark. Please brighten the environment."
                luminance > 200 -> "Lighting too bright — overexposed video will affect detection."
                isBlurry -> "Image is blurry — adjust focus or hold device steady."
                else -> null
            }

            onAnalysisResult(luminance, isTooDark, isTooBright, errorMessage)

            if (!orientationReady) {
                imageProxy.close()
                return@setImageAnalysisAnalyzer
            }

//            poseDetector.process(
//                InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)
//            ).addOnSuccessListener { pose ->
//                debugInfo = "Landmarks found: ${pose.allPoseLandmarks.size}"
//                debugInfo += " | Tilt: ${String.format("%.1f", cameraTiltAngle)}°"
//                Log.d("PoseDetection", "Pose detection success, landmarks: ${pose.allPoseLandmarks.size}")
//
//                if (pose.allPoseLandmarks.isNotEmpty()) {
//                    val landmarks = pose.allPoseLandmarks
//                    val xCoords = landmarks.map { it.position.x }
//                    val yCoords = landmarks.map { it.position.y }
//
//                    val left = (xCoords.minOrNull() ?: 0f).toInt()
//                    val right = (xCoords.maxOrNull() ?: 0f).toInt()
//                    val top = (yCoords.minOrNull() ?: 0f).toInt()
//                    val bottom = (yCoords.maxOrNull() ?: 0f).toInt()
//
//                    val boundingBox = if (right > left && bottom > top) {
//                        Rect(left, top, right, bottom)
//                    } else {
//                        val centerX = imageProxy.width / 2
//                        val centerY = imageProxy.height / 2
//                        Rect(centerX - 100, centerY - 200, centerX + 100, centerY + 200)
//                    }
//
//                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
//                    val correctedBoxHeight = if (rotationDegrees == 90 || rotationDegrees == 270) {
//                        boundingBox.width()
//                    } else boundingBox.height()
//
//                    val correctedImageWidth = if (rotationDegrees == 90 || rotationDegrees == 270) {
//                        imageProxy.height
//                    } else imageProxy.width
//
//                    Log.d("RotationFix", "rotationDegrees=$rotationDegrees, correctedBoxHeight=$correctedBoxHeight, correctedImageWidth=$correctedImageWidth")
//
//                    personDistance = estimateDistanceFromBoundingBox(
//                        correctedBoxHeight,
//                        focalLength
//                    ).also { estimatedDistance ->
//                        status ="Ground distance calculated"
//                        Log.d("DistanceEstimation", "Estimated distance: $estimatedDistance")
//                    }
//
//                    personDistance?.let { distance ->
//                        lateralCoverage = calculateGroundCoverage(
//                            focalLength,
//                            sensorSize.width,
//                            distance,
//                            cameraHeight,
//                            cameraTiltAngle
//                        )
//
//                        personHorizontalCoverage = calculatePersonGroundCoverage(
//                            boundingBox,
//                            correctedImageWidth,
//                            focalLength,
//                            sensorSize.width,
//                            distance,
//                            cameraHeight,
//                            cameraTiltAngle
//                        )
//
//                        covers3Meters = personHorizontalCoverage >= 3.0f
//                        debugInfo += " | Person: ${"%.2f".format(personHorizontalCoverage)}m | 3m: $covers3Meters"
//
//                        onDistanceDetectionResult(
//                            distance,
//                            personHorizontalCoverage,
//                            lateralCoverage,
//                            covers3Meters,
//                            status,
//                            debugInfo,
//                            cameraTiltAngle
//                        )
//                    }
//                }
//
//            }.addOnFailureListener { e ->
//                Log.e("PoseDetection", "Detection failed", e)
//                personDistance = null
//                personHorizontalCoverage = 0f
//                covers3Meters = false
//            }.addOnCompleteListener {
//                imageProxy.close()
//            }
        }
    }

    AndroidView(
        factory = {
            PreviewView(it).apply {
                this.controller = controller
                controller.bindToLifecycle(lifecycleOwner)
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        modifier = modifier
    )
}



// Bounding box distance estimation
//private fun estimateDistanceFromBoundingBox(
//    box: Rect,
//    imageHeight: Int,
//    focalLengthPixels: Float
//): Float {
//    // Assumes average person height = 1.7m
//    return (focalLengthPixels * 1.7f) / box.height()
//}
private fun estimateDistanceFromBoundingBox(
    correctedBoxHeight: Int,
    focalLengthPixels: Float
): Float {
    return (focalLengthPixels * 1.7f) / correctedBoxHeight
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