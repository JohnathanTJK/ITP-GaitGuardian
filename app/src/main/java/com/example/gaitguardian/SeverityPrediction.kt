package com.example.gaitguardian

import android.content.Context
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.FloatBuffer
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.absoluteValue

/**
 * Severity classification using ONNX Runtime
 * Binary: Normal (0) vs Impaired (1)
 * Uses ensemble_inference_severity_binary.onnx (RobustScaler + VotingClassifier)
 */
class SeverityPrediction(private val context: Context) {

    companion object {
        private const val TAG = "SeverityPrediction"

        // New files from Python export_to_onnx.py
        private const val MODEL_FILE = "ensemble_inference_severity_binary.onnx"
        private const val METADATA_FILE = "ensemble_inference_metadata_binary.json"
    }

    @Serializable
    data class SeverityMetadata(
        val mode: String,
        val model_type: String,
        val n_features: Int,
        val feature_names: List<String>,
        val class_names: List<String>,
        val test_f1_score: Double? = null
    )

    private var session: OrtSession? = null
    private var ortEnvironment: OrtEnvironment? = null
    private var metadata: SeverityMetadata? = null

    suspend fun initializeModel(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔧 Initializing severity model...")
            ortEnvironment = OrtEnvironment.getEnvironment()

            // Load ONNX pipeline (scaler + classifier)
            val modelBytes = context.assets.open(MODEL_FILE).readBytes()
            session = ortEnvironment!!.createSession(modelBytes)
            Log.d(TAG, "✅ ONNX model loaded: $MODEL_FILE")

            // Load metadata (ignore extra keys like best_params/top_features)
            val metadataJson = context.assets.open(METADATA_FILE).bufferedReader().readText()
            val json = Json { ignoreUnknownKeys = true }
            metadata = json.decodeFromString(metadataJson)
            Log.d(
                TAG,
                "✅ Metadata loaded: n_features=${metadata?.n_features}, " +
                        "classes=${metadata?.class_names}, mode=${metadata?.mode}"
            )

            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize severity model", e)
            false
        }
    }

    /**
     * Predict severity from TUG phase durations (seconds)
     * phaseDurations keys: "Sit-To-Stand", "Walk-From-Chair", "Turn-First",
     *                      "Walk-To-Chair", "Turn-Second", "Stand-To-Sit"
     */
    suspend fun predictSeverity(phaseDurations: Map<String, Float>): String =
        withContext(Dispatchers.Default) {
            val meta = metadata ?: throw IllegalStateException("Model not initialized")
            val sess = session ?: throw IllegalStateException("Model session not initialized")

            try {
                // 1) Build feature map (Kotlin port of Python extract_enhanced_features)
                val featureMap = buildEnhancedFeatures(phaseDurations)

                // 2) Order features according to metadata.feature_names
                val featureNames = meta.feature_names
                val featureVector = FloatArray(featureNames.size) { i ->
                    featureMap[featureNames[i]] ?: 0f
                }

                Log.d(
                    TAG,
                    "🔍 First 10 features: ${
                        featureVector.take(10).joinToString(", ") { "%.4f".format(it) }
                    }"
                )

                // 3) Run ONNX inference (RobustScaler is inside the pipeline)
                val inputTensor = OnnxTensor.createTensor(
                    ortEnvironment!!,
                    FloatBuffer.wrap(featureVector),
                    longArrayOf(1, featureVector.size.toLong())
                )

                val results = sess.run(mapOf("input" to inputTensor))
                inputTensor.close()

                // 4) Parse output: label (LongArray) or probabilities
                val output = results[0].value
                val predictedClass = when (output) {
                    is LongArray -> output[0].toInt()
                    is Array<*> -> {
                        val probs = output[0] as FloatArray
                        probs.indices.maxByOrNull { probs[it] } ?: 0
                    }
                    else -> 0
                }
                results.close()

                val label = meta.class_names.getOrNull(predictedClass) ?: "Unknown"
                Log.d(TAG, "🎯 Severity prediction: class=$predictedClass → $label")

                label
            } catch (e: Exception) {
                Log.e(TAG, "❌ Severity prediction failed", e)
                "Unknown"
            }
        }

    /**
     * Kotlin port of Python extract_enhanced_features()
     * Uses only phase durations and basic ratios, same names as in train_mlp_severity.py
     */
    private fun buildEnhancedFeatures(phaseDurations: Map<String, Float>): Map<String, Float> {
        val sitToStand = phaseDurations["Sit-To-Stand"] ?: 0f
        val walkFrom = phaseDurations["Walk-From-Chair"] ?: 0f
        val turnFirst = phaseDurations["Turn-First"] ?: 0f
        val walkTo = phaseDurations["Walk-To-Chair"] ?: 0f
        val turnSecond = phaseDurations["Turn-Second"] ?: 0f
        val standToSit = phaseDurations["Stand-To-Sit"] ?: 0f

        val totalTime = sitToStand + walkFrom + turnFirst + walkTo + turnSecond + standToSit
        val walkTime = walkFrom + walkTo
        val turnTime = turnFirst + turnSecond
        val transitionTime = sitToStand + standToSit
        val eps = 1e-6f

        val firstHalfTime = sitToStand + walkFrom + turnFirst
        val secondHalfTime = walkTo + turnSecond + standToSit

        fun safeDiv(a: Float, b: Float): Float = if (b.absoluteValue < eps) 0f else a / b
        fun log1p(x: Float): Float = ln(1f + x)

        return mapOf(
            // BASIC TIMING
            "total_duration" to totalTime,
            "sit_to_stand_time" to sitToStand,
            "walk_from_chair_time" to walkFrom,
            "turn_first_time" to turnFirst,
            "walk_to_chair_time" to walkTo,
            "turn_second_time" to turnSecond,
            "stand_to_sit_time" to standToSit,
            "total_walk_time" to walkTime,
            "total_turn_time" to turnTime,
            "total_transition_time" to transitionTime,

            // RATIOS (PHASE PROPORTIONS)
            "sit_to_stand_ratio" to safeDiv(sitToStand, totalTime + eps),
            "walk_ratio" to safeDiv(walkTime, totalTime + eps),
            "turn_ratio" to safeDiv(turnTime, totalTime + eps),
            "transition_ratio" to safeDiv(transitionTime, totalTime + eps),
            "stand_to_sit_ratio" to safeDiv(standToSit, totalTime + eps),

            // CLINICAL RATIOS
            "turn_walk_ratio" to safeDiv(turnTime, walkTime + eps),
            "transition_walk_ratio" to safeDiv(transitionTime, walkTime + eps),
            "sit_stand_asymmetry" to safeDiv(
                abs(sitToStand - standToSit),
                transitionTime + eps
            ),

            // AVERAGE PHASE TIMES
            "avg_walk_time" to if (walkTime > 0f) walkTime / 2f else 0f,
            "avg_turn_time" to if (turnTime > 0f) turnTime / 2f else 0f,

            // PHASE VARIABILITY
            "walk_asymmetry" to safeDiv(abs(walkFrom - walkTo), walkTime + eps),
            "turn_asymmetry" to safeDiv(abs(turnFirst - turnSecond), turnTime + eps),

            // PACE
            "walk_pace" to safeDiv(walkTime, totalTime + eps),
            "turn_pace" to safeDiv(turnTime, totalTime + eps),

            // SPEED (inverse times)
            "sit_to_stand_speed" to safeDiv(1f, sitToStand + eps),
            "walk_from_speed" to safeDiv(1f, walkFrom + eps),
            "turn_first_speed" to safeDiv(1f, turnFirst + eps),
            "walk_to_speed" to safeDiv(1f, walkTo + eps),
            "turn_second_speed" to safeDiv(1f, turnSecond + eps),
            "stand_to_sit_speed" to safeDiv(1f, standToSit + eps),
            "overall_speed" to safeDiv(1f, totalTime + eps),

            // THRESHOLD FLAGS (match Python thresholds)
            "exceeds_normal_threshold" to if (totalTime > 10f) 1f else 0f,
            "exceeds_risk_threshold" to if (totalTime > 13.5f) 1f else 0f,
            "slow_sit_to_stand" to if (sitToStand > 2.5f) 1f else 0f,
            "slow_walking" to if (walkTime > 5f) 1f else 0f,
            "slow_turning" to if (turnTime > 4f) 1f else 0f,
            "slow_stand_to_sit" to if (standToSit > 3f) 1f else 0f,

            // INTERACTIONS
            "turn_walk_product" to (turnTime * walkTime),
            "transition_turn_product" to (transitionTime * turnTime),
            "sit_walk_product" to (sitToStand * walkTime),

            // PHASE SEQUENCE
            "first_half_time" to firstHalfTime,
            "second_half_time" to secondHalfTime,
            "first_second_asymmetry" to safeDiv(
                abs(firstHalfTime - secondHalfTime),
                totalTime + eps
            ),

            // POLYNOMIAL
            "total_duration_squared" to (totalTime * totalTime),
            "turn_walk_ratio_squared" to (safeDiv(turnTime, walkTime + eps)).let { it * it },
            "sit_to_stand_squared" to (sitToStand * sitToStand),

            // LOG FEATURES
            "log_total_duration" to log1p(totalTime),
            "log_turn_walk_ratio" to log1p(safeDiv(turnTime, walkTime + eps)),
            "log_sit_to_stand" to log1p(sitToStand)
        )
    }

    fun cleanup() {
        try {
            session?.close()
            ortEnvironment?.close()
            Log.d(TAG, "✅ Cleaned up severity model")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Cleanup warning", e)
        }
    }
}
