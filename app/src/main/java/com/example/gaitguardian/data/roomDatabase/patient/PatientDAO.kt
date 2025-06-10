package com.example.gaitguardian.data.roomDatabase.patient

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {
// DAO will store all the SQL queries, referenced from Chen Kan jetpackArchTest repo
//    @Query("SELECT * FROM patients_table ORDER BY id ASC")
//    fun getAllPatients(): Flow<List<Patient>>

    @Query("SELECT * FROM patients_table WHERE id = :patientId LIMIT 1")
    fun getPatientById(patientId: Int = 1): Flow<Patient?> // Room does not support StateFlow
    @Insert
    fun insertNewPatient(patient: Patient)

    @Query("DELETE FROM patients_table WHERE id = :id")
    fun deletePatient(id: Int)
}