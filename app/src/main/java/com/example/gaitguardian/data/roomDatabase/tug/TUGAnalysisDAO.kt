package com.example.gaitguardian.data.roomDatabase.tug

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TugAnalysisDao {
// DAO will store all the SQL queries, referenced from Chen Kan jetpackArchTest repo

    @Query("SELECT * FROM tug_analysis_table")
    fun getAllAnalysis(): Flow<List<TUGAnalysis>>  // Room does not support StateFlow
    @Insert
    suspend fun insertNewTUGAnalysis(tugAnalysis: TUGAnalysis) : Long

    // For Updating Result Card
    @Query("SELECT * FROM tug_analysis_table ORDER BY testId DESC LIMIT 1")
    suspend fun getLatestTugAnalysis(): TUGAnalysis?

    //TODO: Update Result Card with this
    @Query("SELECT timeTaken FROM tug_analysis_table ORDER BY testId DESC LIMIT 2")
    suspend fun getLatestTwoTimes(): List<Double>

    @Query("SELECT sitToStand, walkFromChair, turnFirst, walkToChair, turnSecond, standToSit FROM tug_analysis_table WHERE testId = :id ")
    suspend fun getSubtaskById(id: Int): subtaskDuration


}