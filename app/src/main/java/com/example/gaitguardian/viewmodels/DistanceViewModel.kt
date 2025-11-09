import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
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

    // Luminosity state
    private val _luminance = MutableStateFlow(0.0)
    val luminance: StateFlow<Double> = _luminance

    private val _luminosityError = MutableStateFlow<String?>(null)
    val luminosityError: StateFlow<String?> = _luminosityError

    private var personHeight = 1.7f
    private var shoulderWidth = 0.425f

    private var focalLength = 0.00415f
    private var sensorWidth = 0.00368f
    private var imageWidth = 1920

    private var distanceStableSince: Long? = null
    private var luminosityStableSince: Long? = null

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalysis: ImageAnalysis? = null

    private val poseDetector by lazy {
        val options = AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
            .build()
        PoseDetection.getClient(options)
    }

    fun setCameraProvider(provider: ProcessCameraProvider) {
        this.cameraProvider = provider
    }

    fun setImageAnalysis(analysis: ImageAnalysis) {
        this.imageAnalysis = analysis
    }

    fun setPersonHeight(height: Float) {
        personHeight = height
        shoulderWidth = height * 0.25f
    }

    fun setCameraParams(focalLengthMeters: Float, sensorWidthMeters: Float, imageWidthPx: Int) {
        focalLength = focalLengthMeters
        sensorWidth = sensorWidthMeters
        imageWidth = imageWidthPx

        Log.d("CameraParams", "f=$focalLength m, sensorWidth=$sensorWidth m, imageWidth=$imageWidth px")
    }

    @OptIn(ExperimentalGetImage::class)
    fun processImage(imageProxy: ImageProxy) {
        when (_cameraPhase.value) {
            CameraPhase.CheckingDistance -> processPoseDetection(imageProxy)
            CameraPhase.CheckingLuminosity -> processLuminosity(imageProxy)
            CameraPhase.VideoRecording -> {
                // Do not need to process images, so just close / free up image
                imageProxy.close()
            }
        }
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun processPoseDetection(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        val rotation = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotation)

        poseDetector.process(inputImage)
            .addOnSuccessListener { pose ->
                handlePose(pose, imageProxy)
            }
            .addOnFailureListener { e ->
                Log.e("PoseError", "Pose detection failed: ${e.message}")
            }
            .addOnCompleteListener { imageProxy.close() }
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

    private fun handlePose(pose: Pose, imageProxy: ImageProxy) {
        // Landmark use shoulder, need to have realworld shoulder width might be more accurate
        // previously i use height so i think got some calculation error because the width of the shoulder not the same
        val left = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val right = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)

        if (left == null || right == null) return

        val rotation = imageProxy.imageInfo.rotationDegrees
        val imageWidthPx = imageProxy.width
        val imageHeightPx = imageProxy.height

        val shoulderPx = when (rotation) {
            0, 180 -> abs(left.position.x - right.position.x)
            90, 270 -> abs(left.position.y - right.position.y)
            else -> abs(left.position.x - right.position.x)
        }

        val frameWidthPx = if (rotation == 90 || rotation == 270) imageHeightPx else imageWidthPx
        if (shoulderPx <= 0f) return

        val D = (focalLength * shoulderWidth * frameWidthPx) / (shoulderPx * sensorWidth)
        val fovX = 2 * atan((sensorWidth / 2f) / focalLength)
        val lateralWidthMeters = 2 * D * tan(fovX / 2f)

        viewModelScope.launch {
            _personDistance.value = D
            _lateralWidth.value = lateralWidthMeters
        }

        checkDistanceStable(lateralWidthMeters)
    }

    private fun checkDistanceStable(lateralWidthMeters: Float) {
        val now = System.currentTimeMillis()
        if (lateralWidthMeters >= 3.0f) {
            if (distanceStableSince == null) distanceStableSince = now
            val elapsed = now - (distanceStableSince ?: now)

            // After 3 seconds of stable 3m distance, move to luminosity check
            if (elapsed >= 3000 && _cameraPhase.value == CameraPhase.CheckingDistance) {
                Log.d("Validation", "Distance check passed, moving to luminosity check")
                _cameraPhase.value = CameraPhase.CheckingLuminosity
            }
        } else {
            distanceStableSince = null
        }
    }

    private fun checkLuminosityStable(errorMessage: String?) {
        val now = System.currentTimeMillis()

        if (errorMessage == null) {
            // No errors - lighting is good
            if (luminosityStableSince == null) luminosityStableSince = now
            val elapsed = now - (luminosityStableSince ?: now)

            // After 2 seconds of good lighting, proceed to recording
            if (elapsed >= 2000 && _cameraPhase.value == CameraPhase.CheckingLuminosity) {
                Log.d("Validation", "Luminosity check passed, ready to record")
                _cameraPhase.value = CameraPhase.VideoRecording

                // Stop image analysis to free resources
                stopImageAnalysis()
            }
        } else {
            // Has errors - reset timer
            luminosityStableSince = null
        }
    }

    private fun stopImageAnalysis() {
        imageAnalysis?.clearAnalyzer()
        Log.d("Resources", "Image analysis stopped - resources freed")
    }

    fun resetToDistanceCheck() { // maybe remove
        _cameraPhase.value = CameraPhase.CheckingDistance
        distanceStableSince = null
        luminosityStableSince = null
        _luminosityError.value = null

        // Restart image analysis
    }
}