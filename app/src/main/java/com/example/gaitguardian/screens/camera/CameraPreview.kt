package com.example.gaitguardian.screens.camera

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.annotation.OptIn
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
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalGetImage::class, ExperimentalCamera2Interop::class)
@Composable
fun CameraPreview2(
    controller: LifecycleCameraController,
    modifier: Modifier = Modifier,
    onAnalysisResult: (luminance: Double, isTooDark: Boolean, isTooBright: Boolean, errorMessage: String?) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current


    var cameraTiltAngle by remember { mutableStateOf(0f) }
    var cameraRoll by remember { mutableStateOf(0f) }
    var orientationReady by remember { mutableStateOf(false) }
    var sensorListener by remember { mutableStateOf<SensorEventListener?>(null) }


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