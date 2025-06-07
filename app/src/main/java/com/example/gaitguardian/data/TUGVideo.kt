package com.example.gaitguardian.data

data class TUGVideo(
    val testId: Int,
    val dateTime: String,
    val medication: String,
    val severity: String,
    val watchStatus: Boolean, // whether the clinician viewed or not
)