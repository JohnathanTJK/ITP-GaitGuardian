package com.example.gaitguardian.data.roomDatabase.tug

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "tug_analysis_table")
data class TUGAnalysis(
    @PrimaryKey(autoGenerate = true)
    val testId: Int = 0,
    // Severity Rating
    @ColumnInfo(name ="severity")
    val severity: String,
    // Time Taken
    @ColumnInfo(name = "timeTaken")
    val timeTaken: Double,
    @ColumnInfo(name = "stepCount")
    val stepCount: Int,
    @ColumnInfo(name = "sitToStand")
    val sitToStand: Double,
    @ColumnInfo(name = "walkFromChair")
    val walkFromChair : Double,
    @ColumnInfo(name = "turnFirst")
    val turnFirst : Double,
    @ColumnInfo(name = "walkToChair")
    val walkToChair :Double,
    @ColumnInfo("turnSecond")
    val turnSecond: Double,
    @ColumnInfo("standToSit")
    val standToSit: Double,
)


data class subtaskDuration(
    val sitToStand: Double,
    val walkFromChair: Double,
    val turnFirst: Double,
    val walkToChair: Double,
    val turnSecond: Double,
    val standToSit: Double,
)

