package com.example.gaitguardian.data.roomDatabase.tug

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "tug_assessment_table")
data class TUGAssessment(
    @PrimaryKey(autoGenerate = false)
//    val testId: Int = 0,
    val testId : String,
    @ColumnInfo(name = "dateTime")
    val dateTime: String,
    @ColumnInfo(name = "videoDuration")
    val videoDuration: Float,
    @ColumnInfo(name ="videoTitle")
    val videoTitle: String? = null,
    @ColumnInfo(name = "onMedication")
    val onMedication: Boolean,
    @ColumnInfo(name = "updateMedication")
    val updateMedication: Boolean = false,
    @ColumnInfo(name = "patientComments")
    val patientComments: String,

    // Clinician Information
    @ColumnInfo(name = "clinicianId")
    val clinicianId: Int? = null,
    @ColumnInfo(name = "watchStatus")
    val watchStatus: Boolean = false, // whether the clinician viewed or not
    @ColumnInfo(name = "notes")
    val notes: String? = null,
)
