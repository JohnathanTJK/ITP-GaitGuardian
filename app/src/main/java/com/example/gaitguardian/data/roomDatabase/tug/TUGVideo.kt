package com.example.gaitguardian.data.roomDatabase.tug

//TODO: to use TUGAssessment instead,
// using this temporarily before finalising TUGAssessment
data class TUGVideo(
    val testId: Int,
    val dateTime: String,
    val medication: String,
    val severity: String,
    val watchStatus: Boolean, // whether the clinician viewed or not
)