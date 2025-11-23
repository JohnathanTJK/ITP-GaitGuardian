package com.example.gaitguardian.data.models

data class TugResult(
    val totalDuration: Double,
    val sitToStandDuration: Double,
    val walkingDuration: Double,
    val standToSitDuration: Double,
    val riskAssessment: String,
    val analysisDate: String,
    val phaseBreakdown: Map<String, Double>
)
