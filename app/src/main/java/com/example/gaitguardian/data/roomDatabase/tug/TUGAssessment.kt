package com.example.gaitguardian.data.roomDatabase.tug

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tug_assessment_table")
data class TUGAssessment(
    @PrimaryKey(autoGenerate = true)
    val testId: Int = 0,
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
//TODO: probably another table for the video analysis output and match with testID
//package com.example.gaitguardian.data.roomDatabase.tug
//
//import androidx.room.ColumnInfo
//import androidx.room.Entity
//import androidx.room.PrimaryKey
//
//@Entity(tableName = "tug_assessment_table")
//data class TUGAssessment(
//    @PrimaryKey(autoGenerate = true)
//    val testId: Int,
//    //TODO: Need to check again if it's okay to link it to patientID,
//    // because technically without ID it should still work by pulling out all the data
//    // if it all belongs to a single patient
//    @ColumnInfo(name = "patientId")
//    val patientId: Int,
//    @ColumnInfo(name = "dateTime")
//    val dateTime: String,
//    @ColumnInfo(name = "medication")
//    val medication: String,
//    @ColumnInfo(name = "gaitFeatures")
//    val gaitFeatures: String,
//    @ColumnInfo(name = "severity")
//    val severity: String,
//    @ColumnInfo(name = "videoDuration")
//    val videoDuration: Float,
//    @ColumnInfo(name = "videoStoredLocally")
//    val videoStoredLocally: Boolean,
//
//    // Clinician Information
//    @ColumnInfo(name = "clinicianId")
//    val clinicianId: Int,
//    @ColumnInfo(name = "watchStatus")
//    val watchStatus: Boolean, // whether the clinician viewed or not
//    @ColumnInfo(name = "notes")
//    val notes: String,
//)