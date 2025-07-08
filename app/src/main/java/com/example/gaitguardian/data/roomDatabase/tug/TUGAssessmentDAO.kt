package com.example.gaitguardian.data.roomDatabase.tug

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TugDao {
// DAO will store all the SQL queries, referenced from Chen Kan jetpackArchTest repo

    @Query("SELECT * FROM tug_assessment_table")
    fun getAllTUGVideos(): Flow<List<TUGAssessment>>  // Room does not support StateFlow
    @Insert
    suspend fun insertNewTUGAssessment(tugAssessment: TUGAssessment)

    @Query("SELECT * FROM tug_assessment_table WHERE testId = :id")
    suspend fun getAssessmentById(id: Int): TUGAssessment?

    @Query("UPDATE tug_assessment_table SET watchStatus = :watchStatus, notes = :notes WHERE testId = :id")
    suspend fun updateClinicianReview(id: Int, watchStatus: Boolean, notes: String)

    @Query("UPDATE tug_assessment_table SET watchStatus = :watchStatus WHERE testId = :id")
    suspend fun multiSelectMarkAsReviewed(id: Int, watchStatus: Boolean)
//    @Query("DELETE FROM patients_table WHERE id = :id")
//    fun deletePatient(id: Int)
}