package com.example.gaitguardian.screens.camera.mergedsiti

import android.util.Log
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

//@Composable
//fun CameraPreview(
//    controller: LifecycleCameraController,
//    modifier: Modifier = Modifier
//) {
//    val lifecycleOwner = LocalLifecycleOwner.current
//    AndroidView(
//        factory = {
//            PreviewView(it).apply {
//                this.controller = controller
//                controller.bindToLifecycle(lifecycleOwner)
//                scaleType = PreviewView.ScaleType.FILL_CENTER
//            }
//        },
//        modifier = modifier
//    )
//}
@Composable
fun CameraPreview(
    controller: LifecycleCameraController,
    modifier: Modifier = Modifier,
    onAnalysisResult: (luminance: Double, isTooDark: Boolean, isTooBright: Boolean, errorMessage: String?) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    // Set up image analysis
    LaunchedEffect(controller) {
        controller.setImageAnalysisAnalyzer(
            ContextCompat.getMainExecutor(context)
        ) { imageProxy ->
            // Extract luminance data
            val buffer = imageProxy.planes[0].buffer
            val data = ByteArray(buffer.remaining())
            buffer.get(data)
            val luminance = data.map { it.toInt() and 0xFF }.average()

            val isTooDark = luminance < 160
            val isTooBright = luminance > 200

            // Blur detection via luminance variance
            val variance = data.map { (it.toInt() and 0xFF).toDouble() }
                .let { values ->
                    val mean = values.average()
                    values.map { (it - mean) * (it - mean) }.average()
                }

            val isBlurry = variance < 180.0

            val errorMessage = when {
                luminance < 60 -> "Lighting too dark. Please brighten the environment."
                luminance > 200 -> "Lighting too bright — overexposed video will affect detection."
                isBlurry -> "Image is blurry — adjust focus or hold device steady."
                else -> null
            }

            // Update the parent composable
            onAnalysisResult(luminance, isTooDark, isTooBright, errorMessage)

            Log.d("Analyzer", "Luminance: $luminance, Variance: $variance")
            imageProxy.close()
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