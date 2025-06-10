package com.example.gaitguardian.data.roomDatabase.tug

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tug_assessment_table")
data class TUGAssessment(
    @PrimaryKey(autoGenerate = true)
    val testId: Int,

    @ColumnInfo(name = "patientId")
    val patientId: Int,
    @ColumnInfo(name = "dateTime")
    val dateTime: String,
    @ColumnInfo(name = "medication")
    val medication: String,
    @ColumnInfo(name = "severity")
    val severity: String,
    @ColumnInfo(name = "videoDuration")
    val videoDuration: Float,
    @ColumnInfo(name = "videoStoredLocally")
    val videoStoredLocally: Boolean,

    // Clinician Information
    @ColumnInfo(name = "clinicianId")
    val clinicianId: Int,
    @ColumnInfo(name = "watchStatus")
    val watchStatus: Boolean, // whether the clinician viewed or not
    @ColumnInfo(name = "notes")
    val notes: String,
)