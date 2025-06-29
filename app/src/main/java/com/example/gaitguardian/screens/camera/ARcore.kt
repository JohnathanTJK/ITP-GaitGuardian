package com.example.gaitguardian.screens.camera

import android.annotation.SuppressLint
import android.graphics.PointF
import android.media.Image
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.ar.core.CameraIntrinsics
import com.google.ar.core.Config
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetectorOptionsBase.STREAM_MODE
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import kotlinx.coroutines.delay

@SuppressLint("MissingPermission", "RememberReturnType")
@Composable
fun ARCoreDistanceCheckScreen(
    onDistanceMet: ()->Unit
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val orientation = rememberDeviceOrientation()
    var session by remember { mutableStateOf<Session?>(null) }
    var renderingView = remember { SurfaceView(context) }

    val poseDetector = remember {
        PoseDetection.getClient(
            AccuratePoseDetectorOptions.Builder().setDetectorMode(STREAM_MODE).build()
        )
    }

    // Lifecycle-managed initialization & cleanup
    DisposableEffect(Unit) {
        try {
            Session(context).apply {
                configure(Config(this).apply {
                    depthMode = Config.DepthMode.AUTOMATIC
                    planeFindingMode = Config.PlaneFindingMode.DISABLED
                })
                session = this
                resume()
                renderingView.holder.addCallback(object: SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        setCameraTextureName(0) // placeholder, attach GL texture if rendering
                    }
                    override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, h2: Int) { }
                    override fun surfaceDestroyed(h: SurfaceHolder) { }
                })
            }
        } catch (e: Exception) {
            Toast.makeText(context, "AR init failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
        onDispose {
            session?.close()
            session = null
        }
    }

    LaunchedEffect(session) {
        val s = session ?: return@LaunchedEffect
        while (true) {
            val frame = s.update()
            val cam = frame.camera
            if (cam.trackingState != TrackingState.TRACKING) continue

            val image = frame.acquireCameraImage()
            val rotation = 0
            val inputImage = InputImage.fromMediaImage(image, rotation)

            poseDetector.process(inputImage)
                .addOnSuccessListener { pose ->
                    val L = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
                    val R = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
                    if (L != null && R != null) {
                        try {
                            val intrinsics: CameraIntrinsics = frame.camera.textureIntrinsics
//                            val intrinsics = frame.camera.cameraIntrinsics
                            val depth = frame.acquireDepthImage16Bits()

                            val wL = toWorldCoords(L.position, depth, intrinsics)
                            val wR = toWorldCoords(R.position, depth, intrinsics)

                            val adjusted = adjustForOrientation(wL, wR, orientation)
                            val lateral = kotlin.math.abs(adjusted)

                            if (lateral >= 3f) {
                                session?.pause()
                                session?.close()
                                session = null
                                onDistanceMet()
                                return@addOnSuccessListener
                            }
                            depth.close()
                        } catch(_: Exception) { }
                    }
                }
            image.close()
            delay(33)
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { renderingView },
            modifier = Modifier.fillMaxSize()
        )
        Text(
            "Walk apart until 3 m detected",
            Modifier.align(Alignment.Center).background(Color.Black.copy(alpha=0.5f)),
            color=Color.White
        )
    }
}

fun Pose.toWorld() = floatArrayOf(tx(), ty(), tz())

fun toWorldCoords(p: PointF, d: Image, intr: CameraIntrinsics): FloatArray {
    val (fx, fy) = intr.focalLength
    val (cx, cy) = intr.principalPoint
    val px = p.x.toInt(); val py = p.y.toInt()
    val bytes = d.planes[0].buffer
    val idx = py * d.planes[0].rowStride + px * d.planes[0].pixelStride
    val mm = ((bytes.get(idx+1).toInt() shl 8) or (bytes.get(idx).toInt() and 0xFF))
    val zz = mm / 1000f
    val xx = (px - cx) * zz / fx
    val yy = (py - cy) * zz / fy
    return floatArrayOf(xx, yy, zz)
}

fun adjustForOrientation(A: FloatArray, B: FloatArray, o: DeviceOrientation): Float =
    when(o) {
        DeviceOrientation.PORTRAIT -> B[0] - A[0]
        DeviceOrientation.LANDSCAPE_LEFT -> (B[1]-A[1])
        DeviceOrientation.LANDSCAPE_RIGHT -> (A[1]-B[1])
        DeviceOrientation.PORTRAIT_UPSIDE_DOWN -> A[0] - B[0]
    }