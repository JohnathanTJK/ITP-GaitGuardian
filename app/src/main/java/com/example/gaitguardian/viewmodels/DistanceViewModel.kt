package com.example.gaitguardian.viewmodels

import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.tan

class DistanceViewModel : ViewModel() {

    enum class CameraPhase {
        CheckingDistance,      // Check 3m space
        CheckingLuminosity,    // Check lighting conditions
        VideoRecording         // recording
    }

    private val _cameraPhase = MutableStateFlow(CameraPhase.CheckingDistance)
    val cameraPhase: StateFlow<CameraPhase> = _cameraPhase

    private val _personDistance = MutableStateFlow(0f)
    val personDistance: StateFlow<Float> = _personDistance

    private val _lateralWidth = MutableStateFlow(0f)
    val lateralWidth: StateFlow<Float> = _lateralWidth

    private val _luminance = MutableStateFlow(0.0)
    val luminance: StateFlow<Double> = _luminance

    private val _luminosityError = MutableStateFlow<String?>(null)
    val luminosityError: StateFlow<String?> = _luminosityError

    // Using Boundary Box method with ML Kit Object Detection
    private val _boundingBox = MutableStateFlow<Rect?>(null)
    val boundingBox: StateFlow<Rect?> = _boundingBox

    private var personHeight = 1.7f // meters
    private val estimatedPersonCoverage = 0.9f // Bounding box typically captures 85-95% of person

    // Camera parameters
    private var focalLength = 0.00415f       // meters
    private var sensorWidth = 0.00368f       // meters
    private var sensorHeight = 0.00276f      // meters
    private var imageWidth = 1920
    private var imageHeight = 1080

    private var distanceStableSince: Long? = null
    private var luminosityStableSince: Long? = null

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalysis: ImageAnalysis? = null

    private val _imageSize = MutableStateFlow(Triple(0, 0, 0)) // width, height, rotation
    val imageSize: StateFlow<Triple<Int, Int, Int>> = _imageSize

    // To initialize Object Detection
    private val objectDetector by lazy {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
        ObjectDetection.getClient(options)
    }

    fun setCameraProvider(provider: ProcessCameraProvider) {
        this.cameraProvider = provider
    }

    fun setImageAnalysis(analysis: ImageAnalysis) {
        this.imageAnalysis = analysis
    }

    fun setPersonHeight(height: Float) {
        personHeight = height
        Log.d("DistanceViewModel", "Person height set to $personHeight meters")
    }

    fun setCameraParams(
        focalLengthMeters: Float,
        sensorWidthMeters: Float,
        sensorHeightMeters: Float,
        imageWidthPx: Int,
        imageHeightPx: Int
    ) {
        focalLength = focalLengthMeters
        sensorWidth = sensorWidthMeters
        sensorHeight = sensorHeightMeters
        imageWidth = imageWidthPx
        imageHeight = imageHeightPx

        Log.d(
            "DistanceViewModel",
            "Camera params -> f=$focalLength m, sensorWidth=$sensorWidth m, sensorHeight=$sensorHeight m, imageWidth=$imageWidth px, imageHeight=$imageHeight px"
        )
    }

    @OptIn(ExperimentalGetImage::class)
    fun processImage(imageProxy: ImageProxy) {
        when (_cameraPhase.value) {
//            CameraPhase.CheckingDistance -> processPoseDetection(imageProxy)
            CameraPhase.CheckingDistance -> processObjectDetection(imageProxy)
            CameraPhase.CheckingLuminosity -> processLuminosity(imageProxy)
            CameraPhase.VideoRecording -> imageProxy.close()
        }
    }
    //    private fun handlePose(pose: Pose, imageProxy: ImageProxy) {
//        // Landmark use shoulder, need to have realworld shoulder width might be more accurate
//        // previously i use height so i think got some calculation error because the width of the shoulder not the same
//        val left = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
//        val right = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
//
//        if (left == null || right == null) return
//
//        val rotation = imageProxy.imageInfo.rotationDegrees
//        val imageWidthPx = imageProxy.width
//        val imageHeightPx = imageProxy.height
//
//        val shoulderPx = when (rotation) {
//            0, 180 -> abs(left.position.x - right.position.x)
//            90, 270 -> abs(left.position.y - right.position.y)
//            else -> abs(left.position.x - right.position.x)
//        }
//
//        val frameWidthPx = if (rotation == 90 || rotation == 270) imageHeightPx else imageWidthPx
//        if (shoulderPx <= 0f) return
//
//        val D = (focalLength * shoulderWidth * frameWidthPx) / (shoulderPx * sensorWidth)
//        val fovX = 2 * atan((sensorWidth / 2f) / focalLength)
//        val lateralWidthMeters = 2 * D * tan(fovX / 2f)
//
//        viewModelScope.launch {
//            _personDistance.value = D
//            _lateralWidth.value = lateralWidthMeters
//        }
//
//        checkDistanceStable(lateralWidthMeters)
//    }
//
//    private fun checkDistanceStable(lateralWidthMeters: Float) {
//        val now = System.currentTimeMillis()
//        if (lateralWidthMeters >= 3.0f) {
//            if (distanceStableSince == null) distanceStableSince = now
//            val elapsed = now - (distanceStableSince ?: now)
//
//            // After 3 seconds of stable 3m distance, move to luminosity check
//            if (elapsed >= 3000 && _cameraPhase.value == CameraPhase.CheckingDistance) {
//                Log.d("Validation", "Distance check passed, moving to luminosity check")
//                _cameraPhase.value = CameraPhase.CheckingLuminosity
//            }
//        } else {
//            distanceStableSince = null
//        }
//    }
    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    @OptIn(ExperimentalGetImage::class)
    private fun processObjectDetection(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        val imgWidth = mediaImage.width
        val imgHeight = mediaImage.height

        objectDetector.process(image)
            .addOnSuccessListener { results ->
                handleDetectionResults(results, imgWidth, imgHeight)
            }
            .addOnCompleteListener { imageProxy.close() }
            .addOnFailureListener { e ->
                Log.e("DistanceViewModel", "Object detection failed: ${e.message}")
            }
    }

