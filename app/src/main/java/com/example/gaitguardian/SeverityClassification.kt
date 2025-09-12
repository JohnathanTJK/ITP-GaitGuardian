package com.example.gaitguardian

import android.util.Log

/**
 * Severity classification for TUG (Timed Up and Go) test based on metrics
 * Converted from Python implementation in app.py
 */
object SeverityClassification {
    
    private const val TAG = "SeverityClassification"
    
    /**
     * Classify gait severity based on TUG metrics
     * 
     * @param tugMetrics Map containing TUG timing metrics
     * @param gaitMetrics Map containing gait analysis metrics (currently not used in classification)
     * @return Severity classification as String: "Normal", "Slight", "Mild", "Moderate", or "Severe"
     */
    fun classifyGaitSeverity(
        tugMetrics: Map<String, Double>,
        gaitMetrics: Map<String, Double> = emptyMap()
    ): String {
        return try {
            val totalTime = tugMetrics["total_time"] ?: 0.0
            val walkFromChairTime = tugMetrics["walk_from_chair_time"] ?: 0.0
            val walkToChairTime = tugMetrics["walk_to_chair_time"] ?: 0.0
            val turnFirstTime = tugMetrics["turn_first_time"] ?: 0.0
            val turnSecondTime = tugMetrics["turn_second_time"] ?: 0.0

            val walkingTime = walkFromChairTime + walkToChairTime
            val turningTime = turnFirstTime + turnSecondTime
            val turnWalkRatio = if (walkingTime > 0.1) turningTime / walkingTime else 0.0

            // Debug logging (matching Python implementation)
            Log.d(TAG, "DEBUG: total_time=${String.format("%.2f", totalTime)}")
            Log.d(TAG, "DEBUG: walking_time=${String.format("%.2f", walkingTime)}")
            Log.d(TAG, "DEBUG: turning_time=${String.format("%.2f", turningTime)}")
            Log.d(TAG, "DEBUG: turn_walk_ratio=${String.format("%.2f", turnWalkRatio)}")

            val severity = when {
                totalTime <= 7 && turnWalkRatio < 1.0 -> "Normal"
                totalTime <= 13 && turnWalkRatio < 1.0 -> "Slight"
                totalTime <= 13 && turnWalkRatio >= 1.0 -> "Mild"
                totalTime > 13 && turnWalkRatio > 1.0 -> "Severe"
                totalTime > 13 -> "Moderate"
                else -> "Unknown"
            }

            Log.d(TAG, "DEBUG: severity=$severity")
            severity

        } catch (e: Exception) {
            Log.e(TAG, "Error classifying gait severity: ${e.message}", e)
            "Unknown"
        }
    }
    
    /**
     * Classify severity using TUGAnalysis data structure
     * 
     * @param tugAnalysis TUGAnalysis object containing timing data
     * @return Severity classification as String
     */
    fun classifyFromTugAnalysis(tugAnalysis: com.example.gaitguardian.data.roomDatabase.tug.TUGAnalysis): String {
        val tugMetrics = mapOf(
            "total_time" to tugAnalysis.timeTaken,
            "walk_from_chair_time" to tugAnalysis.walkFromChair,
            "walk_to_chair_time" to tugAnalysis.walkToChair,
            "turn_first_time" to tugAnalysis.turnFirst,
            "turn_second_time" to tugAnalysis.turnSecond
        )
        
        // We can include additional gait metrics if needed
        val gaitMetrics = mapOf(
            "step_count" to tugAnalysis.stepCount.toDouble()
        )
        
        return classifyGaitSeverity(tugMetrics, gaitMetrics)
    }
    
    /**
     * Create TUGAnalysis object with severity classification
     * 
     * @param timeTaken Total time taken for TUG test
     * @param stepCount Number of steps
     * @param sitToStand Time for sit-to-stand phase
     * @param walkFromChair Time for walking from chair phase
     * @param turnFirst Time for first turn phase
     * @param walkToChair Time for walking to chair phase
     * @param turnSecond Time for second turn phase
     * @param standToSit Time for stand-to-sit phase
     * @return TUGAnalysis object with calculated severity
     */
    fun createTugAnalysisWithSeverity(
        timeTaken: Double,
        stepCount: Int,
        sitToStand: Double,
        walkFromChair: Double,
        turnFirst: Double,
        walkToChair: Double,
        turnSecond: Double,
        standToSit: Double
    ): com.example.gaitguardian.data.roomDatabase.tug.TUGAnalysis {
        
        val tugMetrics = mapOf(
            "total_time" to timeTaken,
            "walk_from_chair_time" to walkFromChair,
            "walk_to_chair_time" to walkToChair,
            "turn_first_time" to turnFirst,
            "turn_second_time" to turnSecond
        )
        
        val severity = classifyGaitSeverity(tugMetrics)
        
        return com.example.gaitguardian.data.roomDatabase.tug.TUGAnalysis(
            severity = severity,
            timeTaken = timeTaken,
            stepCount = stepCount,
            sitToStand = sitToStand,
            walkFromChair = walkFromChair,
            turnFirst = turnFirst,
            walkToChair = walkToChair,
            turnSecond = turnSecond,
            standToSit = standToSit
        )
    }
    
    /**
     * Convert PredictionResult to TUGAnalysis with severity classification
     * 
     * @param predictionResult PredictionResult from TugPrediction processing
     * @param stepCount Number of steps (default 0 if not available)
     * @return TUGAnalysis object with calculated severity
     */
    fun createTugAnalysisFromPredictionResult(
        predictionResult: TugPrediction.PredictionResult,
        stepCount: Int = 0
    ): com.example.gaitguardian.data.roomDatabase.tug.TUGAnalysis {
        
        val phaseDurations = predictionResult.phase_durations
        
        return com.example.gaitguardian.data.roomDatabase.tug.TUGAnalysis(
            severity = predictionResult.severity,
            timeTaken = predictionResult.total_duration_sec.toDouble(),
            stepCount = stepCount,
            sitToStand = phaseDurations["Sit-To-Stand"]?.toDouble() ?: 0.0,
            walkFromChair = phaseDurations["Walk-From-Chair"]?.toDouble() ?: 0.0,
            turnFirst = phaseDurations["Turn-First"]?.toDouble() ?: 0.0,
            walkToChair = phaseDurations["Walk-To-Chair"]?.toDouble() ?: 0.0,
            turnSecond = phaseDurations["Turn-Second"]?.toDouble() ?: 0.0,
            standToSit = phaseDurations["Stand-To-Sit"]?.toDouble() ?: 0.0
        )
    }
}
