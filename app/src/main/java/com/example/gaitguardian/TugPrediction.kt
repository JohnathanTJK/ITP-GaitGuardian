package com.example.gaitguardian

import android.content.Context
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.io.IOException
import java.nio.FloatBuffer
import kotlin.math.roundToInt

/**
 * CLEAN TugPrediction - Only does ONNX inference, smoothing, and duration analysis
 * Feature extraction is handled by FeatureExtraction
 */
class TugPrediction(private val context: Context) {

    companion object {
        private const val TAG = "TugPrediction"
        private const val DEFAULT_FPS = 30f
    }

    // ===== Data Types =====

    @Serializable
    data class PhaseAnalysis(
        val phase: String,
        val start_frame: Int,
        val end_frame: Int,
        val duration_sec: Float,
        val frame_count: Int
    )

    @Serializable
    data class PredictionResult(
        val filename: String,
        val total_frames: Int,
        val total_duration_sec: Float,
        val phase_durations: Map<String, Float>,
        val phase_analysis: List<PhaseAnalysis>,
        val severity: String = "Unknown",
        val success: Boolean,
        val error_message: String? = null
    )

    data class ModelData(
        val ortSession: OrtSession,
        val labelEncoder: List<String>,
        val labelNames: List<String>
    )

    // ===== State =====

    private var modelData: ModelData? = null
    private var ortEnvironment: OrtEnvironment? = null
    private var debugCounter = 0

    // ===== Initialization =====