    private fun handleDetectionResults(objects: List<DetectedObject>, imgWidth: Int, imgHeight: Int) {
        if (objects.isEmpty()) return
        // try to retrieve detected 'Person' objects only
        val person = objects.firstOrNull { obj ->
            obj.labels.any { it.text.equals("Person", ignoreCase = true) }
        } ?: objects.first()

        val box = person.boundingBox
        _boundingBox.value = box
        _imageSize.value = Triple(imgWidth, imgHeight, 0)

        Log.d("DistanceViewModel", "Detected bounding box: $box, image size: ${imgWidth}x${imgHeight}")

        computeWalkableSpace(box, imgWidth, imgHeight)
    }

    private fun computeWalkableSpace(box: Rect, imageWidthPx: Int, imageHeightPx: Int) {
        val boundingHeightPx = box.height().toFloat()
        val boundingWidthPx = box.width().toFloat()

        if (boundingHeightPx <= 0f) return

        // Calculate distance from person height
        val actualHeightInBox = personHeight * estimatedPersonCoverage
        val heightOnSensor = (boundingHeightPx / imageHeightPx) * sensorHeight
        val distanceMeters = (actualHeightInBox * focalLength) / heightOnSensor

        // Calculate total lateral width visible at this distance
        val fovX = 2 * atan((sensorWidth / 2f) / focalLength)
        val totalLateralWidth = 2 * distanceMeters * tan(fovX / 2f)

        // Calculate person's actual width in the real world
        val personWidthOnSensor = (boundingWidthPx / imageWidthPx) * sensorWidth
        val personRealWidth = (personWidthOnSensor * distanceMeters) / focalLength

        // Available walking space = total width - person width
        val walkableSpace = totalLateralWidth - personRealWidth

        Log.d("DistanceViewModel", "BoundingBox: ${boundingWidthPx}w x ${boundingHeightPx}h px")
        Log.d("DistanceViewModel", "Distance: $distanceMeters m")
        Log.d("DistanceViewModel", "Total visible width: $totalLateralWidth m")
        Log.d("DistanceViewModel", "Person width: $personRealWidth m")
        Log.d("DistanceViewModel", "Walkable space: $walkableSpace m")

        viewModelScope.launch {
            _personDistance.value = distanceMeters
            _lateralWidth.value = walkableSpace // Now represents actual walkable space
        }

        checkDistanceStable(walkableSpace)
    }

    private fun checkDistanceStable(walkableSpace: Float) {
        val now = System.currentTimeMillis()
        if (walkableSpace >= 3.0f) {
            if (distanceStableSince == null) distanceStableSince = now
            val elapsed = now - (distanceStableSince ?: now)
            // wait for 6s
            if (elapsed >= 6000 && _cameraPhase.value == CameraPhase.CheckingDistance) {
                Log.d("DistanceViewModel", "Distance check passed, moving to luminosity check")
                _cameraPhase.value = CameraPhase.CheckingLuminosity
            }
        } else {
            distanceStableSince = null
        }
    }
    private fun processLuminosity(imageProxy: ImageProxy) {
        try {
            val buffer = imageProxy.planes[0].buffer
            val data = ByteArray(buffer.remaining()).apply { buffer.get(this) }
            val luminance = data.map { it.toInt() and 0xFF }.average()
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

            viewModelScope.launch {
                _luminance.value = luminance
                _luminosityError.value = errorMessage
            }

            checkLuminosityStable(errorMessage)
        } finally {
            imageProxy.close()
        }
    }

    private fun checkLuminosityStable(errorMessage: String?) {
        val now = System.currentTimeMillis()
        if (errorMessage == null) {
            if (luminosityStableSince == null) luminosityStableSince = now
            val elapsed = now - (luminosityStableSince ?: now)
            // wait for 2s
            if (elapsed >= 2000 && _cameraPhase.value == CameraPhase.CheckingLuminosity) {
                Log.d("DistanceViewModel", "Luminosity check passed, ready to record")
                _cameraPhase.value = CameraPhase.VideoRecording
                stopImageAnalysis()
            }
        } else {
            luminosityStableSince = null
        }
    }

    private fun stopImageAnalysis() {
        imageAnalysis?.clearAnalyzer()
        Log.d("DistanceViewModel", "Image analysis stopped - resources freed")
    }

    fun resetToDistanceCheck() {
        _cameraPhase.value = CameraPhase.CheckingDistance
        distanceStableSince = null
        luminosityStableSince = null
        _luminosityError.value = null
    }
}
