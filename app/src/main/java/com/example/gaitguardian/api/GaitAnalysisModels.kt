package com.example.gaitguardian.api

import com.google.gson.annotations.SerializedName

data class GaitAnalysisResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("gait_metrics") val gaitMetrics: GaitMetrics?,
    @SerializedName("tug_metrics") val tugMetrics: TugMetrics?,
    @SerializedName("severity") val severity: String?,
    @SerializedName("processing_info") val processingInfo: ProcessingInfo?,
    @SerializedName("request_id") val requestId: String?,
    @SerializedName("error") val error: String?,
    @SerializedName("error_type") val errorType: String?
)

data class GaitMetrics(
    @SerializedName("step_count") val stepCount: Int,
    @SerializedName("mean_step_length") val meanStepLength: Double,
    @SerializedName("stride_time") val strideTime: Double,
    @SerializedName("cadence") val cadence: Double,
    @SerializedName("step_symmetry") val stepSymmetry: Double,
    @SerializedName("left_knee_range") val leftKneeRange: Double,
    @SerializedName("right_knee_range") val rightKneeRange: Double,
    @SerializedName("upper_body_sway") val upperBodySway: Double,
    @SerializedName("turn1_duration") val turn1Duration: Double,
    @SerializedName("turn2_duration") val turn2Duration: Double
)

data class TugMetrics(
    @SerializedName("sit_to_stand_time") val sitToStandTime: Double,
    @SerializedName("walk_from_chair_time") val walkFromChairTime: Double,
    @SerializedName("turn_first_time") val turnFirstTime: Double,
    @SerializedName("walk_to_chair_time") val walkToChairTime: Double,
    @SerializedName("turn_second_time") val turnSecondTime: Double,
    @SerializedName("stand_to_sit_time") val standToSitTime: Double,
    @SerializedName("total_time") val totalTime: Double
)

data class ProcessingInfo(
    @SerializedName("total_frames") val totalFrames: Int,
    @SerializedName("processed_frames") val processedFrames: Int,
    @SerializedName("fps") val fps: Double,
    @SerializedName("processing_time_seconds") val processingTimeSeconds: Double
)