    suspend fun initializeModel(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔧 Initializing ONNX model and metadata...")
            ortEnvironment = OrtEnvironment.getEnvironment()
            // Load ONNX model
            val modelBytes = context.assets.open("xgboost_tug_model.onnx").readBytes()
            val ortSession = ortEnvironment!!.createSession(modelBytes)
            // Load metadata
            val metadataJson = context.assets.open("onnx_metadata.json").bufferedReader().readText()
            Log.d(TAG, "Metadata JSON content: ${metadataJson.take(500)}...")
            val metadata = Json.parseToJsonElement(metadataJson).jsonObject
            Log.d(TAG, "Parsed metadata keys: ${metadata.keys}")
            val classesElement = metadata["output_classes"]
            if (classesElement == null) {
                Log.e(TAG, "output_classes not found in metadata")
                return@withContext false
            }
            val labelEncoder = Json.decodeFromJsonElement<List<String>>(classesElement)
            val labelNames = labelEncoder
            modelData = ModelData(ortSession, labelEncoder, labelNames)
            Log.d(TAG, "Model and metadata loaded successfully")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize model", e)
            return@withContext false
        }
    }

    // ===== Main Processing Functions =====

    /**
     * Main processing function for pose landmarks using FeatureExtraction
     */
    suspend fun processPoseLandmarks(
        landmarksList: List<List<NormalizedLandmark>>,
        fps: Float = DEFAULT_FPS,
        progressCallback: FrameProgressCallback? = null
    ): PredictionResult = withContext(Dispatchers.Default) {
        
        val model = modelData ?: throw IllegalStateException("❌ Model not initialized")
        
        Log.i(TAG, "========================================")
        Log.i(TAG, "🧠 TUG PREDICTION STARTING")
        Log.i(TAG, "Total frames: ${landmarksList.size}")
        Log.i(TAG, "FPS: $fps")
        Log.i(TAG, "Timestamp: ${System.currentTimeMillis()}")
        Log.i(TAG, "========================================")
        Log.i(TAG, "Processing ${landmarksList.size} pose landmark frames with fixed Python port")
        
        // Start overall timing
        val overallStartTime = System.currentTimeMillis()
        
        try {
            // Extract features using the fixed Python port - now returns frame-by-frame features
            val featureExtractionStartTime = System.currentTimeMillis()
            val featureExtractor = FeatureExtraction()
            val frameFeatures = featureExtractor.extractTugFeatures(landmarksList, fps)
            val featureExtractionEndTime = System.currentTimeMillis()
            
            Log.i(TAG, "Extracted ${frameFeatures.size} frame feature vectors from ${landmarksList.size} frames")
            
            // Convert frame-by-frame features to model input format and run predictions
            val predictionStartTime = System.currentTimeMillis()
            val predictions = mutableListOf<String>()
            
            // Process each frame's features for real-time prediction
            frameFeatures.forEachIndexed { frameIndex, featureMap ->
                // Convert feature map to ordered FloatArray for model input
                val featureVector = convertFeatureMapToArray(featureMap)
                
                // Check if this frame has no movement detected
                val noMovementDetected = featureMap["no_movement_detected"] == 1.0f
                
                val prediction = predictWithModel(featureVector, model)
                
                // CRITICAL FIX: Override prediction when no movement detected
                // The model was trained on data with pose jitter, so it can predict movement
                // phases even when all velocities are zero. Force Sit-To-Stand in this case.
                val overriddenPrediction = if (noMovementDetected) {
                    0  // Force Sit-To-Stand (class 0)
                } else {
                    prediction
                }
                
                val label = model.labelEncoder.getOrNull(overriddenPrediction) ?: "Unknown"
                predictions.add(label)
                
                // Report progress every 10 frames for performance
                if (frameIndex % 10 == 0) {
                    progressCallback?.onProgress(frameIndex, frameFeatures.size, "Analyzing movement")
                }
                
                if (frameIndex % 50 == 0) { // Log every 50th frame
                    Log.d(TAG, "Frame $frameIndex: features=${featureVector.size}, prediction=$prediction, overridden=$overriddenPrediction, label=$label, noMovement=$noMovementDetected")
                }
            }
            val predictionEndTime = System.currentTimeMillis()
            
            // Post-process predictions
            val postProcessingStartTime = System.currentTimeMillis()
            Log.i(TAG, "🔧 Post-processing ${predictions.size} predictions...")
            val smoothed = smoothMajority(predictions, window = 5)
            val corrected = correctSequence(smoothed)
            val postProcessingEndTime = System.currentTimeMillis()
            
            // Analyze phases
            val phaseAnalysis = analyzePhaseDurations(corrected, fps)
            val totalDuration = phaseAnalysis.sumOf { it.duration_sec.toDouble() }.toFloat()
            val phaseDurations = phaseAnalysis.associate { it.phase to it.duration_sec }
            
            val overallEndTime = System.currentTimeMillis()
            
            // Calculate timing metrics
            val featureExtractionDuration = (featureExtractionEndTime - featureExtractionStartTime) / 1000.0
            val predictionDuration = (predictionEndTime - predictionStartTime) / 1000.0
            val postProcessingDuration = (postProcessingEndTime - postProcessingStartTime) / 1000.0
            val totalProcessingTime = (overallEndTime - overallStartTime) / 1000.0
            
            // Log results with timing
            Log.i(TAG, "Raw predictions: ${predictions.groupingBy { it }.eachCount()}")
            Log.i(TAG, "Final sequence: ${corrected.groupingBy { it }.eachCount()}")
            Log.i(TAG, "Phase durations:")
            phaseAnalysis.forEach { phase ->
                Log.i(TAG, "   ${phase.phase}: ${phase.duration_sec}s (${phase.frame_count} frames)")
            }
            Log.i(TAG, "Total TUG duration: ${totalDuration}s")
            
            // Log unique prediction fingerprint
            val predictionFingerprint = predictions.take(10).joinToString(",") + "_" + predictions.takeLast(10).joinToString(",")
            Log.e(TAG, "🔍 PREDICTION UNIQUENESS - First 10 + Last 10 predictions: $predictionFingerprint")
            
            // Log detailed timing breakdown
            Log.e(TAG, "⏱️ ===== PROCESSING TIME BREAKDOWN =====")
            Log.e(TAG, "⏱️ Feature Extraction: ${String.format("%.2f", featureExtractionDuration)} s (${String.format("%.1f", featureExtractionDuration/totalProcessingTime*100)}%)")
            Log.e(TAG, "⏱️ ONNX Prediction: ${String.format("%.2f", predictionDuration)} s (${String.format("%.1f", predictionDuration/totalProcessingTime*100)}%)")
            Log.e(TAG, "⏱️ Post-Processing: ${String.format("%.2f", postProcessingDuration)} s (${String.format("%.1f", postProcessingDuration/totalProcessingTime*100)}%)")
            Log.e(TAG, "⏱️ TOTAL PROCESSING TIME: ${String.format("%.2f", totalProcessingTime)} s")
            Log.e(TAG, "⏱️ =====================================")
            
            // Calculate severity using SeverityClassification
            val tugMetrics = createTugMetricsMap(phaseDurations, totalDuration.toDouble())
            val severity = SeverityClassification.classifyGaitSeverity(tugMetrics)
            Log.i(TAG, "Calculated severity: $severity")
            
            PredictionResult(
                filename = "pose_landmarks",
                total_frames = landmarksList.size,
                total_duration_sec = totalDuration,
                phase_durations = phaseDurations,
                phase_analysis = phaseAnalysis,
                severity = severity,
                success = true
            )
            
        } catch (e: Exception) {
            val errorMessage = when (e) {
                is IndexOutOfBoundsException -> "No body detected in video frames. Please ensure you are in the frame."
                is IOException -> "File read/write error during analysis"
                else -> "Unexpected error: ${e.localizedMessage ?: "Unknown"}"
            }
            PredictionResult(
                filename = "pose_landmarks",
                total_frames = landmarksList.size,
                total_duration_sec = 0f,
                phase_durations = emptyMap(),
                phase_analysis = emptyList(),
                severity = "Unknown",
                success = false,
//                error_message = "Processing failed: ${e.message}"
                error_message = errorMessage
            )
        }
    }

    /**
     * Process pre-computed features from Python (bypasses internal feature extraction)
     */
    suspend fun processFeatures(
        features: List<FloatArray>,
        featureNames: List<String>,
        fps: Float
    ): PredictionResult = withContext(Dispatchers.Default) {
        
        val model = modelData ?: throw IllegalStateException("Model not initialized")
        
        Log.i(TAG, "Processing ${features.size} pre-computed feature vectors")
        Log.i(TAG, "Feature names: ${featureNames.size} (expected: 111)")
        
        try {
            // Validate feature count
            if (featureNames.size != 111) {
                Log.w(TAG, "Feature count mismatch: got ${featureNames.size}, expected 111")
            }
            
            // Run predictions for each feature vector
            val predictions = features.mapIndexed { index, featureVector ->
                if (featureVector.size != 111) {
                    Log.w(TAG, "Frame $index has ${featureVector.size} features, expected 111")
                }
                
                val prediction = predictWithModel(featureVector, model)
                val label = model.labelEncoder.getOrNull(prediction) ?: "Unknown"
                
                if (index < 5) { // Debug first few predictions
                    Log.d(TAG, "Frame $index: prediction=$prediction, label=$label")
                    Log.d(TAG, "Features: [${featureVector.take(5).joinToString(", ") { "%.3f".format(it) }}...]")
                }
                
                label
            }
            
            // Post-process predictions
            Log.i(TAG, "Post-processing ${predictions.size} predictions...")
            val smoothed = smoothMajority(predictions, window = 5)
            val corrected = correctSequence(smoothed)
            
            // Analyze phases
            val phaseAnalysis = analyzePhaseDurations(corrected, fps)
            val totalDuration = phaseAnalysis.sumOf { it.duration_sec.toDouble() }.toFloat()
            val phaseDurations = phaseAnalysis.associate { it.phase to it.duration_sec }
            
            // Log results
            Log.i(TAG, "Raw predictions: ${predictions.groupingBy { it }.eachCount()}")
            Log.i(TAG, "Final sequence: ${corrected.groupingBy { it }.eachCount()}")
            Log.i(TAG, "⏱Phase durations:")
            phaseAnalysis.forEach { phase ->
                Log.i(TAG, "   ${phase.phase}: ${phase.duration_sec}s (${phase.frame_count} frames)")
            }
            Log.i(TAG, "Total TUG duration: ${totalDuration}s")
            
            // Calculate severity using SeverityClassification
            val tugMetrics = createTugMetricsMap(phaseDurations, totalDuration.toDouble())
            val severity = SeverityClassification.classifyGaitSeverity(tugMetrics)
            Log.i(TAG, "Calculated severity: $severity")
            
            PredictionResult(
                filename = "python_features",
                total_frames = features.size,
                total_duration_sec = totalDuration,
                phase_durations = phaseDurations,
                phase_analysis = phaseAnalysis,
                severity = severity,
                success = true
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Feature processing failed", e)
            PredictionResult(
                filename = "python_features",
                total_frames = features.size,
                total_duration_sec = 0f,
                phase_durations = emptyMap(),
                phase_analysis = emptyList(),
                severity = "Unknown",
                success = false,
                error_message = "Feature processing failed: ${e.message}"
            )
        }
    }


    // ===== Feature Conversion =====
    
    private fun convertFeatureMapToArray(featureMap: Map<String, Float>): FloatArray {
        // Define the expected feature order to match the ONNX model training data
        // This should match the exact order used during Python model training
        val expectedFeatures = listOf(
            // Clinical gait parameters
            "hip_height", "hip_y_velocity", "hip_x_velocity", "hip_vertical_acceleration", "hip_horizontal_acceleration",
            "com_x", "com_y", "com_x_velocity", "com_y_velocity", "com_acceleration",
            
            // Joint kinematics
            "left_knee_angle", "right_knee_angle", "left_hip_angle", "right_hip_angle", 
            "left_ankle_angle", "right_ankle_angle", "torso_angle", "torso_angle_velocity",
            
            // Joint coordination
            "knee_coordination", "hip_coordination", "ankle_coordination", "knee_extension_power", 
            "hip_extension_power", "ankle_power", "left_knee_velocity", "right_knee_velocity",
            
            // Phase-specific features
            "vertical_momentum", "sit_to_stand_power", "trunk_flexion_velocity", "forward_momentum", "forward_acceleration",
            
            // Gait analysis
            "ankle_distance", "heel_distance", "toe_distance", "base_of_support_width", "base_of_support_length",
            "lheel_y_velocity", "rheel_y_velocity", "lheel_x_velocity", "rheel_x_velocity",
            "step_asymmetry", "stride_asymmetry", "time_since_last_step", "step_frequency", 
            "step_timing_variability", "stride_length_variation",
            
            // Turning kinematics
            "shoulder_angle", "shoulder_rotation_velocity", "hip_rotation", "hip_rotation_velocity",
            "axial_dissociation", "axial_dissociation_velocity", "head_yaw", "head_yaw_velocity",
            "torso_twist", "torso_twist_velocity", "direction_change_magnitude", "turn_preparation_score",
            
            // Balance and stability
            "mediolateral_sway", "anteroposterior_sway", "weight_shift_x", "weight_shift_y",
            "stability_margin", "body_sway", "postural_control", "movement_complexity",
            
            // Energy and power
            "kinetic_energy", "potential_energy", "total_mechanical_energy", "rotational_energy",
            "concentric_power", "eccentric_power", "total_body_momentum",
            
            // Temporal context
            "task_progression", "progression_velocity", "sit_to_stand_likelihood", "walk_from_likelihood",
            "turn_first_likelihood", "walk_to_likelihood", "turn_second_likelihood", "stand_to_sit_likelihood",
            
            // Movement phase indicators (NEW - sequence-aware cumulative tracking)
            "is_strong_vertical", "is_strong_forward", "is_strong_turning",
            "cumulative_vertical", "cumulative_forward", "cumulative_turning", "sequence_progression",
            
            // Temporal smoothing features (rolling windows: 5, 10, 15, 30)
            "hip_y_velocity_smooth_5", "turn_score_smooth_5", "movement_complexity_smooth_5",
            "hip_height_std_5", "forward_momentum_std_5", "rotation_variation_5",
            "hip_y_velocity_smooth_10", "turn_score_smooth_10", "movement_complexity_smooth_10",
            "hip_height_std_10", "forward_momentum_std_10", "rotation_variation_10",
            "hip_y_velocity_smooth_15", "turn_score_smooth_15", "movement_complexity_smooth_15",
            "hip_height_std_15", "forward_momentum_std_15", "rotation_variation_15",
            "hip_y_velocity_smooth_30", "turn_score_smooth_30", "movement_complexity_smooth_30",
            "hip_height_std_30", "forward_momentum_std_30", "rotation_variation_30",
            
            // Derived features
            "hip_y_acceleration", "movement_jerk", "rotation_acceleration",
            "sustained_vertical_movement", "sustained_forward_movement", "sustained_turning"
        )
        
        // Convert to ordered FloatArray
        val result = FloatArray(111) // Expected model input size (updated from 111 to 111)
        for (i in expectedFeatures.indices.take(111)) { // Ensure we don't exceed 111 features
            val featureName = expectedFeatures[i]
            result[i] = featureMap[featureName] ?: 0f
        }
        
        // Log feature conversion for debugging (first few frames only)
        if (featureMap["frame"]?.toInt() ?: 0 < 3) {
            Log.d(TAG, "Frame ${featureMap["frame"]?.toInt()}: Converted ${featureMap.size} features to ${result.size} array")
            Log.d(TAG, "Sample values: hip_y_velocity=${featureMap["hip_y_velocity"]}, forward_momentum=${featureMap["forward_momentum"]}")
        }
        
        return result
    }

    // ===== ONNX Inference =====

    private fun predictWithModel(features: FloatArray, m: ModelData): Int {
        try {
            val inputTensor = OnnxTensor.createTensor(ortEnvironment!!, FloatBuffer.wrap(features), longArrayOf(1, 111))
            val result = m.ortSession.run(mapOf("input" to inputTensor))
            
            // XGBoost ONNX model returns class indices as long[], not probabilities
            val outputTensor = result.get(0).value
            val predictedIndex = when (outputTensor) {
                is LongArray -> outputTensor[0].toInt()
                is Array<*> -> {
                    val probabilities = outputTensor[0] as FloatArray
                    probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
                }
                else -> 0
            }
            
            inputTensor.close()
            result.close()
            
            // Ensure we return a valid index within bounds
            return predictedIndex.coerceIn(0, m.labelEncoder.size - 1)
        } catch (e: Exception) {
            Log.e(TAG, "Error during ONNX prediction", e)
            return 0
        }
    }

    // ===== Smoothing and Sequence Correction =====

    private fun smoothMajority(labels: List<String>, window: Int = 5): List<String> {
        if (labels.size <= window) return labels
        val smoothed = mutableListOf<String>()
        val halfWindow = window / 2
        for (i in labels.indices) {
            val start = maxOf(0, i - halfWindow)
            val end = minOf(labels.size, i + halfWindow + 1)
            val windowLabels = labels.subList(start, end)
            val mostCommon = windowLabels.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: labels[i]
            smoothed.add(mostCommon)
        }
        return smoothed
    }

    private fun correctSequence(labels: List<String>): List<String> {
        if (labels.isEmpty()) return labels
        // Expected TUG sequence mapped to actual label encoder indices
        val expectedSequence = listOf(
            "Sit-To-Stand",
            "Walk-From-Chair",
            "Turn-First",
            "Walk-To-Chair",
            "Turn-Second",
            "Stand-To-Sit"
        )
        val corrected = mutableListOf<String>()
        var lastValidPhase = -1
        for (label in labels) {
            val currentPhaseIndex = expectedSequence.indexOf(label)
            if (currentPhaseIndex != -1) {
                if (currentPhaseIndex >= lastValidPhase) {
                    corrected.add(label)
                    lastValidPhase = currentPhaseIndex
                } else {
                    val prevPhase = when {
                        lastValidPhase < 0 -> expectedSequence.firstOrNull() ?: label
                        lastValidPhase >= expectedSequence.size -> expectedSequence.lastOrNull() ?: label
                        else -> expectedSequence[lastValidPhase]
                    }
                    corrected.add(prevPhase)
                }
            } else {
                val timeProgress = if (labels.isNotEmpty()) corrected.size.toFloat() / labels.size.toFloat() else 0f
                val temporalPhaseIndex = (timeProgress * expectedSequence.size).toInt().coerceIn(0, if (expectedSequence.isNotEmpty()) expectedSequence.size - 1 else 0)
                val temporalPhase = if (expectedSequence.isNotEmpty() && temporalPhaseIndex >= 0 && temporalPhaseIndex < expectedSequence.size) expectedSequence[temporalPhaseIndex] else label
                corrected.add(temporalPhase)
            }
        }
        return corrected
    }

    /**
     * Force a logical phase sequence based on model predictions
     * Converted from Python force_phase_sequence function
     */
    private fun forcePhaseSequence(
        predictions: List<String>, 
        phaseOrder: List<String>? = null, 
        minFrames: Int = 5
    ): List<String> {
        val defaultPhaseOrder = listOf(
            "Sit-To-Stand",
            "Walk-From-Chair", 
            "Turn-First",
            "Walk-To-Chair",
            "Turn-Second",
            "Stand-To-Sit"
        )
        if (defaultPhaseOrder.isEmpty()) return predictions
        
        val phases = phaseOrder ?: defaultPhaseOrder
        val result = MutableList(predictions.size) { "" }
        var currentPhase = 0
        var count = 0
        
        for (i in predictions.indices) {
            // If we've gone through all phases, stick to the last one
            if (phases.isEmpty()) {
                result[i] = "Unknown"
                continue
            }
            if (currentPhase >= phases.size) {
                result[i] = phases.lastOrNull() ?: "Unknown"
                continue
            }
            
            val expected = phases[currentPhase]
            result[i] = expected
            
            // Look ahead to see if we should transition to next phase
            if (i + 5 < predictions.size) {
                val window = predictions.subList(i, i + 5)
                val nextPhaseIndex = currentPhase + 1
                
                if (nextPhaseIndex < phases.size) {
                    val nextPhase = phases[nextPhaseIndex]
                    val nextPhaseCount = window.count { it == nextPhase }
                    
                    if (nextPhaseCount >= 3) {
                        count++
                        if (count >= minFrames) {
                            currentPhase++
                            count = 0
                            val prevPhaseName = if (phases.isNotEmpty() && currentPhase > 0 && currentPhase - 1 < phases.size) phases[currentPhase-1] else "Unknown"
                            val currPhaseName = if (phases.isNotEmpty() && currentPhase >= 0 && currentPhase < phases.size) phases[currentPhase] else "Unknown"
                            Log.d(TAG, "Phase transition at frame $i: $prevPhaseName → $currPhaseName")
                        }
                    } else {
                        count = 0
                    }
                }
            }
        }
        
        return result
    }

    // ===== Duration Analysis =====

    private fun expandPredictionsToFrames(predictions: List<String>, totalFrames: Int): List<String> {
        val frameLevelPredictions = mutableListOf<String>()
        val framesPerPrediction = totalFrames.toFloat() / predictions.size.toFloat()
        
        predictions.forEachIndexed { index, prediction ->
            val startFrame = (index * framesPerPrediction).toInt()
            val endFrame = ((index + 1) * framesPerPrediction).toInt().coerceAtMost(totalFrames)
            repeat(endFrame - startFrame) {
                frameLevelPredictions.add(prediction)
            }
        }
        
        // Fill any remaining frames with the last prediction
        while (frameLevelPredictions.size < totalFrames) {
            frameLevelPredictions.add(predictions.lastOrNull() ?: "Unknown")
        }
        
        return frameLevelPredictions.take(totalFrames)
    }

    private fun analyzePhaseDurations(sequence: List<String>, fps: Float): List<PhaseAnalysis> {
        if (sequence.isEmpty()) return emptyList()
        
        val phases = mutableListOf<PhaseAnalysis>()
        var currentPhase = sequence[0]
        var startFrame = 0
        
        for (i in 1..sequence.size) {
            val isLastFrame = i == sequence.size
            val phaseChanged = !isLastFrame && i < sequence.size && sequence[i] != currentPhase
            
            if (phaseChanged || isLastFrame) {
                val endFrame = i - 1
                val frameCount = endFrame - startFrame + 1
                val duration = frameCount / fps
                
                phases.add(PhaseAnalysis(
                    phase = currentPhase,
                    start_frame = startFrame,
                    end_frame = endFrame,
                    duration_sec = duration,
                    frame_count = frameCount
                ))
                
                if (!isLastFrame && i < sequence.size) {
                    currentPhase = sequence[i]
                    startFrame = i
                }
            }
        }
        
        return phases
    }

    // ===== Utilities =====
    
    /**
     * Create TUG metrics map for severity classification
     */
    private fun createTugMetricsMap(phaseDurations: Map<String, Float>, totalTime: Double): Map<String, Double> {
        return mapOf(
            "total_time" to totalTime,
            "walk_from_chair_time" to (phaseDurations["Walk-From-Chair"]?.toDouble() ?: 0.0),
            "walk_to_chair_time" to (phaseDurations["Walk-To-Chair"]?.toDouble() ?: 0.0),
            "turn_first_time" to (phaseDurations["Turn-First"]?.toDouble() ?: 0.0),
            "turn_second_time" to (phaseDurations["Turn-Second"]?.toDouble() ?: 0.0),
            "sit_to_stand_time" to (phaseDurations["Sit-To-Stand"]?.toDouble() ?: 0.0),
            "stand_to_sit_time" to (phaseDurations["Stand-To-Sit"]?.toDouble() ?: 0.0)
        )
    }

    private fun failResult(name: String, msg: String): PredictionResult =
        PredictionResult(
            filename = name,
            total_frames = 0,
            total_duration_sec = 0f,
            phase_durations = emptyMap(),
            phase_analysis = emptyList(),
            severity = "Unknown",
            success = false,
            error_message = msg
        )

    fun cleanup() {
        try {
            modelData?.ortSession?.close()
            ortEnvironment?.close()
            Log.d(TAG, "Cleaned up successfully")
        } catch (e: Exception) {
            Log.w(TAG, "Cleanup warning", e)
        }
    }
    }
