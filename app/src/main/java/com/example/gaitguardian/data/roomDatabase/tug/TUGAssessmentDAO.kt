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

//    @Query("DELETE FROM patients_table WHERE id = :id")
//    fun deletePatient(id: Int)
}